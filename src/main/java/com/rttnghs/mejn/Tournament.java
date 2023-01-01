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
package com.rttnghs.mejn;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.statistics.EventCounter;
import com.rttnghs.mejn.statistics.Score;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import com.rttnghs.mejn.strategy.StrategyFactory;

/**
 * Play multiple games and get the stats.
 */
public class Tournament {

	private static final Logger logger = LogManager.getLogger(Tournament.class);

	private final StrategyFactory strategyFactory;
	private final List<String> strategyNames;
	private final int games;
	private final EventCounter<String, Integer> finishCounts = new EventCounter<>();

	public Tournament(StrategyFactory strategyFactory, List<String> strategyNames, int games) {
		// If strategyFactory == null then use the base factory.
		this.strategyFactory = strategyFactory;
		this.strategyNames = strategyNames;
		this.games = games;
	}

	public EventCounter<String, Integer> play() {
		logger.info("Starting " + games + " games: " + Config.value + " Strategies: " + strategyNames);
		for (int i = 0; i < games; i++) {
			Game game = new Game(strategyFactory, strategyNames);
			EventCounter<String, Integer> gameFinishCounts = game.play();
			finishCounts.add(gameFinishCounts);
		}
		logger.debug(finishCounts);
		return finishCounts;
	}

	public static List<String> getStrategyNames() {
		String bracketStrategyNamesAttribute = Config.configuration.getString("brackets[@strategies]");
		List<String> bracketStrategyNames = new ArrayList<>(
				Arrays.asList(bracketStrategyNamesAttribute.split(",", -1)));
		return bracketStrategyNames;
	}

	/**
	 * @param bracketStrategyNames list of strategy names to be playing against one
	 *                             another.
	 * @return a list of brackets. A bracket is a list of strategies to play against
	 *         one another, which is some permutation of the given names in the
	 *         bracketStrategyNames as configured in the strategy config file.
	 *         <p>
	 *         Note that each list of strategy names can be shorter than the total
	 *         list of strategies to pull from. In other words, not all strategies
	 *         have to play n each game.
	 */
	public static List<List<String>> getStrategyNameBrackets(List<String> bracketStrategyNames) {
		List<List<String>> strategyNameBrackets = new ArrayList<>();

		List<String> bracketConfigurations = Config.configuration.getList(String.class, "brackets.bracket");

		// Function that takes an index (from the bracket) and does a lookup in the list
		// of bracketStrategyNames
		Function<Integer, String> bracketStrategyNameMapper = index -> bracketStrategyNames.get(index);

		for (String bracketString : bracketConfigurations) {
			List<String> bracketList = Stream.of(bracketString.split(",", -1)).map(String::trim).map(Integer::parseInt)
					.map(bracketStrategyNameMapper).collect(Collectors.toList());
			strategyNameBrackets.add(bracketList);
		}
		return strategyNameBrackets;

	}

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		Instant start = Instant.now();

		EventCounter<String, Integer> finishCounts = new EventCounter<>();

		List<String> strategyNames = getStrategyNames();
		List<List<String>> strategyNameBrackets = getStrategyNameBrackets(strategyNames);

		List<CompletableFuture<EventCounter<String, Integer>>> futures = new ArrayList<>(strategyNameBrackets.size());

		for (List<String> strategyNameBracket : strategyNameBrackets) {
			Tournament tournament = new Tournament(new BaseStrategyFactory(), strategyNameBracket,
					Config.configuration.getInt("games"));
			CompletableFuture<EventCounter<String, Integer>> future = CompletableFuture
					.supplyAsync(() -> tournament.play());
			futures.add(future);
		}

		for (CompletableFuture<EventCounter<String, Integer>> completableFuture : futures) {
			finishCounts.add(completableFuture.get());
		}
		// Assume all brackets have the same number of players.
		int playerCount = strategyNameBrackets.get(0).size();
		Function<Integer, Integer> scorer = (finishPosition) -> Score.get(finishPosition, playerCount);
		// Multiply by 100, to keep a reasonable resolution when dividing by games
		// as the smallest score for each game is 1, and we don't want to loose too much
		// precision without having to go to floats of doubles.
		// More accuracy isn't needed, because of the randomness of the games, which
		// causes more variance in score that this rounding error.
		Map<String, Integer> scores = EventCounter.getNormalizedScores(finishCounts, scorer, 100);

		Duration interval = Duration.between(start, Instant.now());
		logger.info(finishCounts);
		logger.info(scores);
		logger.info("Tournament took " + interval.toMillis() + " millis");
	}

}
