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

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.Move;
import com.rttnghs.mejn.strategy.BaseStrategy;
import com.rttnghs.mejn.strategy.Strategy;

/**
 * A Ranking strategy uses a function to evaluate (rank) a move. Each move gets
 * a certain value applied and the move with the highest ranking score is
 * picked.
 *
 */
public abstract class RankingStrategy extends BaseStrategy implements Strategy {

	private static final Logger logger = LogManager.getLogger(RankingStrategy.class);
	
	private final BiFunction<Move, Supplier<BoardState>, Integer> moveEvaluator;

	public RankingStrategy(BiFunction<Move, Supplier<BoardState>, Integer> moveEvaluator, String name,
			Collection<Integer> parameters) {
		super(name, parameters);
		this.moveEvaluator = moveEvaluator;
	}

	/**
	 * Let BaseStrategy deal with null, empty, or single value choices. We deal with
	 * multiple choices below.
	 */
	@Override
	public Move choose(List<Move> choices, Supplier<BoardState> stateSupplier) {
		return autoChoose(choices, stateSupplier);
	}

	@Override
	public Move multiChoose(List<Move> choices, Supplier<BoardState> stateSupplier) {

		SortedMap<Integer, Move> rankedMoves = new TreeMap<>();
		for (Move move : choices) {
			rankedMoves.put(moveEvaluator.apply(move, stateSupplier), move);
		}
        logger.trace("rankedMoves:{}", rankedMoves);
		return rankedMoves.get(rankedMoves.lastKey());
	}

}
