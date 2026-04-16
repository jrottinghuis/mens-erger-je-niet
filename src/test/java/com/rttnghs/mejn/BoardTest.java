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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.BaseBoardState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class BoardTest {

	@Test
	public final void testBoard() {
		// Make up two randon names for the strategies.
		Board board = new Board(Arrays.asList("strategy1", "strategy2"));
		assertNotNull(board.getBoardState());

		board = new Board(Arrays.asList("null", "strategy2"));
		assertNotNull(board.getBoardState());

		board = new Board(Arrays.asList("strategy1", "null"));
		assertNotNull(board.getBoardState());

		board = new Board(Arrays.asList("strategy1", "strategy2", "strategy3", "strategy4"));
		assertNotNull(board.getBoardState());

		board = new Board(Arrays.asList("strategy1", "strategy2", "strategy3", "null"));
		assertNotNull(board.getBoardState());

		board = new Board(Arrays.asList("strategy1", "strategy2", "null", "strategy4"));
		assertNotNull(board.getBoardState());

		board = new Board(Arrays.asList("strategy1", "null", "strategy3", "null"));
		assertNotNull(board.getBoardState());

		board = new Board(Arrays.asList("null", "strategy2", "strategy3", "strategy4"));
		assertNotNull(board.getBoardState());
	}

	@Test
	final void testMoveReturnsStrikeOutcomeDeterministically() {
		int dotsPerPlayer = Config.value.dotsPerPlayer();
		int dieFaces = Config.value.dieFaces();
		int boardSize = 2 * dotsPerPlayer;

		Position playerZeroBegin = new Position(BEGIN, -dieFaces).normalize(boardSize);
		Position playerOneBegin = new Position(BEGIN, -dieFaces + dotsPerPlayer).normalize(boardSize);
		List<Position> beginPositions = new ArrayList<>(List.of(playerZeroBegin, playerOneBegin));

		BaseBoardState state = new BaseBoardState(boardSize, dotsPerPlayer, 1, beginPositions);
		Position contested = new Position(EVENT, 3);
		state.move(new Move(playerOneBegin, contested));

		Board board = new Board(Arrays.asList("strategy1", "strategy2"), new Die(dieFaces), state, 0, 1);
		Board.MoveResult result = board.move(new Move(playerZeroBegin, contested));

		assertTrue(result.hasStrike());
		assertFalse(result.hasFinished());
		assertEquals(Optional.of(new Board.Strike(new Move(contested, playerOneBegin), 1)), result.strike());
		assertEquals(Optional.empty(), result.finishedPlayer());
		assertEquals(0, board.getBoardState().getPlayer(contested));
		assertEquals(1, board.getBoardState().getPlayer(playerOneBegin));
	}

	@Test
	final void testMoveReturnsFinishedOutcomeDeterministically() {
		int dotsPerPlayer = Config.value.dotsPerPlayer();
		int dieFaces = Config.value.dieFaces();
		int boardSize = 2 * dotsPerPlayer;

		Position playerZeroBegin = new Position(BEGIN, -dieFaces).normalize(boardSize);
		Position playerOneBegin = new Position(BEGIN, -dieFaces + dotsPerPlayer).normalize(boardSize);
		List<Position> beginPositions = new ArrayList<>(List.of(playerZeroBegin, playerOneBegin));

		BaseBoardState state = new BaseBoardState(boardSize, dotsPerPlayer, 1, beginPositions);
		Position eventBeforeHome = new Position(EVENT, dotsPerPlayer - 1);
		state.move(new Move(playerZeroBegin, eventBeforeHome));

		Board board = new Board(Arrays.asList("strategy1", "strategy2"), new Die(dieFaces), state, 0, 1);
		Board.MoveResult result = board.move(new Move(eventBeforeHome, new Position(HOME, 0)));

		assertFalse(result.hasStrike());
		assertTrue(result.hasFinished());
		assertEquals(Optional.empty(), result.strike());
		assertEquals(Optional.of(0), result.finishedPlayer());
		assertEquals(0, board.getBoardState().getPlayer(new Position(HOME, 0)));
	}

	@Test
	final void testMoveNullThrowsAndInvalidIsNeutral() {
		int dotsPerPlayer = Config.value.dotsPerPlayer();
		int dieFaces = Config.value.dieFaces();
		int boardSize = 2 * dotsPerPlayer;

		Position playerZeroBegin = new Position(BEGIN, -dieFaces).normalize(boardSize);
		Position playerOneBegin = new Position(BEGIN, -dieFaces + dotsPerPlayer).normalize(boardSize);
		List<Position> beginPositions = new ArrayList<>(List.of(playerZeroBegin, playerOneBegin));

		BaseBoardState state = new BaseBoardState(boardSize, dotsPerPlayer, 1, beginPositions);
		Board board = new Board(Arrays.asList("strategy1", "strategy2"), new Die(dieFaces), state, 0, 1);

		String before = board.getBoardState().toString();

		assertThrows(NullPointerException.class, () -> board.move(null));
		assertEquals(before, board.getBoardState().toString());

		Move invalidMove = new Move(new Position(HOME, 7), new Position(HOME, 13));
		Board.MoveResult invalidResult = board.move(invalidMove);
		assertFalse(invalidResult.hasStrike());
		assertFalse(invalidResult.hasFinished());
		assertEquals(invalidMove, invalidResult.move());
		assertEquals(Optional.empty(), invalidResult.strike());
		assertEquals(Optional.empty(), invalidResult.finishedPlayer());
		assertEquals(before, board.getBoardState().toString());
	}

}
