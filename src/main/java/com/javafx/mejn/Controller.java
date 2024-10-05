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
package com.javafx.mejn;

import com.javafx.mejn.strategy.ManualStrategy;
import com.rttnghs.mejn.Board;
import com.rttnghs.mejn.History;
import com.rttnghs.mejn.Move;
import com.rttnghs.mejn.Player;
import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.BaseHistory;
import com.rttnghs.mejn.statistics.EventCounter;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import com.rttnghs.mejn.strategy.RandomStrategy;
import com.rttnghs.mejn.strategy.Strategy;
import com.rttnghs.mejn.strategy.StrategyFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import static com.javafx.mejn.MainApplication.*;

class Controller {
    private static final Logger logger = LogManager.getLogger(Controller.class);
    private static final String MANUAL_STRATEGY = "ManualStrategy";

    private final StrategyFactory strategyFactory;
    final BooleanProperty autoSelectSingleChoice = new SimpleBooleanProperty(true);
    private Board board;
    private TurnStep currentStep = null;
    private BaseHistory<Move> history = new BaseHistory<>(512);
    private final List<String> finished = new ArrayList<>(4);

    /**
     * Agent=player that strikes other player off the board. Event is index of other
     * player that is struck.
     */
    private EventCounter<Integer, Integer> strikes = new EventCounter<>();

    /**
     * For the finishes EventCounter, the agent is the name of the strategy, and the
     * event is the 0-based position they finished in.
     */

    private List<Move> allowedMoves = new ArrayList<>();

    private Task<Move> chooseTask;
    private Move choice;
    Timeline timeline = new Timeline();

    private enum TurnStep {
        NEXT, // show the next player through an empty die
        ROLL, // Show what this player has rolled
        OPTIONS, // Show what options the player can choose from, if any
        SELECTION, // Show which option was selected (if any choice)
        MOVE, // Show the move applied
        FINISHED // The game has finished when all players have all finished
    }

    // create constructor
    public Controller() {
        strategyFactory = new BaseStrategyFactory();
        board = new Board(4);
        addDebugAction();

    }

    private void addDebugAction() {
        // TODO: Remove after debugging
        captureDebugItem.setOnAction(e -> {
            logger.debug("Debugging");
            logger.debug("Current step: {}", currentStep);
            logger.debug("board.player: {}", board.getCurrentPlayer());
            logger.debug("Current Player Index in board view: {}", boardView.getCurrentPlayerIndex());
            logger.debug("Current die value: {}", board.getCurrentDieValue());
            logger.debug("Finished players: {}", finished);
            logger.debug("Strikes: {}", strikes);
            logger.debug("History: {}", history);
            logger.debug("Players: {}", MainApplication.players);
            logger.debug("Allowed moves: {}", allowedMoves);
            logger.debug("Choice: {}", choice);
        });
    }

    /**
     * Initialize the controller.
     *
     * @param scene the scene to add accelerators to. If null, no accelerators are added.
     */
    synchronized void initialize(Scene scene) {
        logger.trace("Controller initializing");

        // Happens only the first time from the MainApp
        if (scene != null) {
            // Load from configuration only once during construction, not during initialization.
            // That would wipe out user selection of strategies on reset.
            MainApplication.strategyOptions.clear();
            MainApplication.strategyOptions.setAll(strategyFactory.listStrategies());

            String strategySelectionAttribute = Config.configuration.getString("gui[@strategies]");
            strategySelections.clear();
            List<String> strategyNames = new ArrayList<>(Arrays.asList(strategySelectionAttribute.split(",", -1)));
            strategySelections.addAll(strategyNames);
            boardView.addAccelerators(scene);

        }

        if (chooseTask != null) {
            chooseTask.cancel();
        }
        pause();

        resetBoardView();

        history = new BaseHistory<>(512);
        finished.clear();
        strikes = new EventCounter<>();

        // Clear the players list
        MainApplication.players.clear();

        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            // Rotate perspective counter clockwise
            int rotation = Player.rotation(playerIndex);
            Supplier<History<Move>> shiftedHistorySupplier = history.getSupplier(Move.shifter(rotation, 40));
            Strategy strategy = strategyFactory.getStrategy(strategySelections.get(playerIndex)).initialize(shiftedHistorySupplier);

            // If strategy is instanceOf ManualStrategy, then set the choice handler
            if (strategy instanceof ManualStrategy manualStrategy) {
                int finalPlayerIndex = playerIndex;
                manualStrategy.setChoiceHandler(positionCompletableFuture -> boardView.setChoiceHandler(positionCompletableFuture, finalPlayerIndex));
            }

            MainApplication.players.add(playerIndex, new Player(strategy, playerIndex, 40));
        }

        // Reset any previous choice.
        choice = null;
        currentStep = null;

        // if timeline was previously created, stop it and create a new one
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        KeyFrame keyFrame = new KeyFrame(Duration.millis(5000), event -> step());
        timeline.getKeyFrames().add(keyFrame);

        timeline.rateProperty().bind(MainApplication.playbackSpeed);
    }

    private void resetBoardView() {
        board = new Board(4);
        boardView.setCurrentPlayerIndex(-1);

        // Reset the selected position in BoardView so that previously set listeners are dropped.
        MainApplication.boardView.resetSelectedPosition();

        // Reset all home and event positions in BoardView
        boardView.homePositions.forEach(player -> {
            player.forEach(position -> {
                position.isChoice(false);
                position.setSelected(false);
                position.setOccupiedBy(-1);
                position.setFinishOrder(-1);
            });
        });
        boardView.eventPositions.forEach(position -> {
            position.isChoice(false);
            position.setSelected(false);
            position.setOccupiedBy(-1);
        });

        // Reset begin positions in BoardView
        for (int player = 0; player < boardView.beginPositions.size(); player++) {
            int finalPlayer = player;
            boardView.beginPositions.get(player).forEach(position -> {
                position.setOccupiedBy(finalPlayer);
                position.isChoice(false);
                position.setSelected(false);
            });
        }
        boardView.isPaused(true);
        boardView.isFinished(false);

        boardView.currentDieValue.set(0);
    }

    /**
     * Wrap things up in preparation for shut-down
     */
    void stop() {
        if (chooseTask != null) {
            chooseTask.cancel();
        }
        pause();
        boardView.setCurrentPlayerIndex(-1);
    }


    // Create a method step that will be called from the UI and that has a switch statement to handle the currentStep
    void step() {
        if (currentStep == null) {
            next();
            return;
        }
        switch (currentStep) {
            case NEXT:
                roll();
                break;
            case ROLL:
                options();
                break;
            case OPTIONS:
                if (choice != null) {
                    selection();
                } else if (allowedMoves.isEmpty()) {
                    // If there are no choices, move to the next player
                    next();
                } else if (allowedMoves.size() == 1 && autoSelectSingleChoice.get()) {
                    // If there is only one choice, just select it when step is called.
                    // Make sure we set the selected position in BoardView so that the selection listeners can unroll properly
                    boardView.setSelectedPosition(allowedMoves.get(0).to(), true);
                } else if (strategySelections.get(board.getCurrentPlayer()).equals(MANUAL_STRATEGY)) {
                    pause();
                }
                break;
            case SELECTION:
                move();
                break;
            case MOVE:
                next();
                break;
            case FINISHED:
                break;
            default:
                logger.error("Unknown step: {}", currentStep);
                next();
        }
    }

    /**
     * Sets the current step to NEXT
     */
    private void next() {
        boardView.setSelectedPosition(null, true);
        int nextPlayer = board.nextPlayer();

        if (nextPlayer < 0) {
            finish();
            return;
        }

        boardView.setCurrentPlayerIndex(board.getCurrentPlayer());
        // Die location indicates who's move it is next, but hide the value for now
        boardView.currentDieValue.set(0);

        currentStep = TurnStep.NEXT;
    }

    /**
     * Shows the current Die role.
     */
    private void roll() {
        boardView.currentDieValue.set(board.getCurrentDieValue());
        currentStep = TurnStep.ROLL;
    }

    /**
     * Shows the available options for the current player.
     */
    private void options() {
        // Reset any previous choice.
        choice = null;

        allowedMoves = board.getAllowedMoves();

        if ((allowedMoves == null) || (allowedMoves.isEmpty())) {
            // No valid moves to make by the player, continue to next player.
            next();
            return;
        }

        // Iterate through the allowedMoves and set the isChoiceProperty to true for the corresponding position in BoardView
        allowedMoves.forEach(move -> {
            PositionView toPosition = boardView.getPositionView(move.to(), board.getCurrentPlayer(), false);
            toPosition.isChoice(true);
        });

        // Make a copy of allowedMoves in a final variable to pass to the Task
        final List<Move> finalAllowedMoves = List.copyOf(allowedMoves);
        chooseTask = new Task<>() {
            @Override
            protected Move call() throws Exception {
                return MainApplication.players.get(board.getCurrentPlayer()).choose(finalAllowedMoves, board.getBoardState());
            }
        };

        // Set the choice to the value returned by the Task
        chooseTask.setOnSucceeded(event -> {
            choice = chooseTask.getValue();
            // Step only if the strategy is the ManualStrategy
            if (strategySelections.get(board.getCurrentPlayer()).equals(MANUAL_STRATEGY)) {
                step();
            }
        });

        Thread thread = new Thread(chooseTask);
        thread.setDaemon(true);
        thread.start();

        // If strategy is manual, and allowedMoves.length > 1 then pause
        if (strategySelections.get(board.getCurrentPlayer()).equals(MANUAL_STRATEGY)) {
            if (allowedMoves.size() == 1 && !autoSelectSingleChoice.get()) {
                pause();
            } else if (allowedMoves.size() > 1 ) {
                pause();
            }
        }

        currentStep = TurnStep.OPTIONS;
    }

    /**
     * Shows the selected move. Hides the non-selected options.
     */
    private void selection() {

        // There was a valid choice, but the strategy didn't pick it.
        if (!allowedMoves.contains(choice)) {
            // invalid choice, choose random for player
            logger.error("Invalid choice: {}", choice);
            choice = RandomStrategy.choose(allowedMoves);
            logger.error("Invalid choice, instead choosing random: {}", choice);
        }

        // loop over allowedMoves with an iterator to be able to remove each move from the list after setting boardView.isChoiceProperty to false
        for (Iterator<Move> iterator = allowedMoves.iterator(); iterator.hasNext(); ) {
            Move move = iterator.next();
            PositionView toPosition = boardView.getPositionView(move.to(), board.getCurrentPlayer(), false);
            toPosition.isChoice(false);
            if (move.equals(choice)) {
                toPosition.setSelected(true);
            }
            iterator.remove();
        }
        currentStep = TurnStep.SELECTION;
    }

    /**
     * Moves the player to the selected position.
     */
    private void move() {

        if (choice != null) {
            Move strike = board.getStrikeMove(choice);
            if (strike != null) {
                int struckPlayer = board.getBoardState().getPlayer(choice.to());
                strikes.increment(board.getCurrentPlayer(), struckPlayer);
                logger.trace("Player {} strikes {} with {} forcing {}", board.getCurrentPlayer(), struckPlayer, choice, strike);
                // Note that we're moving the struck player from the choice.to() position to their respective 'begin' position
                applyMove(strike, struckPlayer);
            }
            applyMove(choice, board.getCurrentPlayer());
            // Reset the choice
            choice = null;
            boardView.setSelectedPosition(null, true);
        }

        currentStep = TurnStep.MOVE;
    }

    private void applyMove(Move move, int playerIndex) {
        int finishedPlayer = board.move(move);
        if (finishedPlayer != -1) {
            logger.debug("Finished: {}", finishedPlayer);
            finished.add(MainApplication.players.get(finishedPlayer).getName());
            for (int i = 0; i < 4; i++) {
                boardView.homePositions.get(finishedPlayer).get(i).setFinishOrder(finished.size());
            }
        }
        history.add(move);

        PositionView fromPosition = boardView.getPositionView(move.from(), playerIndex, true);
        fromPosition.setOccupiedBy(-1);

        PositionView toPosition = boardView.getPositionView(move.to(), playerIndex, false);
        toPosition.setOccupiedBy(playerIndex);
        toPosition.setSelected(false);
    }

    /**
     * The game is over
     */
    private void finish() {
        boardView.setCurrentPlayerIndex(-1);
        pause();
        boardView.isFinished(true);
        currentStep = TurnStep.FINISHED;
    }


    void reset() {
        initialize(null);
    }

    void play() {
        if (!boardView.isFinished()) {
            boardView.isPaused(false);
            timeline.play();
        }
    }

    void pause() {
        boardView.isPaused(true);
        if (timeline != null) {
            timeline.stop();
        }
    }

    void playOrPause() {
        if (boardView.isPaused() && !boardView.isFinished()) {
            play();
        } else {
            pause();
        }
    }
}


