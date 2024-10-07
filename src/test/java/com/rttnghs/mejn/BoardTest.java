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

import java.util.Arrays;

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

}
