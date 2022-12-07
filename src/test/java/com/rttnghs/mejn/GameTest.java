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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.rttnghs.mejn.statistics.EventCounter;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;

class GameTest {

	private static final Logger logger = LogManager.getLogger(GameTest.class);

	@Test
	final void testGame() {
		Instant start = Instant.now();
		logger.info("Starting game: ");
		Game game = new Game(new BaseStrategyFactory(), Arrays.asList("RandomStrategy", "RankingStrategy"));

		EventCounter<String, Integer> results = game.play();
		assertTrue(results.getEvents("RandomStrategy").size() > 0);
		assertTrue(results.getEvents("RankingStrategy").size() > 0);

		Instant end = Instant.now();
		Duration interval = Duration.between(start, end);
		logger.info("Game took " + interval.toMillis() + " millis");
	}

	@Test
	final void testAnotherGame() {
		Instant start = Instant.now();
		logger.info("Starting game: ");
		Game game = new Game(new BaseStrategyFactory(),
				Arrays.asList("FarStrategy", "RandomStrategy", "NearStrategy"));

		EventCounter<String, Integer> results = game.play();
		assertTrue(results.getEvents("RandomStrategy").size() > 0);

		Instant end = Instant.now();
		Duration interval = Duration.between(start, end);
		logger.info("Game took " + interval.toMillis() + " millis");
	}

	@Test
	final void testMain() {
		Game.main(null);
	}

}
