package com.javafx.mejn;

import com.rttnghs.mejn.Board;
import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import com.rttnghs.mejn.strategy.StrategyFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.javafx.mejn.MainApp.boardView;

public class Controller {
    private static final Logger logger = LogManager.getLogger(Controller.class);

    private final StrategyFactory strategyFactory;

    private boolean isInitialized = false;

    private Board board;

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
            List<String> strategies = new ArrayList<>();
            strategies.add("ManualStrategy");
            strategies.addAll(strategyFactory.listStrategies());
            MainApp.strategyOptions.clear();
            MainApp.strategyOptions.setAll(strategies);

            String strategySelectionAttribute = Config.configuration.getString("gui[@strategies]");
            MainApp.strategySelections.clear();
            MainApp.strategySelections.addAll(Arrays.asList(strategySelectionAttribute.split(",", -1)));

            resetBoard();

            isInitialized = true;
        }
    }

    public void resetBoard() {
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
        boardView.currentDieValue.set(board.getCurrentDieValue());
    }
}


