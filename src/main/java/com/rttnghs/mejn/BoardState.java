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

import static com.rttnghs.mejn.Layer.HOME;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A board state is essentially a list of positions for each player. The list of
 * positions will be kept in order.
 * <p>
 * Note that this class is immutable. This means that the state can be
 * passed around without the risk of modifications having a side-effect on
 * others using the same state.
 */
public class BoardState {

	private static final Logger logger = LogManager.getLogger(BoardState.class);

	/**
	 * This state is meant to be immutable and should not be modified.
	 */
	private final List<List<Position>> state;
	private final int boardSize;
	private final int pawnsPerPlayer;

	/**
	 * @param boardSize      The number of spots in the Event layer of the board.
	 * @param pawnsPerPlayer how many pawns each player should have. Should be >0;
	 * @param beginPositions List of  begin positions for each player with the length of
	 *                       number of players. Cannot be null; If the list of a player is empty, the player is considered finished.
	 */
	public BoardState(int boardSize, int pawnsPerPlayer, List<Position> beginPositions) {
		if ((beginPositions == null) || beginPositions.isEmpty() || pawnsPerPlayer < 1) {
			throw new IllegalStateException(
					"Cannot create BoardState with empty/null beginpositions or < 1 pawns per player");
		}
		this.boardSize = boardSize;
		this.pawnsPerPlayer = pawnsPerPlayer;
		List<List<Position>> newState = new ArrayList<>(beginPositions.size());
		// Iterate over player begin positions and expand them to the pawnsPerPlayer
		for (int i = 0; i < beginPositions.size(); i++) {
			ArrayList<Position> playerState = new ArrayList<>(pawnsPerPlayer);
			Position beginPosition = beginPositions.get(i);
			if (beginPosition != null) {
				for (int j = 0; j < pawnsPerPlayer; j++) {
					playerState.add(j, beginPosition);
				}
			}
			newState.add(i, Collections.unmodifiableList(playerState));
		}
		state = Collections.unmodifiableList(newState);
	}

	/**
	 * For private use only.
	 *
	 * @param boardSize  The number of spots in the Event layer of the board.
	 * @param otherState to start this new board state with. Each sub-list
	 *              (playerPositions) must have the same length.
	 */
	protected BoardState(List<List<Position>> otherState, int boardSize, int pawnsPerPlayer) {
		this.boardSize = boardSize;
		this.pawnsPerPlayer = pawnsPerPlayer;
		List<List<Position>> newStateCopy = new ArrayList<>(otherState.size());
        for (List<Position> positions : otherState) {
            List<Position> playerState = new ArrayList<>(positions);
            playerState.sort(Position::compareTo);
            newStateCopy.add(Collections.unmodifiableList(playerState));
        }
		this.state = Collections.unmodifiableList(newStateCopy);
	}

	/**
	 * @return the total number of spots in the EVENT layer of the board.
	 */
	public int getBoardSize() {
		return boardSize;
	}

	/**
	 * @return the pawnsPerPlayer
	 */
	public int getPawnsPerPlayer() {
		return state.getFirst().size();
	}

	/**
	 * @return the playerCount
	 */
	public int getPlayerCount() {
		return state.size();
	}

	/**
	 * @param player index of player in the state
	 * @param pawn   index of the pawn in the state
	 * @return the position of the given pawn for the given player or null if there
	 *         is no such pawn.
	 */
	public Position getPosition(int player, int pawn) {
		if ((player < 0) || (player >= state.size())) {
			// out of bound for player
			return null;
		}
		List<Position> playerState = state.get(player);
		if ((pawn < 0) || (pawn >= playerState.size())) {
			return null;
		}
		return playerState.get(pawn);
	}

	/**
	 * Return (unmodifiable) list of positions for the given player.
	 * 
	 * @param player index of player in the state
	 * @return the list of positions for the given player, or null if there is no
	 *         such player.
	 */
	public List<Position> getPositions(int player) {
		if ((player < 0) || (player >= state.size())) {
			// out of bound for player
			return null;
		}
		return state.get(player);
	}

	@Override
	public int hashCode() {
		return Objects.hash(boardSize, state);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BoardState other = (BoardState) obj;
		return boardSize == other.boardSize && Objects.equals(state, other.state);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("(").append(boardSize).append(")[");

		for (int i = 0; i < state.size(); i++) {
			str.append("P").append(i).append("={");
			List<Position> playerState = state.get(i);
			for (int j = 0; j < playerState.size(); j++) {
				str.append(playerState.get(j));
				if (j + 1 < playerState.size()) {
					str.append(",");
				}
			}
			str.append("}");
			// Print semi-colon only between players
			if (i + 1 < state.size()) {
				str.append(";");
			}
		}
		str.append("]");
		return str.toString();
	}

	/**
	 * @param position to locate in the state lists.
	 * @return the zero based index of the player that is at the given position for
	 *         the state or -1 if there is no such player.
	 */
	public int getPlayer(Position position) {
		if (position == null) {
			return -1;
		}
		for (int i = 0; i < state.size(); i++) {
			List<Position> playerState = state.get(i);
			if (playerState.contains(position)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @param move assumed to be a valid move
	 * @return new state with the move applied. Will be the same as this state if
	 *         move is null, or to/from Positions in the move are null. Same when
	 *         there is no player at the given move.from() position.
	 */
	public BoardState move(Move move) {
		if ((move == null) || (move.from() == null) || (move.to() == null)) {
			return this;
		}
		if (move.from() == move.to()) {
			// nobody is moving anywhere
			return this;
		}
		// Determine who's pawn we're moving
		int player = getPlayer(move.from());
		if (player == -1) {
			// No such move
			return this;
		}
		List<Position> oldPlayerState = state.get(player);
		List<Position> newPlayerState = getPositions(move, oldPlayerState);
		// Determine if the newPlayerState is in order, most of the time it will be
		// Note that this seems some unnecessarily complicated logic.
		// The initial version of this method was more straightforward, but performance
		// profiling showed that a little optimization was in order.
		boolean outOfOrder = false;
		for (int i = 1; i < oldPlayerState.size(); i++) {
			// if any previous item is bigger than the current one, the list is out of order
			if (newPlayerState.get(i - 1).compareTo(newPlayerState.get(i)) > 0) {
				outOfOrder = true;
				// Stop on first out of order item.
				break;
			}
		}
		if (outOfOrder) {
			newPlayerState.sort(Comparator.naturalOrder());
		}

		List<List<Position>> newState = new ArrayList<>(state.size());
		for (int i = 0; i < state.size(); i++) {
			List<Position> playerState = state.get(i);
			if (i != player) {
				newState.add(new ArrayList<>(playerState));
			} else {
				newState.add(new ArrayList<>(newPlayerState));
			}
		}
		return new BoardState(newState, boardSize, pawnsPerPlayer);
	}

	private static List<Position> getPositions(Move move, List<Position> oldPlayerState) {
		List<Position> newPlayerState = new ArrayList<>(oldPlayerState.size());
		// Count backwards, and replace the first match on move.from(). This is useful
		// to replace only one move from BEGIN layer, and keep list sorted.
		int replacedIndex = -1;
		for (int i = oldPlayerState.size() - 1; i >= 0; i--) {
			Position oldPosition = oldPlayerState.get(i);
			if ((replacedIndex < 0) && oldPosition.equals(move.from())) {
				newPlayerState.add(move.to());
				replacedIndex = i;
			} else {
				newPlayerState.add(oldPosition);
			}
		}
		return newPlayerState;
	}

	/**
	 * @param playerIndex index of the player to shift the perspective for.
	 * @param shift       how many spots this board state should be shifted by.
	 * @return shifted perspective of the board.
	 */
	public BoardState shift(int playerIndex, int shift) {
		if (shift == 0) {
			// Happens for each player 0 turn
			return this;
		}
		// Start at player i, then move along modulo number of players.
		int p = playerIndex;
		List<List<Position>> newState = new ArrayList<>(state.size());
		// Here i is just to count the number of players. p is the actual index to grab.
		// For example, for four players and playerIndex initialized to 2, p is element
		// of {2, 3, 0, 1}
		for (int i = 0; i < state.size(); i++) {
			List<Position> playerState = state.get(p);
			List<Position> newPlayerState = new ArrayList<>(playerState.size());
			for (Position position : playerState) {
				newPlayerState.add(position.move(shift).normalize(boardSize));
			}
			newState.add(newPlayerState);
			p = (p + 1) % state.size();
		}
		return new BoardState(newState, boardSize, pawnsPerPlayer);
	}

	/**
	 * @param player non-null collection with positions.
	 * @return true if all Positions in the collection are in the HOME layer or the player has no pawns. Non-existing players are also considered finished.
	 */
	public boolean isFinished(int player) {
		// Consider non-existing players as finished.
		if ((player < 0) || (state.get(player).isEmpty())) {
			return true;
		}

		// Either player has zero pawn, or pawnsPerPlayer
		if (state.get(player).size() != pawnsPerPlayer) {
			throw new RuntimeException("Player " + player + " has " + state.get(player).size() + " pawns, but should have " + pawnsPerPlayer);
		}


		// Check if all pawns are in the HOME layer, return result on the first one that is not.
		for (int i = 0; i < pawnsPerPlayer; i++) {
			if (getPosition(player, i).layer() != HOME) {
				return false;
			}
		}
		// logger.debug(()-> "isFinished true " + player);
		return true;
	}

}
