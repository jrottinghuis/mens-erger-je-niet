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
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
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
    static final IntegerProperty PLAYER_INDEX = new SimpleIntegerProperty(0);

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

        // TODO: Add these to a separate array so that I can easily access them for the Controller class
        spot = 0;
        for (int j = 0; j < 4; j++) {
            getPositionView(spot, Layer.HOME, 6, 10 - j, boardPane);
            spot++;
        }
        spot = 10;
        for (int j = 0; j < 4; j++) {
            getPositionView(spot, Layer.HOME, 2 + j, 6, boardPane);
            spot++;
        }
        spot = 20;
        for (int j = 0; j < 4; j++) {
            getPositionView(spot, Layer.HOME, 6, 2 + j, boardPane);
            spot++;
        }
        spot = 30;
        for (int j = 0; j < 4; j++) {
            getPositionView(spot, Layer.HOME, 10 - j, 6, boardPane);
            spot++;
        }


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
            if (eventPositionViews.get(10).occupiedBy.get() == -1) {
                eventPositionViews.get(10).occupiedBy.setValue(0);
            } else {
                eventPositionViews.get(10).occupiedBy.setValue(-1);
            }
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
}
