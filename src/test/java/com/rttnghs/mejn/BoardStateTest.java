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
import static com.rttnghs.mejn.TestBoardState.getMove;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/**
 *
 */
class BoardStateTest {

	private static final Logger logger = LogManager.getLogger(BoardStateTest.class);

	/**
	 * Test method for
	 * {@link com.rttnghs.mejn.BoardState#BoardState(int, int, java.util.List)}.
	 */
	@Test
	final void testBoardState() {

		assertThrows(IllegalStateException.class, () -> new BoardState(40, 4, null));

		List<Position> beginPositionsFour = new ArrayList<>(4);
		assertThrows(IllegalStateException.class, () -> new BoardState(40, 4, beginPositionsFour));

		beginPositionsFour.add(new Position(BEGIN, 34));
		beginPositionsFour.add(new Position(BEGIN, 4));
		beginPositionsFour.add(new Position(BEGIN, 14));
		beginPositionsFour.add(new Position(BEGIN, 24));
		BoardState boardState = new BoardState(40, 4, beginPositionsFour);

		BoardState boardStateTwo = new BoardState(40, 2, beginPositionsFour);
		BoardState boardStateFour = new BoardState(40, 4, beginPositionsFour);

		assertThrows(IllegalStateException.class, () -> new BoardState(40, 0, beginPositionsFour));
		assertThrows(IllegalStateException.class, () -> new BoardState(40, -6, beginPositionsFour));

		List<Position> beginPositionsSix = new ArrayList<>(6);
		beginPositionsSix.add(new Position(BEGIN, 42));
		beginPositionsSix.add(new Position(BEGIN, 2));
		beginPositionsSix.add(new Position(BEGIN, 10));
		beginPositionsSix.add(new Position(BEGIN, 18));
		beginPositionsSix.add(new Position(BEGIN, 26));
		beginPositionsSix.add(new Position(BEGIN, 34));
		BoardState boardStateSix = new BoardState(48, 6, beginPositionsSix);

		// Test the equality
		assertTrue(boardState.equals(boardState));
		assertTrue(boardState.equals(boardStateFour));
		assertTrue(boardStateSix.equals(boardStateSix));

		assertFalse(boardState.equals(boardStateTwo));
		assertFalse(boardState.equals(boardStateSix));
		assertFalse(boardStateFour.equals(boardStateSix));

		assertFalse(boardStateFour.equals(boardStateSix));

		// Confirm that the string representations are same for equal and different for
		// not equal.
		assertTrue(boardState.toString().equals(boardState.toString()));
		assertTrue(boardState.toString().equals(boardStateFour.toString()));
		assertTrue(boardStateSix.toString().equals(boardStateSix.toString()));

		assertFalse(boardState.toString().equals(boardStateTwo.toString()));
		assertFalse(boardState.equals(boardStateSix));
		assertFalse(boardStateFour.toString().equals(boardStateSix.toString()));

		assertFalse(boardStateFour.toString().equals(boardStateSix.toString()));

		// For debugging purposes
		logger.trace(boardStateSix);
	}

	@Test
	final void testGetters() {
		List<Position> beginPositionsFour = new ArrayList<>(4);
		beginPositionsFour.add(new Position(BEGIN, 34));
		beginPositionsFour.add(new Position(BEGIN, 4));
		BoardState boardStateTwo = new BoardState(40, 2, beginPositionsFour);

		beginPositionsFour.add(new Position(BEGIN, 14));
		beginPositionsFour.add(new Position(BEGIN, 24));
		BoardState boardState = new BoardState(40, 4, beginPositionsFour);

		assertEquals(4, boardState.getPawnsPerPlayer());
		assertEquals(4, boardState.getPlayerCount());
		assertEquals(40, boardState.getBoardSize());

		// Test that if we mess with the original input list, that doesn't affect the
		// previously created state. Ignote the incorrect begin position for this test.
		beginPositionsFour.add(new Position(BEGIN, 44));
		BoardState boardStateFive = new BoardState(40, 7, beginPositionsFour);
		assertEquals(4, boardState.getPawnsPerPlayer());
		assertEquals(4, boardState.getPlayerCount());
		assertEquals(7, boardStateFive.getPawnsPerPlayer());
		assertEquals(5, boardStateFive.getPlayerCount());

		assertEquals(2, boardStateTwo.getPawnsPerPlayer());
		assertEquals(2, boardStateTwo.getPlayerCount());
		assertEquals(40, boardStateTwo.getBoardSize());

		beginPositionsFour.add(new Position(BEGIN, 54));
		beginPositionsFour.add(new Position(BEGIN, 64));
		BoardState boardStateSeven = new BoardState(40, 3, beginPositionsFour);
		assertEquals(3, boardStateSeven.getPawnsPerPlayer());
		assertEquals(7, boardStateSeven.getPlayerCount());
		assertEquals(40, boardStateSeven.getBoardSize());

		List<Position> beginPositionsSix = new ArrayList<>(6);
		beginPositionsSix.add(new Position(BEGIN, 42));
		beginPositionsSix.add(new Position(BEGIN, 2));
		beginPositionsSix.add(new Position(BEGIN, 10));
		beginPositionsSix.add(new Position(BEGIN, 18));
		beginPositionsSix.add(new Position(BEGIN, 26));
		beginPositionsSix.add(new Position(BEGIN, 34));
		BoardState boardStateSix = new BoardState(48, 9, beginPositionsSix);
		assertEquals(9, boardStateSix.getPawnsPerPlayer());
		assertEquals(6, boardStateSix.getPlayerCount());
		assertEquals(48, boardStateSix.getBoardSize());

	}

	@Test
	final void testEquals() {

		List<Position> beginPositionsFour = new ArrayList<>(4);
		beginPositionsFour.add(new Position(BEGIN, 34));
		beginPositionsFour.add(new Position(BEGIN, 4));
		beginPositionsFour.add(new Position(BEGIN, 14));
		beginPositionsFour.add(new Position(BEGIN, 24));
		BoardState boardState = new BoardState(40, 4, beginPositionsFour);

		BoardState anotherBoardState = new BoardState(40, 4, beginPositionsFour);

		assertTrue(boardState.equals(anotherBoardState));
		assertTrue(boardState.equals(boardState));
		assertTrue(anotherBoardState.equals(boardState));

		assertEquals(boardState, anotherBoardState);

		assertEquals(boardState.hashCode(), anotherBoardState.hashCode());

		List<Position> beginPositionsSix = new ArrayList<>(6);
		beginPositionsSix.add(new Position(BEGIN, 42));
		beginPositionsSix.add(new Position(BEGIN, 2));
		beginPositionsSix.add(new Position(BEGIN, 10));
		beginPositionsSix.add(new Position(BEGIN, 18));
		beginPositionsSix.add(new Position(BEGIN, 26));
		beginPositionsSix.add(new Position(BEGIN, 34));
		BoardState boardStateSix = new BoardState(48, 6, beginPositionsSix);

		assertNotEquals(boardState, boardStateSix);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	final void testEqualsNull() {

		List<Position> beginPositionsFour = new ArrayList<>(4);
		beginPositionsFour.add(new Position(BEGIN, 34));
		beginPositionsFour.add(new Position(BEGIN, 4));
		beginPositionsFour.add(new Position(BEGIN, 14));
		beginPositionsFour.add(new Position(BEGIN, 24));
		BoardState boardState = new BoardState(40, 4, beginPositionsFour);

		// Test nulls
		assertFalse(boardState.equals(null));
		assertFalse(boardState.equals(this)); // Different type on purpose.
	}

	@Test
	final void testGetPosition() {
		List<Position> beginPositionsFour = new ArrayList<>(4);
		beginPositionsFour.add(new Position(BEGIN, 34));
		beginPositionsFour.add(new Position(BEGIN, 4));
		beginPositionsFour.add(new Position(BEGIN, 14));
		beginPositionsFour.add(new Position(BEGIN, 24));
		BoardState boardState = new BoardState(40, 4, beginPositionsFour);

		Position pZeroZero = boardState.getPosition(0, 0);
		Position pTwoTwo = boardState.getPosition(2, 2);
		Position pThreeOne = boardState.getPosition(3, 1);

		assertTrue(pZeroZero.equals(new Position(BEGIN, 34)));
		assertFalse(pZeroZero.equals(pTwoTwo));
		assertFalse(pThreeOne.equals(pTwoTwo));
		assertTrue(pTwoTwo.equals(new Position(BEGIN, 14)));
		assertTrue(boardState.getPosition(1, 1).equals(boardState.getPosition(1, 3)));

		assertEquals(null, boardState.getPosition(-1, 1));
		assertEquals(null, boardState.getPosition(-3, 5));
		assertEquals(null, boardState.getPosition(0, 4));
		assertEquals(null, boardState.getPosition(1, 4));
		assertEquals(null, boardState.getPosition(-1, -1));
		assertEquals(null, boardState.getPosition(0, -1));
		assertEquals(null, boardState.getPosition(1, -1));

		logger.trace(boardState);
	}

	@Test
	final void testUnmodifiable() {
		List<Position> beginPositionsFour = new ArrayList<>(4);
		beginPositionsFour.add(new Position(BEGIN, 34));
		beginPositionsFour.add(new Position(BEGIN, 4));
		beginPositionsFour.add(new Position(BEGIN, 14));
		beginPositionsFour.add(new Position(BEGIN, 24));
		BoardState boardState = new BoardState(40, 4, beginPositionsFour);
		List<Position> positions = boardState.getPositions(0);

		assertThrows(UnsupportedOperationException.class, () -> positions.add(new Position(HOME, 0)));

		assertThrows(UnsupportedOperationException.class, () -> positions.add(0, new Position(HOME, 0)));
	}

	@Test
	final void testGetPlayer() {
		List<Position> beginPositionsFour = new ArrayList<>(4);
		beginPositionsFour.add(new Position(BEGIN, 34));
		beginPositionsFour.add(new Position(BEGIN, 4));
		beginPositionsFour.add(new Position(BEGIN, 14));
		beginPositionsFour.add(new Position(BEGIN, 24));
		BoardState boardState = new BoardState(40, 4, beginPositionsFour);

		assertEquals(2, boardState.getPlayer(new Position(BEGIN, 14)));
		assertEquals(1, boardState.getPlayer(new Position(BEGIN, 4)));
		assertEquals(3, boardState.getPlayer(new Position(BEGIN, 24)));
		assertEquals(0, boardState.getPlayer(new Position(BEGIN, 34)));

		// Test the negative cases
		assertEquals(-1, boardState.getPlayer(null));
		assertEquals(-1, boardState.getPlayer(new Position(BEGIN, 17)));
		assertEquals(-1, boardState.getPlayer(new Position(EVENT, 13)));
		assertEquals(-1, boardState.getPlayer(new Position(HOME, 11)));
	}

	@Test
	final void testMove() {
		List<Position> beginPositionsTwo = new ArrayList<>(2);
		beginPositionsTwo.add(new Position(BEGIN, 14));
		beginPositionsTwo.add(new Position(BEGIN, 4));
		BoardState boardState = new BoardState(40, 3, beginPositionsTwo);
		BoardState boardStateCopy = new BoardState(40, 3, beginPositionsTwo);
		assertEquals(boardState, boardStateCopy);

		logger.trace(boardState);

		assertEquals(boardState, boardState.move(null));
		// Move from a position that is not in the state should result in identical
		// state.
		assertEquals(boardState, boardState.move(new Move(new Position(HOME, 7), new Position(HOME, 13))));

		Position from = boardState.getPosition(0, 0);
		Move inPlace = new Move(from, from);
		assertEquals(boardState, boardState.move(inPlace));

		Position start = new Position(EVENT, 0);
		Move move = new Move(from, start);
		assertEquals(start, boardState.move(move).getPosition(0, 2));
		// Confirm original boardstate hasn't changed.
		assertEquals(boardState, boardStateCopy);

		assertNotEquals(boardState, boardState.move(move));
		boardState = boardState.move(move);
		assertNotEquals(boardState, boardStateCopy);
		assertNotEquals(boardState, boardState.move(move));

		boardState = boardState.move(getMove(BEGIN, 4, EVENT, 10));
		assertEquals(new Position(EVENT, 10), boardState.move(move).getPosition(1, 2));
		boardState = boardState.move(getMove(EVENT, 10, EVENT, 11));
		assertEquals(new Position(EVENT, 11), boardState.move(move).getPosition(1, 2));
		boardState = boardState.move(getMove(BEGIN, 4, EVENT, 10));
		boardState = boardState.move(getMove(EVENT, 10, HOME, 1));

		assertEquals(new Position(BEGIN, 4), boardState.move(move).getPosition(1, 0));
		assertEquals(new Position(EVENT, 11), boardState.move(move).getPosition(1, 1));
		assertEquals(new Position(HOME, 1), boardState.move(move).getPosition(1, 2));

		// (40)[P0={B14,B14,E0};P1={B4,E11,H1}]
		boardState = boardState.move(getMove(EVENT, 0, EVENT, 1));
		boardState = boardState.move(getMove(BEGIN, 14, EVENT, 0));
		// (40)[P0={B14,E0,E1};P1={B4,E11,H1}]
		// Now move out0 to out2 and ensure the proper order
		boardState = boardState.move(getMove(EVENT, 0, EVENT, 3));
		String expected = "(40)[P0={B14,E1,E3};P1={B4,E11,H1}]";
		assertEquals(expected, boardState.toString());

		logger.trace(boardState);
	}

	@Test
	final void testOf() {
		BoardState bs = TestBoardState
				.of("(40)[P0={H0,H1,H2,H3};P1={B4,B4,B4,E25};P2={E16,E19,H22,H23};P3={B24,B24,E34,H33}]");
		assertEquals(40, bs.getBoardSize());
		assertEquals(4, bs.getPawnsPerPlayer());
		assertEquals(4, bs.getPlayerCount());

		bs = TestBoardState.of("(20)[P0={B14,E1,E3};P1={B4,E11,H1}]");
		assertEquals(20, bs.getBoardSize());
		assertEquals(3, bs.getPawnsPerPlayer());
		assertEquals(2, bs.getPlayerCount());

		bs = TestBoardState.of("(7)[P0={B2}]");
		assertEquals(7, bs.getBoardSize());
		assertEquals(1, bs.getPawnsPerPlayer());
		assertEquals(1, bs.getPlayerCount());
	}

}
