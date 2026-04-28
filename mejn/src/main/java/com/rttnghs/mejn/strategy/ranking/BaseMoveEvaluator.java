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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.Move;

public abstract class BaseMoveEvaluator implements BiFunction<Move, BoardState, Integer> {

	/**
	 * Parameters to be used for the strategy. Could be empty if so configured in
	 * the config file.
	 */
	protected final List<Integer> parameters = new ArrayList<>();

	public BaseMoveEvaluator(Collection<Integer> parameters) {
		if (parameters != null) {
			this.parameters.addAll(parameters);
		}
	}

	@Override
	public Integer apply(Move move, BoardState boardState) {
		return valuate(move, boardState);
	}

	/**
	 * Provide a numeric valuation for a move. The move with the highest valuation
	 * will be chosen.
	 * 
	 * @param move          to be valuated.
	 * @param boardState	the current state of the board.
	 * @return the valuation (Integer value) for this move, given the supplied
	 *         state.
	 */
	abstract public Integer valuate(Move move, BoardState boardState);

}
