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

import static com.javafx.mejn.MainApp.boardView;
import static com.javafx.mejn.MainApp.debugItem;

public class Controller {
    private static final Logger logger = LogManager.getLogger(Controller.class);

    private final StrategyFactory strategyFactory;
    private Board board;
    private TurnStep currentStep = null;
    private BaseHistory<Move> history = new BaseHistory<>(512);
    private List<String> finished = new ArrayList<>(4);

    /**
     * Agent=player that strikes other player off the board. Event is index of other
     * player that is struck.
     */
    private EventCounter<Integer, Integer> strikes = new EventCounter<>();

    /**
     * For the finishes EventCounter, the agent is the name of the strategy, and the
     * event is the 0-based position they finished in.
     */
    private EventCounter<String, Integer> finishCounts = new EventCounter<>();

    List<Player> players = new ArrayList<>(4);

    private List<Move> allowedMoves = new ArrayList<>();

    private Task<Move> chooseTask;
    private Move choice;
    Timeline timeline = new Timeline();
    private KeyFrame keyFrame;

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
        debugItem.setOnAction(e -> {
            logger.debug("Debugging");
            logger.debug("Current step: {}", currentStep);
            logger.debug("board.player: {}", board.getCurrentPlayer());
            logger.debug("boardview.currentPlayerIndex: {}", boardView.getCurrentPlayerIndex());
            logger.debug("Current die value: {}", board.getCurrentDieValue());
            logger.debug("Finished players: {}", finished);
            logger.debug("Strikes: {}", strikes);
            logger.debug("Finish counts: {}", finishCounts);
            logger.debug("History: {}", history);
            logger.debug("Players: {}", players);
            logger.debug("Allowed moves: {}", allowedMoves);
            logger.debug("Choice: {}", choice);
        });
    }

    /**
     * Initialize the controller.
     */
    public synchronized void initialize(Scene scene) {
        logger.trace("Controller initializing");
        boardView.addAccelerators(scene);
        if (chooseTask != null) {
            chooseTask.cancel();
        }
        pause();
        MainApp.strategyOptions.clear();
        MainApp.strategyOptions.setAll(strategyFactory.listStrategies());

        String strategySelectionAttribute = Config.configuration.getString("gui[@strategies]");
        MainApp.strategySelections.clear();
        List<String> strategyNames = new ArrayList<>(Arrays.asList(strategySelectionAttribute.split(",", -1)));
        MainApp.strategySelections.addAll(strategyNames);

        resetBoardView();

        history = new BaseHistory<>(512);
        finished = new ArrayList<>(4);
        strikes = new EventCounter<>();
        finishCounts = new EventCounter<>();

        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            // Rotate perspective counter clockwise
            int rotation = Player.rotation(playerIndex);
            Supplier<History<Move>> shiftedHistorySupplier = history.getSupplier(Move.shifter(rotation, 40));
            Strategy strategy = strategyFactory.getStrategy(strategyNames.get(playerIndex)).initialize(shiftedHistorySupplier);

            // If strategy is instanceOf ManualStrategy, then set the choice handler
            if (strategy instanceof ManualStrategy manualStrategy) {
                int finalPlayerIndex = playerIndex;
                manualStrategy.setChoiceHandler(positionCompletableFuture -> boardView.setChoiceHandler(positionCompletableFuture, finalPlayerIndex));
            }

            players.add(playerIndex, new Player(strategy, playerIndex, 40));
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

        timeline.rateProperty().bind(MainApp.playbackSpeed);
    }

    private void resetBoardView() {
        board = new Board(4);
        boardView.setCurrentPlayerIndex(-1);

        // Reset the selected position in BoardView so that previously set listeners are dropped.
        MainApp.boardView.resetSelectedPosition();

        // Reset all home and event positions in BoardView
        boardView.homePositions.forEach(player -> {
            player.forEach(position -> {
                position.isChoiceProperty().set(false);
                position.isSelectedProperty().set(false);
                position.occupiedProperty().set(-1);
            });
        });
        boardView.eventPositions.forEach(position -> {
            position.isChoiceProperty().set(false);
            position.isSelectedProperty().set(false);
            position.occupiedProperty().set(-1);
        });

        // Reset begin positions in BoardView
        for (int player = 0; player < boardView.beginPositions.size(); player++) {
            int finalPlayer = player;
            boardView.beginPositions.get(player).forEach(position -> {
                position.occupiedProperty().set(finalPlayer);
                position.isChoiceProperty().set(false);
                position.isSelectedProperty().set(false);
            });
        }

        boardView.currentDieValue.set(0);
    }

    /**
     * Wrap things up in preparation for shut-down
     */
    public void stop() {
        if (chooseTask != null) {
            chooseTask.cancel();
        }
        pause();
        boardView.setCurrentPlayerIndex(-1);
    }


    // Create a method step that will be called from the UI and that has a switch statement to handle the currentStep
    public void step() {
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
                } else if (allowedMoves.size() == 1) {
                    // If there is only one choice, just select it when step is called.
                    // Make sure we set the selected position in BoardView so that the selection listeners can unroll properly
                    boardView.setSelectedPosition(allowedMoves.get(0).to(), true);
                } else if (MainApp.strategySelections.get(board.getCurrentPlayer()).equals("ManualStrategy")) {
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
            toPosition.isChoiceProperty().set(true);
        });

        // Make a copy of allowedMoves in a final variable to pass to the Task
        final List<Move> finalAllowedMoves = List.copyOf(allowedMoves);
        chooseTask = new Task<>() {
            @Override
            protected Move call() throws Exception {
                return players.get(board.getCurrentPlayer()).choose(finalAllowedMoves, board.getBoardState());
            }
        };

        // Set the choice to the value returned by the Task
        chooseTask.setOnSucceeded(event -> {
            choice = chooseTask.getValue();
            // Step only if the strategy is the ManualStrategy
            if (MainApp.strategySelections.get(board.getCurrentPlayer()).equals("ManualStrategy")) {
                step();
            }
        });

        new Thread(chooseTask).start();

        // If strategy is manual, and allowedMoves.length > 1 then pause
        if (MainApp.strategySelections.get(board.getCurrentPlayer()).equals("ManualStrategy") && allowedMoves.size() > 1) {
            pause();
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
            toPosition.isChoiceProperty().set(false);
            if (move.equals(choice)) {
                toPosition.isSelectedProperty().set(true);
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
            finished.add(players.get(finishedPlayer).getName());
        }
        history.add(move);

        PositionView fromPosition = boardView.getPositionView(move.from(), playerIndex, true);
        fromPosition.occupiedProperty().set(-1);

        PositionView toPosition = boardView.getPositionView(move.to(), playerIndex, false);
        toPosition.occupiedProperty().set(playerIndex);
        toPosition.isSelectedProperty().set(false);
    }

    /**
     * The game is over
     */
    private void finish() {
        boardView.setCurrentPlayerIndex(-1);
        pause();
        currentStep = TurnStep.FINISHED;
    }


    public void reset() {
        initialize(null);
    }

    public void play() {
        boardView.isPaused.set(false);
        timeline.play();
    }

    public void pause() {
        boardView.isPaused.set(true);
        if (timeline != null) {
            timeline.stop();
        }
    }

    public void playOrPause() {
        if (boardView.isPaused.get()) {
            play();
        } else {
            pause();
        }
    }
}


