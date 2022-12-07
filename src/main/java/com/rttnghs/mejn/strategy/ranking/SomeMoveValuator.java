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

import static com.rttnghs.mejn.Layer.HOME;
import static com.rttnghs.mejn.Layer.EVENT;

import java.util.Collection;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.Move;

/**
 * Coding up some common rules used. Aiming to have the parameters in the range
 * from -100 to 100. Therefore boardsize (commonly 40) is divided by 10 to get
 * some reasonable range in parameters (otherwise a change from -1 to 0 to 1
 * would be a very large change in total score.
 */
public class SomeMoveValuator extends BaseMoveEvaluator {

	private static final Logger logger = LogManager.getLogger(SomeMoveValuator.class);

	/**
	 * How important is it to strike self (probably a negative value is better here.
	 */
	private final int selftStrikeParam;
	/**
	 * How important is it to strike others
	 */
	private final int otherStrikeParam;
	/**
	 * How important it is to get home when you have the chance.
	 */
	private final int gettingHomeParam;
	/**
	 * How important is it to move a pawn already home
	 */
	private final int alreadyHomeParam;
	/**
	 * How important is it to be far from start
	 */
	private final int fromStartParam;
	/**
	 * How important is it to be far from home
	 */
	private final int fromHomeParam;

	public SomeMoveValuator(Collection<Integer> params) {
		super(params);
		if (parameters.size() < 5) {
			throw new IllegalArgumentException("SomeMoveValuator needs to have at least 5 parameters.");
		}
		this.selftStrikeParam = this.parameters.get(0);
		this.otherStrikeParam = this.parameters.get(1);
		this.gettingHomeParam = this.parameters.get(2);
		this.alreadyHomeParam = this.parameters.get(3);
		this.fromStartParam = this.parameters.get(4);
		this.fromHomeParam = this.parameters.get(5);
	}

	/**
	 * Strong preference not to punch one self in the face.
	 * <p>
	 * Note that we're counting on the player to projects the board state to make us
	 * player 0;
	 * 
	 * @param move
	 * @return
	 */
	private int valuateSelfStrike(Move move, Supplier<BoardState> stateSupplier) {
		// Determine who is at the to position.
		int player = stateSupplier.get().getPlayer(move.to());
		return switch (player) {
		case -1 -> 0; // nobody to strike
		// Striking yourself is as bad as the board position the
		// target is on. Note the plus one to avoid the 0*anything = 0;
		case 0 -> (selftStrikeParam * (move.to().spot() + 1)) / 10;
		default -> otherStrikeParam; // Striking somebody is some positive value
		};
	}

	/**
	 * Preference to make it home if the move is to HOME and from EVENT layer.
	 * 
	 * @param valuation
	 * @return
	 */
	private int valuateHome(Move move, Supplier<BoardState> stateSupplier) {
		if (move.to().layer() == HOME) {
			if (move.from().layer() == EVENT) {
				// Coming home is as good as the board is long plus how far we'd get into home.
				return (gettingHomeParam * stateSupplier.get().getBoardSize()) / 2;
			} else {
				// Best to tinker with pawns already home as last resort.
				return alreadyHomeParam;
			}
		}
		// Otherwise this rule doesn't change the valuation
		return 0;
	}

	/**
	 * Determine value based on how far along the board this pawn is.
	 * 
	 * @param move
	 * @return
	 */
	private int valueateSpot(Move move, Supplier<BoardState> stateSupplier) {
		if (move.to().layer() == EVENT) {
			// Inversely proportional to how far the pawn is. Hence prefer to move the last
			// pawn first
			// return (config.playerCount() * config.dotsPerPlayer()) - move.to().spot();

			// How important it is to be far from start. Add one to avoid multiply by 0 = 1;
			int fromStartWeight = (fromStartParam * (move.to().spot() + 1)) / 10;
			// How important it is to be far from home
			int fromHomehWeight = (fromHomeParam * (stateSupplier.get().getBoardSize() + 1 - move.to().spot())) / 10;
			return fromStartWeight + fromHomehWeight;
			// Total points after 10000 games: {RandomStrategy=1234984,
			// SomeRankingStrategy=905016}
			// Total wins after 10000 games: {RandomStrategy=4254, SomeRankingStrategy=5746}
			// Expected: 535000

			// Try to give this a random positive value, in other words, randomly pick the
			// move.
			// Die die = new Die(config.playerCount() * config.dotsPerPlayer());
			// return die.roll();
			// Total points after 10000 games: {RandomStrategy=1436134,
			// SomeRankingStrategy=703866}
			// Total wins after 10000 games: {RandomStrategy=6179, SomeRankingStrategy=3821}
			// Expected: 535000
		}
		// Otherwise this rule doesn't change the valuation
		return 0;
	}

	@Override
	public Integer valuate(Move move, Supplier<BoardState> stateSupplier) {
		// Add them all up.
		Integer value = valuateSelfStrike(move, stateSupplier) + valuateHome(move, stateSupplier)
				+ valueateSpot(move, stateSupplier);
		return value;
	}

}
