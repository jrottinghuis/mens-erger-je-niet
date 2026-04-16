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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.BaseBoardState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

		assertEquals(new Move(contested, playerOneBegin), result.strikeMove());
		assertEquals(1, result.struckPlayer());
		assertEquals(-1, result.finishedPlayer());
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

		assertNull(result.strikeMove());
		assertNull(result.struckPlayer());
		assertEquals(0, result.finishedPlayer());
		assertEquals(0, board.getBoardState().getPlayer(new Position(HOME, 0)));
	}

	@Test
	final void testMoveNullAndInvalidAreNeutralAndDoNotChangeState() {
		int dotsPerPlayer = Config.value.dotsPerPlayer();
		int dieFaces = Config.value.dieFaces();
		int boardSize = 2 * dotsPerPlayer;

		Position playerZeroBegin = new Position(BEGIN, -dieFaces).normalize(boardSize);
		Position playerOneBegin = new Position(BEGIN, -dieFaces + dotsPerPlayer).normalize(boardSize);
		List<Position> beginPositions = new ArrayList<>(List.of(playerZeroBegin, playerOneBegin));

		BaseBoardState state = new BaseBoardState(boardSize, dotsPerPlayer, 1, beginPositions);
		Board board = new Board(Arrays.asList("strategy1", "strategy2"), new Die(dieFaces), state, 0, 1);

		String before = board.getBoardState().toString();

		Board.MoveResult nullResult = board.move(null);
		assertNull(nullResult.move());
		assertNull(nullResult.strikeMove());
		assertNull(nullResult.struckPlayer());
		assertEquals(-1, nullResult.finishedPlayer());
		assertEquals(before, board.getBoardState().toString());

		Move invalidMove = new Move(new Position(HOME, 7), new Position(HOME, 13));
		Board.MoveResult invalidResult = board.move(invalidMove);
		assertEquals(invalidMove, invalidResult.move());
		assertNull(invalidResult.strikeMove());
		assertNull(invalidResult.struckPlayer());
		assertEquals(-1, invalidResult.finishedPlayer());
		assertEquals(before, board.getBoardState().toString());
	}

}
