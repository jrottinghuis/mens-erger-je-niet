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
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import static com.javafx.mejn.MainApp.boardView;

public class Controller {
    private static final Logger logger = LogManager.getLogger(Controller.class);

    private final StrategyFactory strategyFactory;
    private boolean isInitialized = false;
    private Board board;
    private TurnStep currentStep = TurnStep.NEXT;
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
    }

    /**
     * Initialize the controller.
     */
    public synchronized void initialize() {
        logger.debug("isInitialized: " + isInitialized);
        if (!isInitialized) {
            MainApp.strategyOptions.clear();
            MainApp.strategyOptions.setAll(strategyFactory.listStrategies());

            String strategySelectionAttribute = Config.configuration.getString("gui[@strategies]");
            MainApp.strategySelections.clear();
            List<String> strategyNames = new ArrayList<>(Arrays.asList(strategySelectionAttribute.split(",", -1)));
            MainApp.strategySelections.addAll(strategyNames);

            resetBoardView();
            next();

            // Reset the selected position in BoardView so that previously set listeners are dropped.
            MainApp.boardView.resetSelectedPosition();

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
                    manualStrategy.setChoiceHandler(positionCompletableFuture -> boardView.setChoice(positionCompletableFuture, finalPlayerIndex));
                }

                players.add(playerIndex, new Player(strategy, playerIndex, 40));
            }

            if (chooseTask != null) {
                chooseTask.cancel();
            }
            // Reset any previous choice.
            choice = null;

            isInitialized = true;
        }
    }

    public void resetBoardView() {
        board = new Board(4);

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

        // Set isPlaying to false
        boardView.isPlaying.set(false);

        boardView.currentPlayerIndex.set(board.getCurrentPlayer());
        boardView.currentDieValue.set(0);
    }


    // Create a method step that will be called from the UI and that has a switch statement to handle the currentStep
    public void step() {
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
        }
    }

    /**
     * Sets the current step to NEXT
     */
    private void next() {
        int nextPlayer = board.nextPlayer();

        if (nextPlayer < 0) {
            finish();
            return;
        }

        boardView.currentPlayerIndex.set(board.getCurrentPlayer());
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
            boardView.homePositions.get(board.getCurrentPlayer()).get(move.to().spot()).isChoiceProperty().set(true);
            boardView.eventPositions.get(move.to().spot()).isChoiceProperty().set(true);
        });


        // Make a copy of allowedMoves in a final variable to pass to the Task
        final List<Move> finalAllowedMoves = List.copyOf(allowedMoves);
        chooseTask = new Task<>() {
            @Override
            protected Move call() throws Exception {
                return players.get(board.getCurrentPlayer()).choose(finalAllowedMoves, board.getBoardState());
            }
        };

        chooseTask.setOnSucceeded(event -> {
            choice = chooseTask.getValue();
            // Update the UI with the choice
        });

        new Thread(chooseTask).start();
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
            if (move.equals(choice)) {
                boardView.homePositions.get(board.getCurrentPlayer()).get(move.to().spot()).isChoiceProperty().set(false);
                boardView.eventPositions.get(move.to().spot()).isChoiceProperty().set(false);
            }

            switch (move.to().layer()) {
                case EVENT -> {
                    boardView.eventPositions.get(move.to().spot()).isChoiceProperty().set(false);
                    if (move.equals(choice)) {
                        boardView.eventPositions.get(move.to().spot()).isSelectedProperty().set(true);
                    }
                }
                case HOME -> {
                    boardView.homePositions.get(board.getCurrentPlayer()).get(move.to().spot()).isChoiceProperty().set(false);
                    if (move.equals(choice)) {
                        boardView.homePositions.get(board.getCurrentPlayer()).get(move.to().spot()).isSelectedProperty().set(true);
                    }
                }
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
                logger.debug("Player {} strikes {} with {} forcing {}", board.getCurrentPlayer(), struckPlayer, choice, strike);
                applyMove(strike);
            }
            applyMove(choice);
        }

        currentStep = TurnStep.MOVE;
    }

    private void applyMove(Move move) {
        int finishedPlayer = board.move(move);
        if (finishedPlayer != -1) {
            logger.debug("Finished: {}", finishedPlayer);
            finished.add(players.get(finishedPlayer).getName());
        }
        history.add(move);

        switch (move.from().layer()) {
            case EVENT -> {
                boardView.eventPositions.get(move.from().spot()).occupiedProperty().set(-1);
            }
            case HOME -> {
                // TODO: validate the occupied property is set correctly. Do we need to map the spot to the proper item in the list?
                // Or do we enumerate through the home spots until the position is found??
                boardView.homePositions.get(board.getCurrentPlayer()).get(move.from().spot()).occupiedProperty().set(-1);
            }
            case BEGIN -> {
                // Count how many pawns are in the begin position, and set the appropriate one
            }
        }

        switch (move.to().layer()) {
            case EVENT -> {
                boardView.eventPositions.get(move.to().spot()).isChoiceProperty().set(false);
                boardView.eventPositions.get(move.to().spot()).occupiedProperty().set(board.getCurrentPlayer());
            }
            case HOME -> {
                // TODO: validate the occupied property is set correctly. Do we need to map the spot to the proper item in the list?
                // Or do we enumerate through the home spots until the position is found??
                boardView.homePositions.get(board.getCurrentPlayer()).get(move.to().spot()).isChoiceProperty().set(false);
                boardView.homePositions.get(board.getCurrentPlayer()).get(move.to().spot()).occupiedProperty().set(board.getCurrentPlayer());
            }
            case BEGIN -> {
                // Count how many pawns are in the begin position, and set the appropriate one
            }
        }
    }

    private void finish() {
        boardView.currentPlayerIndex.set(-1);
        boardView.isPlaying.set(false);
        currentStep = TurnStep.FINISHED;
    }
}


