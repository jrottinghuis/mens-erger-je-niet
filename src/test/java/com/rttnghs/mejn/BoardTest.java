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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class BoardTest {

	@Test
	public final void testBoard() {
		Board board = new Board(2);
		assertNotNull(board.getBoardState());
	}

	@Test
	public final void testPotentialMove() {
		Board board = new Board(4);

		/*
		 * Move move = board.potentialMove(0, 0, 0); assertEquals(Position.of(BEGIN,
		 * 34), move.to());
		 * 
		 * move = board.potentialMove(0, 0, 1); assertEquals(Position.of(BEGIN, 35),
		 * move.to());
		 * 
		 * move = board.potentialMove(0, 0, 5); assertEquals(Position.of(BEGIN, 39),
		 * move.to());
		 * 
		 * move = board.potentialMove(0, 0, 6); assertEquals(Position.of(EVENT, 0),
		 * move.to());
		 * 
		 * move = board.potentialMove(0, 0, 7); assertEquals(Position.of(EVENT, 1),
		 * move.to());
		 * 
		 * move = board.potentialMove(1, 2, 7); assertEquals(Position.of(EVENT, 11),
		 * move.to());
		 * 
		 * move = board.potentialMove(2, 1, 6); assertEquals(Position.of(EVENT, 20),
		 * move.to());
		 * 
		 * move = board.potentialMove(3, 3, 8); assertEquals(Position.of(EVENT, 32),
		 * move.to());
		 * 
		 * logger.trace(move);
		 */
	}

}
