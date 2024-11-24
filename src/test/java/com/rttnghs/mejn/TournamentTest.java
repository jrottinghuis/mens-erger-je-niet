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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.rttnghs.mejn.statistics.EventCounter;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;

class TournamentTest {

	private static final Logger logger = LogManager.getLogger(TournamentTest.class);

	@Test
	final void test() {
		Instant start = Instant.now();
		int games = 2;
		Tournament tournament = new Tournament(new BaseStrategyFactory(),
				Arrays.asList("FarStrategy", "RandomStrategy", "NearStrategy"), games);
		EventCounter<String, Integer> tournamentResults = tournament.play();
        logger.info("History {} games: {}", games, tournamentResults);
		Duration interval = Duration.between(start, Instant.now());
        logger.info("Tournament took {} millis", interval.toMillis());
	}

	@Test
	final void testGetStrategyNames() {
		List<String> strategyNames = Tournament.getStrategyNames();
		assertEquals(4, strategyNames.size());
	}

	@Test
	final void testGetStrategyNameBrackets() {
		List<String> strategyNames = Tournament.getStrategyNames();
		List<List<String>> strategyNameBrackets = Tournament.getStrategyNameBrackets(strategyNames);
		assertEquals(4, strategyNameBrackets.getFirst().size());
	}

	@Test
	final void testMain() throws InterruptedException, ExecutionException {
		Tournament.main(null);
	}
}
