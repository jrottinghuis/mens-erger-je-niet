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
package com.rttnghs.mejn.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class ScoreTest {

	private static final Logger logger = LogManager.getLogger(ScoreTest.class);

	@Test
	final void testGetScore() {

		assertEquals(0, Score.get(0, 0));
		assertEquals(0, Score.get(-1, 0));
		assertEquals(0, Score.get(-1, 2));
		assertEquals(0, Score.get(-1, 4));

		assertEquals(0, Score.get(4, 4));
		assertEquals(0, Score.get(7, 4));

		assertEquals(126, Score.get(0, 4));
		assertEquals(63, Score.get(1, 4));
		assertEquals(24, Score.get(2, 4));
		assertEquals(1, Score.get(3, 4));

		assertEquals(1019, Score.get(0, 10));
		assertEquals(1019, Score.get(0, 11));
		assertEquals(1019, Score.get(0, 37));

		assertEquals(1, Score.get(9, 10));

		// More players than there is a score.
		assertEquals(1019, Score.get(0, 11));
		assertEquals(326, Score.get(4, 13));
		assertEquals(1, Score.get(9, 11));

		assertEquals(0, Score.get(10, 17));
		assertEquals(0, Score.get(13, 17));

	}

	@Test
	final void testGetWinningScore() {
		assertEquals(126, Score.winningScore(4));
		assertEquals(1, Score.winningScore(1));

		assertEquals(0, Score.winningScore(0));
		assertEquals(0, Score.winningScore(-3));

		assertEquals(1019, Score.winningScore(512));
	}

	@Test
	final void testAddGenerics() {

		Integer one = Integer.valueOf(1);
		short two = 2;
		Short tooShort = Short.valueOf(two);
		long three = 3;
		Long notLongEnough = Long.valueOf(three);
		Long beforeTooLong = Long.valueOf(4);
		Long tooLong = Long.valueOf(5);
		List<Long> aLongList = new ArrayList<>(Arrays.asList(beforeTooLong, tooLong));
	}

}
