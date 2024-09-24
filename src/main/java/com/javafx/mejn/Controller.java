package com.javafx.mejn;

import com.javafx.mejn.strategy.ManualStrategy;
import com.rttnghs.mejn.Board;
import com.rttnghs.mejn.History;
import com.rttnghs.mejn.Move;
import com.rttnghs.mejn.Player;
import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.BaseHistory;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import com.rttnghs.mejn.strategy.Strategy;
import com.rttnghs.mejn.strategy.StrategyFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
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
    List<Player> players = new ArrayList<>(4);


    private enum TurnStep {
        NEXT, ROLL, OPTIONS, SELECTION, MOVE, FINISHED
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


            history = new BaseHistory<>(512);
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

            isInitialized = true;
        }
    }

    public void resetBoardView() {
        board = new Board(4);

        // Reset all home and event positions in BoardView
        boardView.homePositions.forEach(player -> {
            player.forEach(position -> {
                position.isChoiceProperty().set(false);
                position.occupiedProperty().set(-1);
            });
        });
        boardView.eventPositions.forEach(position -> {
            position.isChoiceProperty().set(false);
            position.occupiedProperty().set(-1);
        });

        // Reset begin positions in BoardView
        for (int player = 0; player < boardView.beginPositions.size(); player++) {
            int finalPlayer = player;
            boardView.beginPositions.get(player).forEach(position -> {
                position.occupiedProperty().set(finalPlayer);
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
                selection();
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
     * Sets the current step to ROLL
     */
    private void roll() {
        boardView.currentDieValue.set(board.getCurrentDieValue());
        currentStep = TurnStep.ROLL;
    }

    private void options() {

        // Use a background thread (task?) to get player.choose(allowedMoves, board.getBoardState()) because this will hand for manual strategies until the user makes a choice
        // This will allow the UI to continue to be responsive


        // Get allowed moves
        // Update the boardView with the allowed moves

        // If there are no options, then skip to the next step

        currentStep = TurnStep.OPTIONS;
    }

    private void selection() {
        // Get the selected move
        // Remove the selected move from the options list
        // Reset the options list in boarView
        currentStep = TurnStep.SELECTION;
    }

    private void move() {
        // Apply the selected move, if any

        // If the player has finished, then add the player to the finished list
        //

        currentStep = TurnStep.MOVE;
    }

    private void finish() {
        boardView.currentPlayerIndex.set(-1);
        boardView.isPlaying.set(false);
        currentStep = TurnStep.FINISHED;
    }
}


