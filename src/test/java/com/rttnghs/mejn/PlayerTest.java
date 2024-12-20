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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.BaseHistory;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;

class PlayerTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@Test
	final void testPlayersOf() {
		final BaseHistory<Move> history = new BaseHistory<>(512);

		String bracketStrategyNamesAttribute = Config.configuration.getString("tournamentBrackets[@strategies]");
		List<String> bracketStrategyNames = new ArrayList<>(
				Arrays.asList(bracketStrategyNamesAttribute.split(",", -1)));

		List<Player> players = Player.playersOf(new BaseStrategyFactory(), bracketStrategyNames, history);
		assertEquals(4, players.size());
	}
}
