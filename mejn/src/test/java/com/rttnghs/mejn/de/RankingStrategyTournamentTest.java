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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankingStrategyTournamentTest {

    private final RankingStrategyTournament runner = new RankingStrategyTournament();

    // Builds a competitor parameter list, padding to the minimum required by SomeRankingStrategy.
    private static List<Integer> competitor(int... values) {
        List<Integer> list = new java.util.ArrayList<>();
        for (int v : values) list.add(v);
        while (list.size() < 6) {
            list.add(0);
        }
        return list;
    }

    @Test
    void runBracket_returnsSizeMatchingCompetitorCount() {
        List<List<Integer>> bracket = List.of(competitor(1), competitor(2), competitor(3), competitor(4));
        List<Double> scores = runner.runBracket(bracket, 10);
        assertEquals(4, scores.size());
    }

    @Test
    void runBracket_allScoresNonNegative() {
        List<List<Integer>> bracket = List.of(competitor(1), competitor(2), competitor(3), competitor(4));
        List<Double> scores = runner.runBracket(bracket, 20);
        scores.forEach(s -> assertTrue(s >= 0.0, "Score should be non-negative: " + s));
    }

    @Test
    void runBracket_atLeastOneNonZeroScore() {
        // With 4 players and 20 games someone will have scored.
        List<List<Integer>> bracket = List.of(competitor(5), competitor(3), competitor(7), competitor(2));
        List<Double> scores = runner.runBracket(bracket, 20);
        boolean anyNonZero = scores.stream().anyMatch(s -> s > 0.0);
        assertTrue(anyNonZero, "At least one competitor should have a non-zero score");
    }

    @Test
    void runBracket_twoCompetitors() {
        // Minimum valid bracket: 2 players.
        List<List<Integer>> bracket = List.of(competitor(1, 2), competitor(3, 4));
        assertDoesNotThrow(() -> runner.runBracket(bracket, 5));
    }

    @Test
    void runBracket_throwsOnEmptyBracket() {
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(List.of(), 10));
    }

    @Test
    void runBracket_throwsOnNullBracket() {
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(null, 10));
    }

    @Test
    void runBracket_throwsOnZeroGames() {
        List<List<Integer>> bracket = List.of(competitor(1), competitor(2), competitor(3), competitor(4));
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(bracket, 0));
    }

    @Test
    void runBracket_throwsOnNegativeGames() {
        List<List<Integer>> bracket = List.of(competitor(1), competitor(2), competitor(3), competitor(4));
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(bracket, -1));
    }

    @Test
    void runBracket_throwsOnTooFewParameters() {
        // 5 params is one short - SomeMoveValuator requires 6.
        List<List<Integer>> bracket = List.of(List.of(1, 2, 3, 4, 5), competitor(1));
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(bracket, 5));
    }

    @Test
    void isValidCompetitor_trueForSufficientParams() {
        assertTrue(runner.isValidCompetitor(competitor(1, 2, 3)));
    }

    @Test
    void isValidCompetitor_falseForTooFewParams() {
        assertFalse(runner.isValidCompetitor(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    void isValidCompetitor_falseForNull() {
        assertFalse(runner.isValidCompetitor(null));
    }

    @Test
    void isValidCompetitor_falseForEmptyList() {
        assertFalse(runner.isValidCompetitor(List.of()));
    }

    @Test
    void runBracket_reflectionContractMethodExists() throws NoSuchMethodException {
        // Verify the structural contract that LocalComputeServer relies on.
        var method = RankingStrategyTournament.class.getMethod("runBracket", List.class, int.class);
        assertEquals(List.class, method.getReturnType());
        assertEquals(2, method.getParameterCount());
    }
}
