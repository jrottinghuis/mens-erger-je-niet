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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.HistorySupplier;
import com.rttnghs.mejn.internal.ShiftingBoardStateSupplier;
import com.rttnghs.mejn.strategy.Strategy;
import com.rttnghs.mejn.strategy.StrategyFactory;

/**
 *
 */
public class Player {

	private static final Logger logger = LogManager.getLogger(Player.class);


	/**
	 * @param strategyFactory to be used to create strategies.
	 * @param strategyNames the list of names of the strategies to get from the strategy factory.
	 * @param historySupplier used to get a thing that supplies a history.
	 * @return list of players, one for each strategy, in order.
	 */
	public static List<Player> playersOf(StrategyFactory strategyFactory, List<String> strategyNames,
			HistorySupplier<Move> historySupplier) {

		List<Player> players = new ArrayList<>(strategyNames.size());
		int boardSize = strategyNames.size() * Config.value.dotsPerPlayer();

		for (int playerIndex = 0; playerIndex < strategyNames.size(); playerIndex++) {
			Player player = null;
			if (strategyNames.get(playerIndex) == null) {
				player = new NullPlayer(playerIndex, boardSize);
			} else {

				// Rotate perspective counter clockwise
				int rotation = rotation(playerIndex);
				Supplier<History<Move>> shiftedHistorySupplier = historySupplier.getSupplier(Move.shifter(rotation, boardSize));
				Strategy strategy = strategyFactory.getStrategy(strategyNames.get(playerIndex)).initialize(shiftedHistorySupplier);
				player = new Player(strategy, playerIndex, boardSize);
			}
			players.add(playerIndex, player);
            logger.debug("Player {} strategy {}", playerIndex, player.getName());
		}
		return players;
	}

	/**
	 * @param playerIndex zero based index where along the board this player sits.
	 * @return how many spots the Moves of a board need to be rotated to put the
	 *         current player start from 0;
	 */
	public static int rotation(int playerIndex) {
		return playerIndex * Config.value.dotsPerPlayer() * -1;
	}

	private final Strategy strategy;
	/**
	 * zero based index where along the board this player sits. Used to shift
	 * positions around for the strategy to think it is in position 0;
	 */
	private final int playerIndex;
	private final int boardSize;

	/**
	 * @param strategy        used to choose moves
	 * @param playerIndex     zero based index where along the board this player
	 *                        sits.
	 * @param boardSize       number of spots on the board.
	 */
	public Player(Strategy strategy, int playerIndex, int boardSize) {
		this.strategy = strategy;
		this.boardSize = boardSize;
		this.playerIndex = playerIndex;
	}

	/**
	 * @return how many spots the Moves of a board need to be rotated to put the
	 *         current player start from 0;
	 */
	public int rotation() {
		return rotation(playerIndex);
	}

	/**
	 * Have the player choose a move from the list of choices.
	 * This method will rotate the board to the perspective of the player.
	 * 
	 * @param choices Could be empty to indicate that there are no choices.
	 * @param state   the state of the board.
	 * @return the move the choices list. Could be null if there were no choices.
	 */
	public Move choose(List<Move> choices, BoardState state) {
		// Rotate perspective counter clockwise
		Supplier<BoardState> bsProvider = new ShiftingBoardStateSupplier(state, playerIndex, rotation());

		if (choices == null) {
			// this should not happen.
			return strategy.choose(new ArrayList<>(0), bsProvider);
		}
		// Shift the move to the perspective where strategy thinks it is player 0;

		List<Move> shiftedChoices = choices.stream().map(Move.shifter(rotation(), boardSize))
				.collect(Collectors.toList());

		Move choice = strategy.choose(shiftedChoices, bsProvider);

		// Shift perspective back, moving board clockwise
		if (choice != null) {
			choice = choice.shift(rotation() * -1, boardSize);
		}
		return choice;
	}

	/**
	 * Inform player to finalize because the game is over.
	 * 
	 * @param position this player finished in. First place = 1, runner up, 2, etc.
	 */
	public void finalize(int position) {
		strategy.finalize(position);
		logger.debug(() -> strategy.getName() + " position " + position);
	}

	/**
	 * @return the name of the strategy this player is using.
	 */
	public String getName() {
		return strategy.getName();
	}

}
