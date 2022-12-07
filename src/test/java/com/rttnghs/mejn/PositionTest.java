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
import static com.rttnghs.mejn.Layer.HOME;
import static com.rttnghs.mejn.Layer.EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/**
 * Tests for Position class.
 */
class PositionTest {

	private static final Logger logger = LogManager.getLogger(PositionTest.class);

	final static Position start = new Position(EVENT, 0);
	final static Position otherStart = new Position(EVENT, 0);
	final static Position aBegin = new Position(BEGIN, -6);
	final static Position three = new Position(EVENT, 3);
	final static Position forty = new Position(EVENT, 40);

	@Test
	void testEquals() {
		assertTrue(start.equals(start));
		assertTrue(otherStart.equals(otherStart));
		assertTrue(start.equals(otherStart));
		assertTrue(otherStart.equals(start));

		assertFalse(aBegin.equals(start));
		assertFalse(aBegin.equals(null));
	}

	@Test
	void testCompareTo() {
		assertTrue(start.compareTo(start) == 0);
		assertTrue(otherStart.compareTo(otherStart) == 0);

		assertTrue(start.compareTo(otherStart) == 0);
		assertTrue(otherStart.compareTo(start) == 0);

		assertTrue(aBegin.compareTo(start) < 0);
		assertTrue(aBegin.compareTo(three) < 0);
		assertTrue(start.compareTo(three) < 0);
		assertTrue(start.compareTo(aBegin) > 0);
		assertTrue(three.compareTo(aBegin) > 0);
		assertTrue(three.compareTo(start) > 0);
	}

	@Test
	void testOf() {
		Position e0 = new Position(EVENT, 0);
		assertTrue(e0.equals(Position.of("E0")));
		assertTrue(Position.of("E0").equals(e0.move(0)));
		assertTrue(e0.move(8).equals(Position.of("E8")));

		assertEquals(e0, Position.of(e0.toString()));
		assertEquals(new Position(BEGIN, 1), Position.of("B1"));
		assertEquals(new Position(EVENT, -1), Position.of("E-1"));
		assertEquals(new Position(HOME, 24), Position.of("H24"));

		assertNull(Position.of(null));
		assertNull(Position.of(""));
		assertNull(Position.of("H"));
		assertNull(Position.of("MI6"));
		assertNull(Position.of("D0"));
		assertNull(Position.of("HH"));
		assertNull(Position.of("HH"));
	}

	@Test
	void testNormalize() {
		// Positions with positive spots map to themselves
		assertTrue(start.equals(start.normalize(40)));
		assertTrue(start.equals(start.normalize(48)));
		assertTrue(three.equals(three.normalize(40)));
		assertTrue(three.equals(three.normalize(48)));
		assertTrue(forty.equals(forty.normalize(48)));

		// Positions with negative positions do not map to themselves
		assertFalse(aBegin.equals(aBegin.normalize(40)));
		assertFalse(aBegin.equals(aBegin.normalize(48)));

		// wrapped is not equal to itself
		assertFalse(forty.equals(forty.normalize(40)));

		// Forty wraps to start in a 40 board.
		assertTrue(forty.normalize(40).equals(start));
		assertTrue(start.equals(forty.normalize(40)));

		// Double-normalized is equal to itself.
		assertTrue(forty.normalize(40).equals(forty.normalize(40)));

		// Positions are identical to themselves (same object) if not wrapped
		assertTrue(start == (start.normalize(40)));
		assertTrue(three == (three.normalize(40)));

		// Confirm wrapping with of methods.
		assertTrue(new Position(EVENT, 37).move(5).equals(Position.of("E42")));
	}

	@Test
	public void testIsBetween() {
		Position bMin6 = new Position(BEGIN, -6);
		Position e0 = new Position(EVENT, 0);
		Position e1 = new Position(EVENT, 1);
		Position e10 = new Position(EVENT, 10);
		Position e17 = new Position(EVENT, 17);
		Position h1 = new Position(HOME, 1);
		Position h11 = new Position(HOME, 11);
		Position h13 = Position.of("H13");

		// Positions are not in between themselves
		assertFalse(bMin6.isBetween(bMin6, bMin6));
		assertFalse(e17.isBetween(e17, e17));
		assertFalse(h13.isBetween(h13, h13));

		assertTrue(e0.isBetween(bMin6, e10));
		assertTrue(e0.isBetween(bMin6, e17));
		assertTrue(e0.isBetween(bMin6, h13));
		assertTrue(h1.isBetween(e1, h11));

		assertTrue(h1.isBetween(e1, h1));

		assertFalse(h13.isBetween(e1, e10));

		assertFalse(h13.isBetween(h13, null));
		assertFalse(h13.isBetween(null, h13));
		assertFalse(h13.isBetween(null, null));
	}

	@Test
	void testNextLayer() {
		Position bMin6 = new Position(BEGIN, -6);
		Position b0 = new Position(BEGIN, 0);
		Position e0 = new Position(EVENT, 0);
		Position e10 = new Position(EVENT, 10);
		Position e17 = new Position(EVENT, 17);
		Position h1 = new Position(HOME, 1);
		Position h11 = new Position(HOME, 11);
		Position h13 = Position.of("H13");

		assertEquals(new Position(EVENT, bMin6.spot()), bMin6.nextLayer());
		assertEquals(e0, b0.nextLayer());
		assertEquals(new Position(HOME, e0.spot()), e0.nextLayer());

		assertEquals(new Position(HOME, e0.spot()), e0.nextLayer());
		assertEquals(new Position(HOME, e10.spot()), e10.nextLayer());
		assertEquals(new Position(HOME, e17.spot()), e17.nextLayer());

		assertEquals(EVENT, BEGIN.next());

		assertThrows(IllegalStateException.class, () -> h1.nextLayer());
		assertThrows(IllegalStateException.class, () -> h11.nextLayer());
		assertThrows(IllegalStateException.class, () -> h13.nextLayer());
	}

}
