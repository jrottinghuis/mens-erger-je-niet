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
package com.rttnghs.mejn.internal;

import java.util.function.Supplier;

import com.rttnghs.mejn.BoardState;

/**
 * Shifting a board state is an expensive operation. Some strategies never look
 * at a BoardState, and most of them don't look at the state when there are
 * fewer than two choices. This class makes a lazy approach possible. It
 * supplies a board state in a position shifted to the perspective of the player
 * being in position 0;
 */
public class ShiftingBoardStateSupplier implements Supplier<BoardState> {

	/**
	 * Calculated in a lazy fashion.
	 */
	private final BoardState boardState;
	private final int playerIndex;
	private final int shift;

	BoardState shiftedBoardState = null;

	/**
	 * @param boardState  original board state.
	 * @param playerIndex index of the player that should be at position 0 after the
	 *                    shift.
	 * @param shift       number of spots that the state needs to be shifted for the
	 *                    perspective player.
	 */
	public ShiftingBoardStateSupplier(BoardState boardState, int playerIndex, int shift) {
		this.boardState = boardState;
		this.playerIndex = playerIndex;
		this.shift = shift;
	}

	/**
	 * @return the board state shifted for the respective player. The shift is
	 *         lazily calculated to avoid costly operation of shifting the state.
	 */
	@Override
	public BoardState get() {
		if (shiftedBoardState == null) {
			shiftedBoardState = boardState.shift(playerIndex, shift);
		}
		return shiftedBoardState;
	}

}
