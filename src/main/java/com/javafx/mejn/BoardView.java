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

import com.rttnghs.mejn.Layer;
import com.rttnghs.mejn.Position;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.javafx.mejn.MainApp.BORDER_OFFSET;
import static com.javafx.mejn.MainApp.debugItem;

/**
 * The BoardView class is responsible for creating the board view. It creates the board, the positions, the dice, and the play control buttons.
 */
public class BoardView {
    private static final Logger logger = LogManager.getLogger(BoardView.class);
    static final int BOARD_SIZE = 40;
    static final List<PositionView> eventPositionViews = new ArrayList<>(BOARD_SIZE);

    static final List<List<PositionView>> homePositions = new ArrayList<>(4);
    static final List<List<PositionView>> beginPositions = new ArrayList<>(4);
    static final List<StringProperty> playerStrategies = new ArrayList<>(4);
    static {
        for (int i = 0; i < 4; i++) {
            List<PositionView> positions = new ArrayList<>(4);
            homePositions.add(positions);

            positions = new ArrayList<>(4);
            beginPositions.add(positions);
            playerStrategies.add(new SimpleStringProperty("Strategy-" + i));
        }
    }

    private final DoubleProperty cellWidth = new SimpleDoubleProperty(44.0);
    private final DoubleProperty strokeWidth = new SimpleDoubleProperty();
    static final IntegerProperty currentPlayerIndex = new SimpleIntegerProperty(-1);
    static final IntegerProperty currentDieValue = new SimpleIntegerProperty(6);

    static final BooleanProperty isPlaying = new SimpleBooleanProperty(false);
    private final Controller controller;

    /**
     * The constructor for the BoardView class. It creates
     * the board, the positions, the dice, and the play control buttons.
     *
     * @param borderPane the BorderPane to which the board is added
     * @param controller
     */
    public BoardView(BorderPane borderPane, Controller controller) {
        this.controller = controller;
        Pane boardPane = new Pane();

        // Add a listener to boardPane to resize the height when the width changes
        boardPane.widthProperty().addListener((_, _, newValue) -> boardPane.setPrefHeight(newValue.doubleValue()));

        ButtonBar buttonBar = new ButtonBar();
        addButtons(buttonBar);

        borderPane.setCenter(boardPane);
        borderPane.setBottom(buttonBar);
        borderPane.setPadding(new Insets(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

        // A cell on a real board is 2.5mm. All other sizes are derived from this.
        cellWidth.bind(boardPane.widthProperty().divide(12));
        strokeWidth.bind(cellWidth.divide(18)); // 0.1/2.5=0.04 = 1/25

        addBorders(boardPane);
        addLines(boardPane);
        addPositions(boardPane);

        addLettersB(boardPane);
        addText(boardPane);

        addDice(boardPane);

        addDebugAction();
    }


    /**
     * Add the play control buttons to the buttonBar.
     *
     * @param buttonBar the ButtonBar to which the buttons are added
     */
    private void addButtons(ButtonBar buttonBar) {
        List<Button> buttons = new ArrayList<>();

        Button stepButton = new Button("Step");
        ButtonBar.setButtonData(stepButton, ButtonBar.ButtonData.NEXT_FORWARD);

        Button nextPlayer = new Button("Next Player");
        ButtonBar.setButtonData(nextPlayer, ButtonBar.ButtonData.NEXT_FORWARD);

        Button playButton = new Button("Play");
        ButtonBar.setButtonData(playButton, ButtonBar.ButtonData.NEXT_FORWARD);

        Button pauseButton = new Button("Pause");
        pauseButton.setCancelButton(true);
        pauseButton.setDisable(true);

        // TODO: Adjust for then the game is waiting for a user input
        isPlaying.addListener((_, _, newValue) -> {
            playButton.setDisable(newValue);
            stepButton.setDisable(newValue);
            nextPlayer.setDisable(newValue);
            pauseButton.setDisable(!newValue);
        });
        ButtonBar.setButtonData(pauseButton, ButtonBar.ButtonData.CANCEL_CLOSE);


        //ButtonBar.setButtonUniformSize(stepButton, false);
        ButtonBar.setButtonUniformSize(nextPlayer, false);
        //ButtonBar.setButtonUniformSize(playButton, false);
        //ButtonBar.setButtonUniformSize(pauseBUtton, false);

        buttons.add(stepButton);
        buttons.add(nextPlayer);
        buttons.add(playButton);
        buttons.add(pauseButton);

        buttonBar.getButtons().addAll(buttons);
    }


    /**
     * Add an outside and a black inside border to the boardPane.
     *
     * @param boardPane the Pane to which the borders are added
     */
    private void addBorders(Pane boardPane) {
        Rectangle redBorder = new Rectangle();
        redBorder.strokeWidthProperty().bind(strokeWidth.multiply(2));
        redBorder.setStroke(Color.CRIMSON);
        redBorder.setStrokeType(StrokeType.INSIDE);
        redBorder.setFill(Color.KHAKI);
        redBorder.widthProperty().bind(cellWidth.multiply(12));
        redBorder.heightProperty().bind(cellWidth.multiply(12));
        boardPane.getChildren().add(redBorder);

        // Add another rectangle with black stroke and transparent color half the cellWidth inside the boardPane
        Rectangle blackBorder = new Rectangle();
        blackBorder.strokeWidthProperty().bind(strokeWidth);
        blackBorder.setStroke(Color.BLACK);
        blackBorder.setFill(Color.TRANSPARENT);
        blackBorder.widthProperty().bind(cellWidth.multiply(11.5));
        blackBorder.heightProperty().bind(cellWidth.multiply(11.5));
        blackBorder.xProperty().bind(cellWidth.multiply(0.25));
        blackBorder.yProperty().bind(cellWidth.multiply(0.25));
        boardPane.getChildren().add(blackBorder);
    }

    /**
     * Add the black lines going around the board to the boardPane to create path of the pawn around the board.
     *
     * @param boardPane the Pane to which the lines are added
     */
    private void addLines(Pane boardPane) {
        addLine(boardPane, strokeWidth, cellWidth, 5, 5, 7, 11);
        addLine(boardPane, strokeWidth, cellWidth, 1, 5, 7, 7);
        addLine(boardPane, strokeWidth, cellWidth, 1, 1, 5, 7);
        addLine(boardPane, strokeWidth, cellWidth, 1, 5, 5, 5);
        addLine(boardPane, strokeWidth, cellWidth, 5, 5, 1, 5);
        addLine(boardPane, strokeWidth, cellWidth, 5, 7, 1, 1);
        addLine(boardPane, strokeWidth, cellWidth, 5, 7, 1, 1);
        addLine(boardPane, strokeWidth, cellWidth, 7, 7, 1, 5);
        addLine(boardPane, strokeWidth, cellWidth, 7, 11, 5, 5);
        addLine(boardPane, strokeWidth, cellWidth, 11, 11, 5, 7);
        addLine(boardPane, strokeWidth, cellWidth, 7, 11, 7, 7);
        addLine(boardPane, strokeWidth, cellWidth, 7, 7, 7, 11);
        addLine(boardPane, strokeWidth, cellWidth, 5, 7, 11, 11);
    }

    /**
     * Add a line to the boardPane with the given start and end coordinates.
     *
     * @param boardPane   the Pane to which the line is added
     * @param strokeWidth the width of the stroke
     * @param cellWidth   the width of the cell
     * @param startX      the x coordinate of the start of the line in terms of cellWidth
     * @param endX        the x coordinate of the end of the line in terms of cellWidth
     * @param startY      the y coordinate of the start of the line in terms of cellWidth
     * @param endY        the y coordinate of the end of the line in terms of cellWidth
     */
    private static void addLine(Pane boardPane, DoubleProperty strokeWidth, DoubleProperty cellWidth, int startX, int endX, int startY, int endY) {
        Line line = new Line();
        line.strokeWidthProperty().bind(strokeWidth);
        line.setStroke(Color.BLACK);
        line.startXProperty().bind(cellWidth.multiply(startX));
        line.endXProperty().bind(cellWidth.multiply(endX));
        line.startYProperty().bind(cellWidth.multiply(startY));
        line.endYProperty().bind(cellWidth.multiply(endY));
        boardPane.getChildren().add(line);
    }

    /**
     * Draw the circle for each of the board positions in the Event, Begin, and Home layers. Add each to the respective static list for later manipulation of the properties. The positions are added in a clockwise fashion starting from the bottom left corner of the board and numbered according to the README.md file.
     *
     * @param boardPane the Pane to which the positions are added
     */
    private void addPositions(Pane boardPane) {
        int spot = 0;
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 5, 11 - j, boardPane));
            spot++;
        }
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 5 - j, 7, boardPane));
            spot++;
        }
        for (int j = 0; j < 2; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 1, 7 - j, boardPane));
            spot++;
        }
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 1 + j, 5, boardPane));
            spot++;
        }
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 5, 5 - j, boardPane));
            spot++;
        }
        for (int j = 0; j < 2; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 5 + j, 1, boardPane));
            spot++;
        }
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 7, 1 + j, boardPane));
            spot++;
        }
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 7 + j, 5, boardPane));
            spot++;
        }
        for (int j = 0; j < 2; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 11, 5 + j, boardPane));
            spot++;
        }
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 11 - j, 7, boardPane));
            spot++;
        }
        for (int j = 0; j < 4; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 7, 7 + j, boardPane));
            spot++;
        }
        for (int j = 0; j < 2; j++) {
            eventPositionViews.add(spot, getPositionView(spot, Layer.EVENT, 7 - j, 11, boardPane));
            spot++;
        }

        // Home positions
        for (int j = 0; j < 4; j++) {
            homePositions.get(0).add(j, getPositionView(j, Layer.HOME, 6, 10 - j, boardPane));
        }
        for (int j = 0; j < 4; j++) {
            homePositions.get(1).add(j, getPositionView(10 + j, Layer.HOME, 2 + j, 6, boardPane));
        }
        for (int j = 0; j < 4; j++) {
            homePositions.get(2).add(j, getPositionView(20 + j, Layer.HOME, 6, 2 + j, boardPane));
        }
        for (int j = 0; j < 4; j++) {
            homePositions.get(3).add(j, getPositionView(30 + j, Layer.HOME, 10 - j, 6, boardPane));
        }

        beginPositions.get(0).add(0, getPositionView(34, Layer.BEGIN, 1, 11, boardPane));
        beginPositions.get(0).add(1, getPositionView(34, Layer.BEGIN, 1, 10, boardPane));
        beginPositions.get(0).add(2, getPositionView(34, Layer.BEGIN, 2, 10, boardPane));
        beginPositions.get(0).add(3, getPositionView(34, Layer.BEGIN, 2, 11, boardPane));
        beginPositions.get(1).add(0, getPositionView(4, Layer.BEGIN, 1, 2, boardPane));
        beginPositions.get(1).add(1, getPositionView(4, Layer.BEGIN, 1, 1, boardPane));
        beginPositions.get(1).add(2, getPositionView(4, Layer.BEGIN, 2, 1, boardPane));
        beginPositions.get(1).add(3, getPositionView(4, Layer.BEGIN, 2, 2, boardPane));
        beginPositions.get(2).add(0, getPositionView(14, Layer.BEGIN, 10, 2, boardPane));
        beginPositions.get(2).add(1, getPositionView(14, Layer.BEGIN, 10, 1, boardPane));
        beginPositions.get(2).add(2, getPositionView(14, Layer.BEGIN, 11, 1, boardPane));
        beginPositions.get(2).add(3, getPositionView(14, Layer.BEGIN, 11, 2, boardPane));
        beginPositions.get(3).add(0, getPositionView(24, Layer.BEGIN, 10, 11, boardPane));
        beginPositions.get(3).add(1, getPositionView(24, Layer.BEGIN, 10, 10, boardPane));
        beginPositions.get(3).add(2, getPositionView(24, Layer.BEGIN, 11, 10, boardPane));
        beginPositions.get(3).add(3, getPositionView(24, Layer.BEGIN, 11, 11, boardPane));
    }

    /**
     * Creates a PositionView for a given spot and layer.
     *
     * @param spot      the spot number
     * @param layer     the layer of the position
     * @param x         the x coordinate of the position in terms of cellWidth
     * @param y         the y coordinate of the position in terms of cellWidth
     * @param boardPane the Pane to which the position view is added
     * @return the created PositionView
     */
    private PositionView getPositionView(int spot, Layer layer, int x, int y, Pane boardPane) {
        Position position = new Position(layer, spot);
        return new PositionView(position, x, y, boardPane, cellWidth, strokeWidth);
    }

    /**
     * Add the letter B to the boardPane at the corners of the board.
     *
     * @param boardPane the Pane to which the letters are added
     */
    private void addLettersB(Pane boardPane) {
        StackPane stackPane = getLetterBPane(0, 1.5, 10.5, 0);
        boardPane.getChildren().add(stackPane);

        stackPane = getLetterBPane(1, 1.5, 1.5, 180);
        boardPane.getChildren().add(stackPane);

        stackPane = getLetterBPane(2, 10.5, 1.5, 180);
        boardPane.getChildren().add(stackPane);

        stackPane = getLetterBPane(3, 10.5, 10.5, 0);
        boardPane.getChildren().add(stackPane);
    }

    /**
     * Create a StackPane with the letter B at the given x and y coordinates and rotation.
     *
     * @param playerIndex the index of the player
     * @param x           the x coordinate in terms of cellWidth
     * @param y           the y coordinate in terms of cellWidth
     * @param rotation    the rotation of the letter
     * @return the created StackPane
     */
    private StackPane getLetterBPane(int playerIndex, double x, double y, double rotation) {
        Text letterB = new Text("B");
        letterB.setFill(Color.BLACK);
        letterB.setFont(Font.font("System", FontWeight.BOLD, 10)); // Set the font to bold
        letterB.scaleXProperty().bind(cellWidth.multiply(0.035));
        letterB.scaleYProperty().bind(cellWidth.multiply(0.035));
        StackPane stackPane = new StackPane(letterB);
        stackPane.layoutXProperty().bind(cellWidth.multiply(x));
        stackPane.layoutYProperty().bind(cellWidth.multiply(y));
        stackPane.setRotate(rotation);

        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(playerStrategies.get(playerIndex));
        tooltip.setShowDelay(Duration.seconds(0.5));
        tooltip.setShowDuration(Duration.seconds(10));
        Tooltip.install(stackPane, tooltip);
        return stackPane;
    }

    /**
     * Add the text "Mens erger je niet" to the boardPane.
     *
     * @param boardPane the Pane to which the text is added
     */
    private void addText(Pane boardPane) {
        addWord(boardPane, "Mens", 1.5, 4);
        addWord(boardPane, "erger", 9, 4);
        addWord(boardPane, "  je", 1.5, 9);
        addWord(boardPane, "niet!", 9, 9);
    }

    /**
     * Add a word to the boardPane at the given x and y coordinates.
     *
     * @param boardPane the Pane to which the word is added
     * @param word      the word to add
     * @param x         the x coordinate in terms of cellWidth
     * @param y         the y coordinate in terms of cellWidth
     */
    private void addWord(Pane boardPane, String word, double x, double y) {
        Text text = new Text(word);
        text.xProperty().bind(cellWidth.multiply(x));
        text.yProperty().bind(cellWidth.multiply(y));
        text.setFont(Font.font("Brush Script MT", 60));

        // Add an event handler to cellWidth to resize the font size when the cellWidth changes
        cellWidth.addListener((_, _, newValue) -> text.setFont(Font.font("Brush Script MT", newValue.doubleValue())));

        boardPane.getChildren().add(text);
    }

    /**
     * Add the dice to the boardPane. All dice combinations will be created,
     * but only the one for the current player, and the current die value will be visible.
     *
     * @param boardPane the Pane to which the dice are added
     */
    private void addDice(Pane boardPane) {
        addDice(boardPane, 0, 3, 11);
        addDice(boardPane, 1, 1, 3);
        addDice(boardPane, 2, 9, 1);
        addDice(boardPane, 3, 11, 9);

    }

    /**
     * Add the dice for the player with the given index at the given x and y coordinates.
     *
     * @param boardPane   the Pane to which the dice are added
     * @param playerIndex the index of the player
     * @param x           the x coordinate in terms of cellWidth
     * @param y           the y coordinate in terms of cellWidth
     */
    private void addDice(Pane boardPane, int playerIndex, double x, double y) {
        for (int i = 1; i < 7; i++) {
            StackPane diePane = createDie(playerIndex, i, x, y);
            boardPane.getChildren().add(diePane);
        }
    }

    /**
     * Create a die with the given playerIndex and dieValue at the given x and y coordinates.
     *
     * @param playerIndex the index of the player
     * @param dieValue    the value of the die
     * @param x           the x coordinate in terms of cellWidth
     * @param y           the y coordinate in terms of cellWidth
     * @return the created StackPane
     */
    private StackPane createDie(int playerIndex, int dieValue, double x, double y) {
        StackPane diePane = new StackPane();
        Rectangle die = new Rectangle();
        die.setFill(Color.FIREBRICK);
        die.widthProperty().bind(cellWidth.multiply(0.6));
        die.heightProperty().bind(cellWidth.multiply(0.6));
        die.setArcWidth(10);
        die.setArcHeight(10);
        diePane.getChildren().add(die);
        StackPane.setAlignment(die, javafx.geometry.Pos.CENTER);
        diePane.layoutXProperty().bind(cellWidth.multiply(x).subtract(cellWidth.multiply(0.3)));
        diePane.layoutYProperty().bind(cellWidth.multiply(y).subtract(cellWidth.multiply(0.3)));
        diePane.visibleProperty().bind(currentPlayerIndex.isEqualTo(playerIndex).and(currentDieValue.isEqualTo(dieValue)));

        // Position dots based on dieValue
        switch (dieValue) {
            case 1:
                addDot(diePane, 0.5, 0.5);
                break;
            case 2:
                addDot(diePane, 0.25, 0.25);
                addDot(diePane, 0.75, 0.75);
                break;
            case 3:
                addDot(diePane, 0.25, 0.25);
                addDot(diePane, 0.5, 0.5);
                addDot(diePane, 0.75, 0.75);
                break;
            case 4:
                addDot(diePane, 0.25, 0.25);
                addDot(diePane, 0.75, 0.25);
                addDot(diePane, 0.25, 0.75);
                addDot(diePane, 0.75, 0.75);
                break;
            case 5:
                addDot(diePane, 0.25, 0.25);
                addDot(diePane, 0.75, 0.25);
                addDot(diePane, 0.5, 0.5);
                addDot(diePane, 0.25, 0.75);
                addDot(diePane, 0.75, 0.75);
                break;
            case 6:
                addDot(diePane, 0.25, 0.25);
                addDot(diePane, 0.75, 0.25);
                addDot(diePane, 0.25, 0.5);
                addDot(diePane, 0.75, 0.5);
                addDot(diePane, 0.25, 0.75);
                addDot(diePane, 0.75, 0.75);
                break;
        }

        return diePane;
    }

    private void addDot(StackPane diePane, double x, double y) {
        Circle dot = new Circle();
        dot.radiusProperty().bind(cellWidth.multiply(0.05));
        dot.setFill(Color.GOLDENROD);
        diePane.getChildren().add(dot);
        dot.translateXProperty().bind(cellWidth.multiply(0.6).multiply(x - 0.5));
        dot.translateYProperty().bind(cellWidth.multiply(0.6).multiply(y - 0.5));
    }

    private void addDebugAction() {
        // TODO: Remove after debugging
        debugItem.setOnAction(e -> {
            // log scene height and width
            logger.error("Cell Width: {}", cellWidth.get());
            logger.error("");
            if (eventPositionViews.get(3).isChoiceProperty().get()) {
                eventPositionViews.get(3).isChoiceProperty().setValue(false);
            } else {
                eventPositionViews.get(3).isChoiceProperty().setValue(true);
            }
            /*
            if (eventPositionViews.get(1).occupiedBy.get() == -1) {
                eventPositionViews.get(1).occupiedBy.setValue(currentPlayerIndex.get());
            } else {
                eventPositionViews.get(1).occupiedBy.setValue(-1);
            }

             */
            // Increased the current die value by 1 but not to increase it beyond 6 then start at 1 again
            currentDieValue.set((currentDieValue.get() + 1));
            if (currentDieValue.get() > 6) {
                currentDieValue.set(1);
            }
            logger.error("Current Die Value: {}", currentDieValue.get());
            currentPlayerIndex.set((currentPlayerIndex.get() + 3) % 4);
            // Toggle a single pawn on
            eventPositionViews.get(1).occupiedProperty().setValue(currentPlayerIndex.get());

            logger.error("Current Player Index: {}", currentPlayerIndex.get());

            for (int i = 0; i < 4; i++) {
                beginPositions.get(i).getFirst().occupiedProperty().setValue(i);
            }
        });
    }


}
