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

import static com.rttnghs.mejn.Layer.BEGIN;
import static com.rttnghs.mejn.Layer.EVENT;
import static com.rttnghs.mejn.Layer.HOME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 
 */
class LayerTest {

	@Test
	void test() {
		assertEquals(-1, BEGIN.number);
		assertEquals(0, EVENT.number);
		assertEquals(1, HOME.number);

		assertTrue(BEGIN.compareTo(EVENT) < 0, "comparable");
		assertTrue(EVENT.compareTo(HOME) < 0, "comparable");
		assertTrue(HOME.compareTo(BEGIN) > 0, "comparable");

		assertTrue(BEGIN.compareTo(BEGIN) == 0, "comparable");
	}

	@Test
	void testNext() {
		assertEquals(EVENT, BEGIN.next());
		assertEquals(HOME, EVENT.next());
		assertThrows(IllegalStateException.class, () -> HOME.next());
	}

	@Test
	void testNumber() {
		assertEquals(-1, BEGIN.number);
		assertEquals(0, EVENT.number);
		assertEquals(1, HOME.number);
	}

	@Test
	void testToChar() {
		assertEquals('B', BEGIN.toChar());
		assertEquals('E', EVENT.toChar());
		assertEquals('H', HOME.toChar());
	}

	@Test
	void testOf() {
		assertEquals(BEGIN, Layer.of('B'));
		assertEquals(EVENT, Layer.of('E'));
		assertEquals(HOME, Layer.of('H'));
		assertNull(Layer.of('K'));
	}
}
