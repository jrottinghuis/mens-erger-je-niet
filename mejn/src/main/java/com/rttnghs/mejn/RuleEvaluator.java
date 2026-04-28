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

import com.rttnghs.mejn.configuration.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Used to apply the rules, determine if moves are allowed etc.
 */
public class RuleEvaluator {

    public static final boolean isSelfStrikeAllowed = Config.configuration.getBoolean("isSelfStrikeAllowed");

    /**
     * List of positions where each respective players start from (where the layers
     * intersect).
     */
    private final Position startPosition;

    /**
     * Game rule evaluator for the given player.
     */
    public RuleEvaluator(Position startPosition) {
        this.startPosition = Objects.requireNonNull(startPosition, "startPosition cannot be null");
    }

    /**
     * Not out of bounds.
     *
     * @param move to check
     * @return false for all BEGIN layers that are not the start. True for all
     * other.
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
     * @param move to check
     * @return whether this move moves a pawn onto the start position.
     */
    private boolean isToStart(Move move) {
        return startPosition.equals(move.to());
    }

    /**
     * @param move to check
     * @return if the move is from the start position, which is preferential. over other moves.
     */
    private boolean isFromStart(Move move) {
        return startPosition.equals(move.from());
    }

    /**
     * @param move to be evaluated
     * @return if the from and to are the same.
     */
    private boolean stationary(Move move) {
        return move.from().equals(move.to());
    }

    /**
     * @param move to be evaluated.
     * @return True when moving to begin, true when moving from start. False when
     * EVENT is occupied. Otherwise allowed if you strike yourself and that is
     * allowed per config.
     */
    private boolean isLegalSelfStrike(BoardState state, Move move) {
        if (isFromStart(move) || move.to().layer() == Layer.BEGIN) {
            // Any move from the start or to the begin layer is legal.
            return true;
        }
        if (move.to().layer() == Layer.HOME) {
            // Move to home is legal if the spot is unoccupied.
            return state.getPlayer(move.to()) == -1;
        }
        if (move.to().layer() == Layer.EVENT) {
            // Move to event is legal if self-strike is allowed or the spot is occupied by another player.
            return isSelfStrikeAllowed || state.getPlayer(move.from()) != state.getPlayer(move.to());
        }
        throw new IllegalArgumentException("Unexpected value: " + move.to().layer());
    }

    /**
     * @param state board state used for occupancy/self-strike checks.
     * @param potentialMoves non-null, possibly empty, normalized list of moves.
     * @return a reduced list of moves with any non-optional moves removed. Forced
     * single-move outcomes are returned as immutable singleton lists.
     */
    public List<Move> evaluate(BoardState state, List<Move> potentialMoves) {
        Objects.requireNonNull(state, "state cannot be null");
        Objects.requireNonNull(potentialMoves, "potentialMoves cannot be null");
        List<Move> legalMoves = new ArrayList<>(potentialMoves.size());

        Move possibleToStartMove = null;

        for (Move potentialMove : potentialMoves) {
            // Discard plainly illegal moves.
            if (stationary(potentialMove) || !isInbound(potentialMove) || !isLegalSelfStrike(state, potentialMove)) {
                continue;
            }

            if (isFromStart(potentialMove)) {
                // Mandatory move: when a pawn is on start it must move off start.
                return List.of(potentialMove);
            }

            if (isToStart(potentialMove)) {
                // Remember the to-start move; from-start has higher priority and will return immediately.
                possibleToStartMove = potentialMove;
            }

            legalMoves.add(potentialMove);
        }

        // If the player is not on start, but moving to start is an option, then that
        // option must be taken.
        if (possibleToStartMove != null) {
            return List.of(possibleToStartMove);
        }

        return legalMoves;
    }

}
