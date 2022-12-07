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
package com.rttnghs.mejn.strategy;

import java.util.List;
import java.util.function.Supplier;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.History;
import com.rttnghs.mejn.Move;

/**
 * This is the thing to implement that determines what a player does during the
 * game.
 */
public interface Strategy {

	/**
	 * @return the name of this strategy. This is expected not to change during the
	 *         lifetime of the strategy.
	 */
	public String getName();

	/**
	 * Strategies shall implement a initialize method. This will be called before
	 * any calls to {@link #choose(List, BoardStateSupplier)}.
	 * 
	 * @param historySupplier supplier that can be used to get the history of moves.
	 *                        <p>
	 *                        Note that this supplier will return a consistent
	 *                        result only during the
	 *                        {@link #choose(List, BoardStateSupplier)} and
	 *                        {@link #finalize(int)} methods. If the strategy
	 *                        implements threading, this history provider does not
	 *                        give any guarantee that other threads will be able to
	 *                        get a consistent view of the history.
	 * @return reference to self for convenient chaining of calls.
	 */
	public Strategy initialize(Supplier<History<Move>> historySupplier);

	/**
	 * Note that the perspective will be that this player is player 0.
	 * 
	 * @param choices           board positions from which to choose. Shall not be
	 *                          null. Empty list indicates that there is not choice.
	 * @param boardStateSuplier used to get to the state of the board. Access only
	 *                          when needed.
	 * @return one of the choices in the list, or else get a random choice assigned.
	 * 
	 */
	public Move choose(List<Move> choices, Supplier<BoardState> boardStateSupplier);

	/**
	 * Inform strategy to finalize because the game is over.
	 * 
	 * @param position this player finished in. First place = 1, runner up, 2, etc.
	 */
	public void finalize(int position);

}
