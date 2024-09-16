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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class DieTest {

	private static final Logger logger = LogManager.getLogger(DieTest.class);

	/**
	 * Test method for {@link com.rttnghs.mejn.Die#Die(int)}.
	 */
	@Test
	public final void testDie() {

		// This should be fine
		new Die(1);
		new Die(2);
		new Die(6);
		new Die(10);

		// Cannot have die with negative numbers
		assertThrows(IllegalArgumentException.class, () -> new Die(0));
		assertThrows(IllegalArgumentException.class, () -> new Die(-1));
		assertThrows(IllegalArgumentException.class, () -> new Die(-17));
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.Die#roll()}.
	 */
	@Test
	public final void testRoll() {
		Die die = new Die(6);
		int[] freq = new int[die.faces]; // creating an array to compute frequency of each face
		int val;
		int chance = 1;
		int rolls = 100000;
		Instant start = Instant.now();
		// rolling the dice many times
		while (chance <= rolls) {
			val = die.roll();
			assertTrue(val > 0);
			assertTrue(val <= die.faces);
			++freq[val - 1];
			chance++;
		}
		Instant end = Instant.now();
		Duration interval = Duration.between(start, end);

        logger.trace("Rolled {} times in {} millis", rolls, interval.toMillis());

		int sum = 0;
		for (int i = 1; i <= die.faces; i++) {
			sum += i * freq[i - 1];
            logger.trace("Side: {}-> Frequency : {}", i, freq[i - 1]);
		}
        logger.trace("Average: {}", (float) sum / rolls);

	}

}
