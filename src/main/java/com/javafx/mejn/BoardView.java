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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.javafx.mejn.MainApp.BORDER_OFFSET;
import static com.javafx.mejn.MainApp.debugItem;


public class BoardView {
    private static final Logger logger = LogManager.getLogger(BoardView.class);
    static final int BOARD_SIZE = 40;
    static final List<PositionView> eventPositionViews = new ArrayList<>(BOARD_SIZE);

    static final List<List<PositionView>> homePositions = new ArrayList<>(4);
    static final List<List<PositionView>> beginPositions = new ArrayList<>(4);
    static {
        for (int i = 0; i < 4; i++) {
            List<PositionView> positions = new ArrayList<>(4);
            homePositions.add(positions);

            positions = new ArrayList<>(4);
            beginPositions.add(positions);
        }
    }

    private final DoubleProperty cellWidth = new SimpleDoubleProperty(44.0);
    private final DoubleProperty strokeWidth = new SimpleDoubleProperty();
    static final IntegerProperty currentPlayerIndex = new SimpleIntegerProperty(0);
    static final IntegerProperty currentDieValue = new SimpleIntegerProperty(5);

    public BoardView(BorderPane borderPane) {
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

        addBorders(boardPane, cellWidth, strokeWidth);
        addLines(boardPane);

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


         StackPane stackPane = getLetterBStackPane(1.5, 10.5, 0);
        boardPane.getChildren().add(stackPane);
        stackPane = getLetterBStackPane(1.5, 1.5, 180);
        boardPane.getChildren().add(stackPane);
        stackPane = getLetterBStackPane(10.5, 1.5, 180);
        boardPane.getChildren().add(stackPane);
        stackPane = getLetterBStackPane(10.5, 10.5, 0);
        boardPane.getChildren().add(stackPane);

        StackPane die = createDie(currentPlayerIndex, 0, 3, 11);
        boardPane.getChildren().add(die);
        die = createDie(currentPlayerIndex, 1, 1, 3);
        boardPane.getChildren().add(die);
        die = createDie(currentPlayerIndex, 2, 9, 1);
        boardPane.getChildren().add(die);
        die = createDie(currentPlayerIndex, 3, 11, 9);
        boardPane.getChildren().add(die);

        // TODO: Remove after debugging
        debugItem.setOnAction(e -> {
            // log scene height and width
            logger.error("Cell Width: {}", cellWidth.get());
            logger.error("BoardPane Width, Height: {}, {}", boardPane.getWidth(), boardPane.getHeight());
            logger.error("");
            if (eventPositionViews.get(3).isChoice.get()) {
                eventPositionViews.get(3).isChoice.setValue(false);
            } else {
                eventPositionViews.get(3).isChoice.setValue(true);
            }
            if (eventPositionViews.get(1).occupiedBy.get() == -1) {
                eventPositionViews.get(1).occupiedBy.setValue(currentPlayerIndex.get());
            } else {
                eventPositionViews.get(1).occupiedBy.setValue(-1);
            }
            // Increased the current die value by 1 but not to increase it beyond 6 then start at 1 again
            currentDieValue.set((currentDieValue.get() + 1));
            if (currentDieValue.get() > 6) {
                currentDieValue.set(1);
            }
            logger.error("Current Die Value: {}", currentDieValue.get());
            currentPlayerIndex.set((currentPlayerIndex.get() + 3) % 4);
            logger.error("Current Player Index: {}", currentPlayerIndex.get());
        });
    }



    private PositionView getPositionView(int spot, Layer layer, int x, int y, Pane boardPane) {
        Position position = new Position(layer, spot);
        return new PositionView(position, x, y, boardPane, cellWidth, strokeWidth);
    }

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

    private void addButtons(ButtonBar buttonBar) {
        List<Button> buttons = new ArrayList<>();
        Button stepButton = new Button("Step");
        Button nextPlayer = new Button("Next Player");
        Button playButton = new Button("Play");
        Button pauseBUtton = new Button("Pause");
        pauseBUtton.setDisable(true);

        ButtonBar.setButtonUniformSize(stepButton, false);
        ButtonBar.setButtonUniformSize(nextPlayer, false);
        ButtonBar.setButtonUniformSize(playButton, false);
        ButtonBar.setButtonUniformSize(pauseBUtton, false);

        buttons.add(stepButton);
        buttons.add(nextPlayer);
        buttons.add(playButton);
        buttons.add(pauseBUtton);

        buttonBar.getButtons().addAll(buttons);
    }

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

    private static void addBorders(Pane boardPane, DoubleProperty cellWidth, DoubleProperty strokeWidth) {
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

    private StackPane getLetterBStackPane(double x, double y, double rotation) {
        Text letterB = new Text("B");
        letterB.setFill(Color.BLACK);
        letterB.setFont(Font.font("System", FontWeight.BOLD, 10)); // Set the font to bold
        letterB.scaleXProperty().bind(cellWidth.multiply(0.035));
        letterB.scaleYProperty().bind(cellWidth.multiply(0.035));
        StackPane stackPane = new StackPane(letterB);
        stackPane.layoutXProperty().bind(cellWidth.multiply(x));
        stackPane.layoutYProperty().bind(cellWidth.multiply(y));
        stackPane.setRotate(rotation);
        return stackPane;
    }

    // TODO: create 6 die objects and set an object property based on the current die value like so:
    // https://stackoverflow.com/questions/43844808/change-the-image-in-imageview-in-javafx
    // Do this for all 4 positions, leaving the current visibility logic in place.
    private StackPane createDie(IntegerProperty currentPlayerIndex, int index, double x, double y) {
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
        diePane.visibleProperty().bind(currentPlayerIndex.isEqualTo(index));

        // Create dots based on dieValue
        for (int i = 1; i <= 6; i++) {
            Circle dot = new Circle();
            dot.radiusProperty().bind(cellWidth.multiply(0.05));
            dot.setFill(Color.GOLD);
            dot.setVisible(false);
            diePane.getChildren().add(dot);
        }

        // Position dots based on dieValue
        switch (currentDieValue.get()) {
            case 1:
                setDotPosition(diePane, 0, 0.5, 0.5);
                break;
            case 2:
                setDotPosition(diePane, 0, 0.25, 0.25);
                setDotPosition(diePane, 1, 0.75, 0.75);
                break;
            case 3:
                setDotPosition(diePane, 0, 0.25, 0.25);
                setDotPosition(diePane, 1, 0.5, 0.5);
                setDotPosition(diePane, 2, 0.75, 0.75);
                break;
            case 4:
                setDotPosition(diePane, 0, 0.25, 0.25);
                setDotPosition(diePane, 1, 0.75, 0.25);
                setDotPosition(diePane, 2, 0.25, 0.75);
                setDotPosition(diePane, 3, 0.75, 0.75);
                break;
            case 5:
                setDotPosition(diePane, 0, 0.25, 0.25);
                setDotPosition(diePane, 1, 0.75, 0.25);
                setDotPosition(diePane, 2, 0.5, 0.5);
                setDotPosition(diePane, 3, 0.25, 0.75);
                setDotPosition(diePane, 4, 0.75, 0.75);
                break;
            case 6:
                setDotPosition(diePane, 0, 0.25, 0.25);
                setDotPosition(diePane, 1, 0.75, 0.25);
                setDotPosition(diePane, 2, 0.25, 0.5);
                setDotPosition(diePane, 3, 0.75, 0.5);
                setDotPosition(diePane, 4, 0.25, 0.75);
                setDotPosition(diePane, 5, 0.75, 0.75);
                break;
        }

        return diePane;
    }

    private void setDotPosition(StackPane diePane, int dotIndex, double x, double y) {
        Circle dot = (Circle) diePane.getChildren().get(dotIndex + 1);
        dot.translateXProperty().bind(cellWidth.multiply(0.6).multiply(x - 0.5));
        dot.translateYProperty().bind(cellWidth.multiply(0.6).multiply(y - 0.5));
        dot.setVisible(true);
    }
}
