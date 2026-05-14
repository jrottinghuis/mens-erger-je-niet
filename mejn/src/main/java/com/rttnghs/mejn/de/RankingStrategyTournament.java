/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rttnghs.mejn.de;

import com.rttnghs.mejn.Tournament;
import com.rttnghs.mejn.statistics.EventCounter;
import com.rttnghs.mejn.statistics.Score;
import com.rttnghs.mejn.strategy.Strategy;
import com.rttnghs.mejn.strategy.StrategyFactory;
import com.rttnghs.mejn.strategy.ranking.SomeRankingStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Bridges MEJN's Tournament infrastructure to a tournament-de worker via Java
 * reflection.
 *
 * <p>Discovered at runtime by the worker implementation in tournament-de using
 * the configured class-name key {@code tournamentRunnerClass}. There is
 * intentionally no compile-time dependency on tournament-de; the contract is
 * purely structural - the method signature
 * {@code List<Double> runBracket(List<List<Integer>>, int)}.
 *
 * <p>Each inner list in {@code genomeBracket} is the parameter vector for one
 * {@link SomeRankingStrategy} participant. All participants play each other in a
 * single {@link Tournament}. Scores are normalized using {@link Score} and
 * {@link EventCounter}, then returned in genome position order.
 *
 * <p>No MEJN {@code Config} is read here; all inputs arrive as parameters so
 * this class can be invoked headlessly from a tournament-de worker JVM.
 */
public class RankingStrategyTournament {

    /**
     * Run one bracket of games and return a normalized score per genome.
     *
     * <p>Strategy names are assigned as {@code "genome-0"}, {@code "genome-1"},
     * etc., matching the position in {@code genomeBracket}. The returned list is
     * in the same order.
     *
     * @param genomeBracket list of genome vectors; one {@link SomeRankingStrategy}
     *                      is created per entry.
     * @param games         number of games to play in the tournament.
     * @return normalized scores indexed by genome position in {@code genomeBracket}.
     *         Values are in the range {@code [0, Score.winningScore(playerCount)]}.
     * @throws IllegalArgumentException if {@code genomeBracket} is empty or
     *                                  {@code games} is not positive.
     */
    public List<Double> runBracket(List<List<Integer>> genomeBracket, int games) {
        if (genomeBracket == null || genomeBracket.isEmpty()) {
            throw new IllegalArgumentException("genomeBracket must not be null or empty");
        }
        if (games <= 0) {
            throw new IllegalArgumentException("games must be positive, got: " + games);
        }

        int playerCount = genomeBracket.size();

        // Build strategy name list and pre-construct each SomeRankingStrategy.
        // Names are "genome-<index>" to keep a stable mapping back to result indices.
        List<String> strategyNames = new ArrayList<>(playerCount);
        Map<String, SomeRankingStrategy> strategyMap = new HashMap<>(playerCount);

        for (int i = 0; i < playerCount; i++) {
            String name = "genome-" + i;
            strategyNames.add(name);
            strategyMap.put(name, new SomeRankingStrategy(name, genomeBracket.get(i)));
        }

        // Inline StrategyFactory backed by the pre-built map; no Config dependency.
        StrategyFactory factory = new StrategyFactory() {
            @Override
            public List<String> listStrategies() {
                return List.copyOf(strategyNames);
            }

            @Override
            public Strategy getStrategy(String strategyName) {
                Strategy s = strategyMap.get(strategyName);
                if (s == null) {
                    throw new IllegalArgumentException("Unknown strategy name: " + strategyName);
                }
                return s;
            }
        };

        Tournament tournament = new Tournament(factory, strategyNames, games);
        EventCounter<String, Integer> finishCounts = tournament.play();

        // Score.get maps finish-order index → integer medal score for playerCount players.
        Function<Integer, Integer> scorer = pos -> Score.get(pos, playerCount);
        Map<String, Integer> normalizedScores = EventCounter.getNormalizedScores(finishCounts, scorer, 100);

        // Rebuild the result in genome-position order.
        List<Double> result = new ArrayList<>(playerCount);
        for (String name : strategyNames) {
            result.add(normalizedScores.getOrDefault(name, 0).doubleValue());
        }
        return result;
    }
}

