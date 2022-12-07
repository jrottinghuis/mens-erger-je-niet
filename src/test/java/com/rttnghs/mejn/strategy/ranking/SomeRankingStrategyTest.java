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
package com.rttnghs.mejn.strategy.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.Move;
import com.rttnghs.mejn.Position;
import com.rttnghs.mejn.TestBoardState;
import com.rttnghs.mejn.internal.BaseHistory;
import com.rttnghs.mejn.strategy.Strategy;

class SomeRankingStrategyTest {

	/**
	 * Some extra plumbing to make use of the static {@link #of(String) method in
	 * TestBoardState} that is meant only for test cases.
	 */
	class MyTestBoardState extends TestBoardState {
		static BoardState myOf(String boardStateString) {
			return of(boardStateString);
		}

		public MyTestBoardState(int boardSize, int pawnsPerPlayer, List<Position> beginPositions) {
			super(boardSize, pawnsPerPlayer, beginPositions);
		}

	}

	@Test
	final void test() {
		Strategy strategy = new SomeRankingStrategy("someName", Arrays.asList(-90, 20, 80, -5, 10, 1));

		assertEquals("someName", strategy.getName());

		// Nothing should happen here.
		strategy.initialize(null);
		strategy.initialize(new BaseHistory<Move>().getSupplier(Move.shifter(0, 40)));

		List<Move> choices = null; // <O3->O4>, <O19->H20>
		Supplier<BoardState> supplier = () -> MyTestBoardState.myOf("(40)[P0={H0};P1={B4}]");
		assertThrows(IllegalArgumentException.class, () -> strategy.choose(null, supplier));

		choices = Arrays.asList(Move.of("<E3->E4>"), Move.of("<E19->H20>"));

		Supplier<BoardState> supplier2 = () -> MyTestBoardState
				.myOf("(40)[P0={H0,H1,H2,H3};P1={B4,B4,B4,E25};P2={E3,E19,H22,H23};P3={B24,B24,E34,H33}]");
		Move choice = strategy.choose(choices, supplier2);
		assertEquals("<E19->H20>", choice.toString());

		// Assume somebody else is at E4
		Supplier<BoardState> supplier3 = () -> MyTestBoardState
				.myOf("(40)[P0={E3,E19,H22,H23};P1={B4,B4,B4,E25};P2={E4,H1,H2,H3};P3={B24,B24,E34,H33}]");
		choice = strategy.choose(choices, supplier3);
		assertEquals(Move.of("<E19->H20>"), choice);

		// Confirm we prefer moving home first
		choices = Arrays.asList(Move.of("<E13->E14>"), Move.of("<E19->H20>"), Move.of("<H21->H22>"));
		Supplier<BoardState> supplier4 = () -> MyTestBoardState
				.myOf("(40)[P0={E13,E19,H21,H23};P1={B4,E4,H12,H13};P2={H0,H1,H2,H3};P3={B24,B24,B24,H30}]");
		choice = strategy.choose(choices, supplier4);
		assertEquals(Move.of("<E19->H20>"), choice);

		// Confirm we prefer moving home first
		choices = Arrays.asList(Move.of("<E38->E39>"), Move.of("<E39->H0>"), Move.of("<H3->H4>"));
		Supplier<BoardState> supplier5 = () -> MyTestBoardState
				.myOf("(40)[P0={B34,E38,E39,H3};P1={B4,E4,H12,H13};P2={B14,B14,B14,B14};P3={B24,B24,B24,H30}]");
		choice = strategy.choose(choices, supplier5);
		assertEquals(Move.of("<E39->H0>"), choice);

	}

}
