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

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rttnghs.mejn.configuration.Config;

/**
 * Used to apply the rules, determine if moves are allowed etc.
 */
public class RuleEvaluator {

	public static boolean isSelfStrikeAllowed = Config.configuration.getBoolean("isSelfStrikeAllowed");
	private static final Logger logger = LogManager.getLogger(RuleEvaluator.class);

	private final BoardState state;
	/**
	 * List of positions where each respective players start from (where the layers
	 * intersect).
	 */
	private final Position startPosition;

	/**
	 * Game rule evaluator for the given player.
	 * 
	 * @param state against which to evaluate moves
	 */
	public RuleEvaluator(BoardState state, Position startPosition) {
		this.state = state;
		this.startPosition = startPosition;
	}

	/**
	 * Not out of bounds.
	 * 
	 * @param move
	 * @return false for all BEGIN layers that are not the start. True for all
	 *         other.
	 */
	private boolean isInbound(Move move) {
		return switch (move.to().layer()) {
		case BEGIN -> isToStart(move);
		case EVENT -> true;
		case HOME -> move.to().spot() < startPosition.spot() + Config.value.pawnsPerPlayer();
		default -> throw new IllegalArgumentException("Unexpected value: " + move.to().layer());
		};
	}

	/**
	 * @param move
	 * @return
	 */
	private boolean isToStart(Move move) {
		return startPosition.equals(move.to());
	}

	/**
	 * @param move
	 * @return
	 */
	private boolean isFromStart(Move move) {
		return startPosition.equals(move.from());
	}

	/**
	 * 
	 * @param move to be evaluated
	 * @return if the from and to are the same.
	 */
	private boolean stationary(Move move) {
		return move.from().equals(move.to());
	}

	/**
	 * @param move to be evaluated.
	 * @return True when moving to begin, true when moving from start. False when
	 *         EVENT is occupied. Otherwise allowed if you strike yourself and that is
	 *         allowed per config.
	 */
	private boolean isLegalSelfStrike(Move move) {
		if (isFromStart(move)) {
			// Any move from the start is legal.
			return true;
		}
		return switch (move.to().layer()) {
		case BEGIN -> true; // Any move to begin is legal
		case EVENT -> isSelfStrikeAllowed || (state.getPlayer(move.from()) != state.getPlayer(move.to()));
		case HOME -> (state.getPlayer(move.to()) == -1) ? true : false; // If nobody is in the to spot, that is legal.
		default -> throw new IllegalArgumentException("Unexpected value: " + move.to().layer());
		};
	}

	/**
	 * @param potentialMoves non-null, possibly empty, normalized list of moves.
	 * @param roll
	 * @return
	 */
	public List<Move> evaluate(List<Move> potentialMoves, int roll) {
		// Eliminate any illegal moves
		List<Move> legalMoves = potentialMoves.stream().filter(not(this::stationary)).filter(this::isInbound)
				.filter(this::isLegalSelfStrike).collect(Collectors.toList());

		// If the player is on start, that must be the move they choose.
		List<Move> possibleFromStartMove = legalMoves.stream().filter(this::isFromStart).collect(Collectors.toList());
		if (possibleFromStartMove.size() == 1) {
			// legalMoves contained a to-start move. possibleStartMove now contains it.
			// logger.trace(() -> "Forcing from-start move: " + possibleFromStartMove);
			return possibleFromStartMove;
		}

		// If the player is not on start, but moving to start is an option, then that
		// option must be taken.
		List<Move> possibleToStartMove = legalMoves.stream().filter(this::isToStart).collect(Collectors.toList());
		if (possibleToStartMove.size() == 1) {
			// legalMoves contained a to-start move. possibleStartMove now contains it.
			// logger.trace(() -> "Forcing to-start move: " + possibleToStartMove);
			return possibleToStartMove;
		}

		return legalMoves;
	}

}
