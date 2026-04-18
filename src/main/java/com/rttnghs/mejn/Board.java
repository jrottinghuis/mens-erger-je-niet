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
import com.rttnghs.mejn.internal.BaseBoardState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.rttnghs.mejn.Layer.*;

/**
 * The board is in charge of keeping track of the board state. It also provides
 * a list of potential moves, and can apply a move. Further, the board
 * determines who next player is.
 */
public class Board {

    /**
     * Atomic strike side effect of a move.
     *
     * @param move move that sends the struck player back to begin
     * @param struckPlayer index of the struck player
     */
    public record Strike(Move move, int struckPlayer) {
        public Strike {
            Objects.requireNonNull(move, "move cannot be null");
        }
    }

    /**
     * Result of applying a move from the board perspective.
     *
     * @param move requested move (never null)
     * @param strike atomic strike side effect, if any
     * @param finishedPlayer player index that finished with this move, if any
     */
    public record MoveResult(Move move, Optional<Strike> strike,
                             Optional<Integer> finishedPlayer) {

        /**
         * @throws NullPointerException if {@code move} or any Optional parameter is null
         */
        public MoveResult {
            Objects.requireNonNull(move, "move cannot be null");
            Objects.requireNonNull(strike, "strike cannot be null");
            Objects.requireNonNull(finishedPlayer, "finishedPlayer cannot be null");
        }

        public boolean hasStrike() {
            return strike.isPresent();
        }

        public boolean hasFinished() {
            return finishedPlayer.isPresent();
        }
    }

    private static final Logger logger = LogManager.getLogger(Board.class);

    private final Die die;
    private final int boardSize;

    /**
     * List of positions where each respective player begins from or get struck
     * back to.
     */
    private final List<Position> beginPositions;

    /**
     * List of positions where each respective players start from (where the layers
     * intersect).
     */
    private final List<Position> startPositions;

    private final BaseBoardState state;

    private final int playerCount;
    /**
     * Zero based index into player indicating who's turn it currently is.
     */
    private int currentPlayer;
    private int currentDieValue;
    private int activePlayerCount;

    /**
     * @param strategyNames listing the players to be used on this board. Names can contain nulls, but the list itself must not be null.
     */
    public Board(List<String> strategyNames) {
        this(strategyNames, new Die(Config.value.dieFaces()), null,
                new Die(strategyNames.size()).roll() - 1, null);
    }

    /**
     * Constructor variant used for deterministic tests.
     *
     * @param strategyNames players to place on the board
     * @param die die used for subsequent turns
     * @param initialState initial state to use, or null to create default begin state
     * @param initialCurrentPlayer zero-based player index that starts
     * @param initialDieValue current die value, or null to roll once from {@code die}
     */
    Board(List<String> strategyNames, Die die, BaseBoardState initialState, int initialCurrentPlayer,
          Integer initialDieValue) {
        this.playerCount = strategyNames.size();
        if (die == null) {
            throw new IllegalArgumentException("die cannot be null");
        }
        this.die = die;
        boardSize = playerCount * Config.value.dotsPerPlayer();
        // hang on to begin positions, they are used throughout the game.
        beginPositions = new ArrayList<>(playerCount);
        activePlayerCount = 0;
        for (int i = 0; i < playerCount; i++) {
            Position beginPosition = null;
            if (strategyNames.get(i) != null) {
                // Player 0 begins at -dieFaces. Then player 1 begins dots per player
                // further along on the board.
                int beginIndex = (-1 * Config.value.dieFaces()) + (i * Config.value.dotsPerPlayer());
                beginPosition = new Position(BEGIN, beginIndex).normalize(boardSize);
                activePlayerCount++;
            }
            beginPositions.add(i, beginPosition);
        }
        // Hang on to start positions, they are used throughout the game.
        startPositions = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            // Player 0 starts at 0. Then player 1 starts dots per player
            // further along on the board.
            int startIndex = (i * Config.value.dotsPerPlayer());
            startPositions.add(i, new Position(EVENT, startIndex).normalize(boardSize));
        }

        state = (initialState == null)
                ? new BaseBoardState(boardSize, Config.value.dotsPerPlayer(), Config.value.pawnsPerPlayer(), beginPositions)
                : initialState;

        if ((initialCurrentPlayer < 0) || (initialCurrentPlayer >= playerCount)) {
            throw new IllegalArgumentException("Invalid initialCurrentPlayer: " + initialCurrentPlayer);
        }
        currentPlayer = initialCurrentPlayer;
        currentDieValue = (initialDieValue == null) ? this.die.roll() : initialDieValue;

        // Ensure active player count matches the injected state as well.
        for (int i = 0; i < playerCount; i++) {
            if ((beginPositions.get(i) != null) && state.isFinished(i)) {
                activePlayerCount--;
            }
        }
    }

    /**
     * Determine potentialMove given a pawn of a player and the number of eyes
     * thrown with the die.
     * <p>
     * Note that this move may not be a legal move.
     *
     * @param player must be in range for the players in the state (zero based)
     * @param from   current position for the pawn to be moved
     * @param spots  must be > 0
     * @return move for the given position and die roll, or null when no valid
     *         starting position exists.
     */
    private Move potentialMove(int player, Position from, int spots) {
        if ((from == null) || (player >= startPositions.size())) {
            // Happens when there is no pawn on that state or no startPosition. Likely index
            // out of range.
            return null;
        }
        Position start = startPositions.get(player);
        Position normalizedFrom = from.normalize(boardSize);
        Position to = moveWithinLayer(normalizedFrom, spots);
        if (player == 0) {
            if (to.spot() < normalizedFrom.spot()) {
                // Bump layer, since player 0 wrapped around 0
                to = to.nextLayer();
            }
        } else {
            if (normalizedFrom.spot() < start.spot() && start.spot() <= to.spot()) {
                // For other players, if (from < start <= to)-ignoring layers then they wrapped
                // their start
                to = to.nextLayer();
            }
        }
        return new Move(normalizedFrom, to);
    }

    private Position moveWithinLayer(Position from, int spots) {
        int normalizedSpot = (from.spot() + spots) % boardSize;
        if (normalizedSpot < 0) {
            normalizedSpot += boardSize;
        }
        return new Position(from.layer(), normalizedSpot);
    }

    /**
     * @return non-null, possibly empty list of potential moves
     */
    protected List<Move> getPotentialMoves() {
        List<Move> potentialMoves = new ArrayList<>(Config.value.pawnsPerPlayer());
        Position previousFrom = null;
        for (int i = 0; i < Config.value.pawnsPerPlayer(); i++) {
            Position from = state.getPosition(currentPlayer, i);
            Position normalizedFrom = (from == null) ? null : from.normalize(boardSize);
            if ((normalizedFrom != null) && normalizedFrom.equals(previousFrom)) {
                continue;
            }
            Move potentialMove = potentialMove(currentPlayer, from, currentDieValue);
            if (potentialMove != null) {
                potentialMoves.add(potentialMove);
                previousFrom = normalizedFrom;
            }
        }
        return potentialMoves;
    }

    /**
     * @return non-null list of moves that are allowed for the current player and
     * current die value; may be empty.
     */
    public List<Move> getAllowedMoves() {
        List<Move> potentialMoves = getPotentialMoves();
        // logger.debug(() -> "PotentialMoves: " + potentialMoves);
        RuleEvaluator rulesEvaluator = new RuleEvaluator(state, startPositions.get(currentPlayer));
        return rulesEvaluator.evaluate(potentialMoves, currentDieValue);
    }

    /**
     * Calculates strike side effects for the current board state before mutation.
     *
     * @param move requested move from current player's perspective
     * @return strike move that will be applied by state mutation, or null when no
     * player occupies the destination
     */
    private Move getStrikeMoveForCurrentState(Move move) {
        if ((move == null) || (move.to() == null)) {
            return null;
        }
        int otherPlayer = state.getPlayer(move.to());
        return (otherPlayer == -1) ? null : new Move(move.to(), beginPositions.get(otherPlayer));
    }

    /**
     * @param move non-null move from the board's perspective
     * @return metadata about the applied move, including strike side effects and
     * finishing player if any.
     * @throws NullPointerException when {@code move} is null
     */
    public MoveResult move(Move move) {
        Objects.requireNonNull(move, "move cannot be null");
        Move strikeMove = getStrikeMoveForCurrentState(move);
        Integer struckPlayer = (strikeMove == null) ? null : state.getPlayer(move.to());
        Strike strike = (strikeMove == null) ? null : new Strike(strikeMove, struckPlayer);
        state.move(move);

        Integer finishedPlayer = null;
        if ((move.from() != null) && (move.to() != null)
                && (move.from().layer() == EVENT) && (move.to().layer() == HOME)) {
            if (state.isFinished(currentPlayer)) {
                activePlayerCount--;
                finishedPlayer = currentPlayer;
            }
        }
        return new MoveResult(move, Optional.ofNullable(strike), Optional.ofNullable(finishedPlayer));
    }

    /**
     * @return the index of the current player, or -1 if there are no more active
     * players.
     */
    public int nextPlayer() {
        // logger.trace(() -> "Previous player in nextPlayer " + currentPlayer);
        // Short circuit when we're done
        if (activePlayerCount == 0) {
            logger.trace("No more active players.");
            return -1;
        }

        // If the current player is done
        if (state.isFinished(currentPlayer)) {
            // A Player that just finished might have finished with a die.faces roll,
            // and we might want to let them play again, but they should not.
            currentDieValue = 0; // Reset last roll;
            // logger.trace("Skipping finished player " + currentPlayer);
        }

        // Same player rolls again when they currentDieValue max faces on the die.
        if (currentDieValue != die.faces()) {
            currentPlayer = (currentPlayer + 1) % playerCount;
        }
        //else {
        //    logger.trace(() -> "Player " + currentPlayer + " gets another turn.");
        //}

        if (state.isFinished(currentPlayer)) {
            // This player is done, try the next player.
            // activePlayerCount decrement in move keeps us from infinite loop.
            return nextPlayer();
        }
        currentDieValue = die.roll();
        // logger.trace("Player " + currentPlayer + " rolled " + currentDieValue);
        return currentPlayer;
    }

    /**
     * @return a (non-modifiable) version of the board state
     */
    public BoardState getBoardState() {
        return state;
    }

    /**
     * @return what the die currently shows.
     */
    public int getCurrentDieValue() {
        return currentDieValue;
    }

    /**
     * @return which player is the current player
     */
    public int getCurrentPlayer() {
        return currentPlayer;
    }

}
