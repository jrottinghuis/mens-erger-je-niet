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
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(start, otherStart);
        assertEquals(otherStart, start);

        assertNotEquals(aBegin, start);
        assertNotEquals(null, aBegin);
	}

	@Test
	void testCompareTo() {
        assertEquals(0, start.compareTo(start));
        assertEquals(0, otherStart.compareTo(otherStart));

        assertEquals(0, start.compareTo(otherStart));
        assertEquals(0, otherStart.compareTo(start));

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
        assertEquals(e0, Position.of("E0"));
        assertEquals(Position.of("E0"), e0.move(0));
        assertEquals(e0.move(8), Position.of("E8"));

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
        assertEquals(start, start.normalize(40));
        assertEquals(start, start.normalize(48));
        assertEquals(three, three.normalize(40));
        assertEquals(three, three.normalize(48));
        assertEquals(forty, forty.normalize(48));

		// Positions with negative positions do not map to themselves
        assertNotEquals(aBegin, aBegin.normalize(40));
        assertNotEquals(aBegin, aBegin.normalize(48));

		// wrapped is not equal to itself
        assertNotEquals(forty, forty.normalize(40));

		// Forty wraps to start in a 40 board.
        assertEquals(forty.normalize(40), start);
        assertEquals(start, forty.normalize(40));

		// Double-normalized is equal to itself.
        assertEquals(forty.normalize(40), forty.normalize(40));

		// Positions are identical to themselves (same object) if not wrapped
        assertSame(start, (start.normalize(40)));
        assertSame(three, (three.normalize(40)));

		// Confirm wrapping with of methods.
        assertEquals(new Position(EVENT, 37).move(5), Position.of("E42"));
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

		assertThrows(IllegalStateException.class, h1::nextLayer);
		assertThrows(IllegalStateException.class, h11::nextLayer);
		assertThrows(IllegalStateException.class, h13::nextLayer);
	}

}
