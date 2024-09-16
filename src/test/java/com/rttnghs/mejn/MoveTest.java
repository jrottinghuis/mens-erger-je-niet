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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class MoveTest {
	private static final Logger logger = LogManager.getLogger(MoveTest.class);

	@Test
	final void testOf() {
		Move beeOneOhOne = Move.of("<B1->E1>");
		assertEquals(beeOneOhOne, Move.of(beeOneOhOne.toString()));

		Position bMin6 = new Position(BEGIN, -6);
		Position b0 = new Position(BEGIN, 0);
		Move move1 = new Move(bMin6, b0);
		assertEquals(move1, Move.of("<B-6->B0>"));
		assertNotNull(Move.of("<B-24->B-4>"));
		assertNull(Move.of(null));

		assertNull(Move.of(""));
		assertNull(Move.of(null));
		assertNull(Move.of("<B-24->B-25"));
		assertNull(Move.of("B-24->B-25>"));

		assertNull(Move.of("B1"));
		assertNull(Move.of("B2->B14"));
		assertNull(Move.of("<>"));
		assertNull(Move.of("<"));
		assertNull(Move.of(">"));
		assertNull(Move.of("<B->>"));
		assertNull(Move.of("<E->H>"));
		assertNull(Move.of("<E2->>"));
		assertNull(Move.of("<E32->H>"));
		assertNull(Move.of("<B->E17>"));
		assertNull(Move.of("<B14->E12"));
		assertNull(Move.of("<->>"));
		assertNull(Move.of("<->B1E1>"));

		// Invalid layer
		assertNull(Move.of("<Z1->E13>"));
		assertNull(Move.of("<B22->F1>"));

	}

	@Test
	final void testMove() {
		Position b0 = new Position(BEGIN, 0);
		assertThrows(IllegalArgumentException.class, () -> new Move(null, null));
		assertThrows(IllegalArgumentException.class, () -> new Move(b0, null));
		assertThrows(IllegalArgumentException.class, () -> new Move(null, b0));
	}

	@Test
	final void testCompareTo() {
		Position bMin6 = new Position(BEGIN, -6);
		Position b0 = new Position(BEGIN, 0);
		Position o0 = new Position(EVENT, 0);
		Position o10 = new Position(EVENT, 10);
		Position h1 = new Position(HOME, 1);
		Position h11 = new Position(HOME, 11);
		Position h13 = new Position(HOME, 13);

		Move move1 = new Move(bMin6, b0);
		Move move2 = new Move(b0, o0);
		Move move3 = new Move(o0, o10);
		Move move4 = new Move(h1, h11);
		Move move5 = new Move(h11, h13);

		assertEquals(0, move1.compareTo(move1));
		assertEquals(0, move2.compareTo(move2));
		assertEquals(0, move3.compareTo(move3));
		assertEquals(0, move3.compareTo(move3));
		assertEquals(0, move4.compareTo(move4));
		assertEquals(0, move5.compareTo(move5));

		assertTrue(move1.compareTo(move2) < 0);
		assertTrue(move1.compareTo(move3) < 0);
		assertTrue(move1.compareTo(move4) < 0);
		assertTrue(move1.compareTo(move5) < 0);

		assertTrue(move2.compareTo(move3) < 0);
		assertTrue(move2.compareTo(move4) < 0);
		assertTrue(move2.compareTo(move5) < 0);

		assertTrue(move3.compareTo(move4) < 0);
		assertTrue(move3.compareTo(move5) < 0);

		assertTrue(move4.compareTo(move5) < 0);

		assertTrue(move2.compareTo(move1) > 0);
		assertTrue(move3.compareTo(move1) > 0);
		assertTrue(move4.compareTo(move1) > 0);
		assertTrue(move5.compareTo(move1) > 0);

		assertTrue(move3.compareTo(move2) > 0);
		assertTrue(move4.compareTo(move2) > 0);
		assertTrue(move5.compareTo(move2) > 0);

		assertTrue(move4.compareTo(move3) > 0);
		assertTrue(move5.compareTo(move3) > 0);

		assertTrue(move5.compareTo(move4) > 0);
	}

	@Test
	final void testToString() {
		Position bMin6 = new Position(BEGIN, -6);
		Position b0 = new Position(BEGIN, 0);
		Position e0 = new Position(EVENT, 0);
		// Position e10 = new Position(EVENT, 10);
		// Position e17 = new Position(EVENT, 17);
		// Position h1 = new Position(HOME, 1);
		// Position h11 = new Position(HOME, 11);
		// Position h13 = Position.of(HOME, 13);

		Move move1 = new Move(bMin6, b0);
		Move move2 = new Move(b0, e0);

		logger.trace(move1);
		logger.trace(move2);
	}

	@Test
	final void testShift() {
		Position b34 = new Position(BEGIN, 34);
		Position e0 = new Position(EVENT, 0);
		Position e3 = new Position(EVENT, 3);

		Move move = new Move(e0, e3);
		Move shifted = move.shift(-10, 40);
		assertEquals(move, shifted.shift(10, 40));
		Position e30 = new Position(EVENT, 30);
		Position e33 = new Position(EVENT, 33);
		Move expected = new Move(e30, e33);
		assertEquals(expected, shifted);

		shifted = move.shift(7, 40);
		assertEquals(move, shifted.shift(-7, 40));

		move = new Move(b34, e0);
		shifted = move.shift(-5, 40);
		assertEquals(move, shifted.shift(5, 40));

		move = new Move(b34, e0);
		shifted = move.shift(-10, 40);
		Position b24 = new Position(BEGIN, 24);

		expected = new Move(b24, e30);
		assertEquals(expected, shifted);
	}

}
