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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.BaseHistory;
import com.rttnghs.mejn.statistics.EventCounter;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import com.rttnghs.mejn.strategy.RandomStrategy;
import com.rttnghs.mejn.strategy.StrategyFactory;

public class Game {

	private static final Logger logger = LogManager.getLogger(Game.class);

	private final Board board;
	private final List<Player> players;
	private final BaseHistory<Move> history = new BaseHistory<>(512);
	private final List<String> finished;
	/**
	 * Agent=player that strikes other player off the board. Event is index of other
	 * player that is struck.
	 */
	private final EventCounter<Integer, Integer> strikes = new EventCounter<>();

	/**
	 * For the finishes EventCounter, the agent is the name of the strategy, and the
	 * event is the 0-based position they finished in.
	 */
	private final EventCounter<String, Integer> finishCounts = new EventCounter<>();

	/**
	 * @param strategyFactory to be used to create strategies for players.
	 * @param strategyNames   The names of the strategies to be used, one per
	 *                        player.
	 */
	public Game(StrategyFactory strategyFactory, List<String> strategyNames) {
		players = Player.playersOf(strategyFactory, strategyNames, history);
		finished = new ArrayList<>(players.size());
		board = new Board(strategyNames.size());
	}

	/**
	 * Play the entire game until all players are done and return the results in the
	 * order of finishing.
	 * 
	 * @return
	 */
	public EventCounter<String, Integer> play() {
		logger.debug("Starting game.");
		while (players.size() > finished.size()) {
			turn();
		}
		for (Player player : players) {
			player.finalize(finished.indexOf(player.getName()));
		}
		for (int i = 0; i < finished.size(); i++) {
			finishCounts.increment(finished.get(i), i);
		}
		logger.debug(finishCounts);
		return finishCounts;
	}

	/**
	 * Take a single turn for the next player. This could result in zero, one, or
	 * two moves. Two moves happen when a player strikes another pawn.
	 */
	private void turn() {
		int currentPlayer = board.nextPlayer();
		if (currentPlayer < 0) {

			// Use lambda in trace method argument to avoid evaluating boarState.toString
			// over and over, which would be slow
			logger.trace(() -> "No more active players: " + board.getBoardState());
			return;
		}

		// logger.debug(() -> "State: " + board.getBoardState());
		List<Move> allowedMoves = board.getAllowedMoves();
		// logger.debug(() -> "AlloweMoves: " + allowedMoves);

		// Let the player choose, whether there is a choice or not (may be useful for
		// some strategies);
		Move choice = players.get(currentPlayer).choose(allowedMoves, board.getBoardState());

		if ((allowedMoves == null) || (allowedMoves.size() == 0)) {
			// No valid moves to make by the player, continue to next player.
			// logger.trace(() -> "No valid moves to choose from: " +
			// board.getBoardState());
			return;
		}
		// There was a valid choice, but the strategy didn't pick it.
		if (!allowedMoves.contains(choice)) {
			// invalid choice, choose random for player
			choice = RandomStrategy.choose(allowedMoves);
		}

		// Note that logger is commented out, because the lambda reference trick () ->
		// to make the argument to the logger lazily evaluated works only with
		// effectively final variables, which choice is not in this case.
		// logger.debug("Player: " + board.getCurrentPlayer() + " chose " + choice);

		Move strike = board.getStrikeMove(choice);
		if (strike != null) {
			int player = board.getBoardState().getPlayer(choice.to());
			strikes.increment(currentPlayer, player);
			logger.debug("Player " + currentPlayer + " strikes " + player + " with " + choice + " forcing " + strike);
			move(strike);
		}
		move(choice);
	}

	private void move(Move move) {
		int finishedPlayer = board.move(move);
		if (finishedPlayer != -1) {
			logger.debug("Finished: " + finishedPlayer);
			finished.add(players.get(finishedPlayer).getName());
		}
		history.add(move);
	}

	/**
	 * Play a single game.
	 * 
	 * @param args
	 */
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Instant start = Instant.now();
		logger.info("Starting game: " + Config.value);
		List<String> strategies = Arrays.asList("RandomStrategy", "FarStrategy", "RankingStrategy",
				"NearStrategy");
		Game game = new Game(new BaseStrategyFactory(), strategies);
		EventCounter<String, Integer> finishCounts = game.play();
		logger.info("Results " + finishCounts);

		Instant end = Instant.now();
		Duration interval = Duration.between(start, end);
		logger.info("Game took " + interval.toMillis() + " millis");
	}

}
