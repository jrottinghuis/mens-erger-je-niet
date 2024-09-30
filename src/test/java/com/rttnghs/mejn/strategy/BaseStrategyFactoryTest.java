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
package com.rttnghs.mejn.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.rttnghs.mejn.configuration.Config;

class BaseStrategyFactoryTest {

	@Test
	final void testGetStrategies() {
		StrategyFactory strategyFactory = new BaseStrategyFactory();

		Strategy strategy;

		String bracketStrategyNamesAttribute = Config.configuration.getString("tournamentBrackets[@strategies]");
		List<String> bracketStrategyNames = new ArrayList<>(
				Arrays.asList(bracketStrategyNamesAttribute.split(",", -1)));

		for (String strategyName : bracketStrategyNames) {
			strategy = strategyFactory.getStrategy(strategyName);
			assertEquals(strategyName, strategy.getName());
		}

		// System.out.println(strategyFactory.getStrategies());
	}

	@Test
	final void testGetStrategy() {
		BaseStrategyFactory strategyFactory = new BaseStrategyFactory();
		Strategy strategy = strategyFactory.getStrategy("RankingStrategy",
				"com.rttnghs.mejn.strategy.ranking.SomeRankingStrategy", "-1,20,30,-40,50,60");
		assertEquals("RankingStrategy", strategy.getName());

		assertThrows(IllegalArgumentException.class, () -> strategyFactory.getStrategy(null));
	}

}
