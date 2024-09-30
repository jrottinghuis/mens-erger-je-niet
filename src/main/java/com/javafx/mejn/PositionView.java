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
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

import static com.javafx.mejn.MainApplication.showPositionNumbers;

/**
 * Represents a view of a position on the board.
 */
class PositionView {

    private static final Logger logger = LogManager.getLogger(PositionView.class);

    private final Position position;

    // Whether this position is a choice for a move.to for the current player
    private final BooleanProperty isChoice = new SimpleBooleanProperty(false);

    // Whether this position is selected as the move.to position
    private final BooleanProperty isSelected = new SimpleBooleanProperty(false);

    // The player index occupying this property. Any other value indicates, not occupied.
    private final IntegerProperty occupiedBy = new SimpleIntegerProperty(-1);

    // The 1-based finish order of the player occupying this home position. -1 indicates not occupied.
    private final IntegerProperty finishOrder = new SimpleIntegerProperty(-1);


    /**
     * Create a new PositionView
     *
     * @param position               the position to represent
     * @param x                      the x coordinate of the position in terms of cellWidth
     * @param y                      the y coordinate of the position in terms of cellWidth
     * @param boardPane              the pane to add the position to
     * @param cellWidth              the width of the cell
     * @param strokeWidth            the width of the stroke
     * @param currentPlayerIndex     the index of the current player
     * @param selectedPositionSetter the consumer to set the selected position
     */
    PositionView(Position position, int x, int y, Pane boardPane, DoubleProperty cellWidth, DoubleProperty strokeWidth, ReadOnlyIntegerProperty currentPlayerIndex, Consumer<Position> selectedPositionSetter) {
        this.position = position;

        // A cell on a real board is 2.5mm. All other sizes are derived from this.
        DoubleProperty circleRadius = new SimpleDoubleProperty();
        circleRadius.bind(cellWidth.multiply(0.375)); // 0.75/2.5=0.6
        DoubleProperty smallCircleRadius = new SimpleDoubleProperty();
        smallCircleRadius.bind(cellWidth.multiply(0.26)); // 0.55/2.5=0.22

        Circle circle = new Circle();
        circle.strokeWidthProperty().bind(strokeWidth);
        circle.setStroke(Color.BLACK);

        // Set the right circle diameter based on the layer
        if (position.layer().equals(Layer.EVENT)) {
            circle.radiusProperty().bind(circleRadius);
        } else {
            circle.radiusProperty().bind(smallCircleRadius);
        }

        setFill(position, circle);

        // Set the appropriate stroke Dash Array and color if this position is an option for a valid move
        isChoice.addListener((_, _, newValue) -> {
            if (newValue) {
                circle.getStrokeDashArray().addAll(5d, 10d);
                circle.setStroke(PlayerView.getColor(currentPlayerIndex));
            } else {
                // Remove the stroke dash array
                circle.getStrokeDashArray().clear();
                circle.setStroke(Color.BLACK);
            }
        });

        // Set the appropriate stroke color if this position is the selected option for a move.
        isSelected.addListener((_, _, newValue) -> {
            if (newValue) {
                circle.getStrokeDashArray().clear();
                circle.setStroke(PlayerView.getColor(currentPlayerIndex));

            } else {
                // Remove the stroke dash array
                circle.getStrokeDashArray().clear();
                circle.setStroke(Color.BLACK);
            }
        });

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(circle);

        Text letter = createLetter();
        letter.scaleXProperty().bind(circle.radiusProperty().divide(8));
        letter.scaleYProperty().bind(circle.radiusProperty().divide(8));
        stackPane.getChildren().add(letter);

        Text number = new Text(String.valueOf(position.spot()));
        number.scaleXProperty().bind(circle.radiusProperty().divide(30));
        number.scaleYProperty().bind(circle.radiusProperty().divide(30));
        number.visibleProperty().bind(showPositionNumbers);
        stackPane.getChildren().add(number);

        Circle pawn = new Circle();
        pawn.strokeWidthProperty().bind(strokeWidth.multiply(0.3));
        pawn.setStroke(Color.BLACK);
        pawn.radiusProperty().bind(smallCircleRadius);
        // Default to not visible
        pawn.setVisible(false);

        occupiedBy.addListener((_, _, newValue) -> {
            if (newValue.intValue() < 0 || newValue.intValue() > 3) {
                pawn.setVisible(false);
            } else {
                pawn.setVisible(true);
                pawn.setFill(PlayerView.getGradient(newValue.intValue()));
            }
        });

        Text finishPositionText = new Text();
        finishPositionText.textProperty().bind(finishOrder.asString());
        finishPositionText.visibleProperty().bind(finishOrder.greaterThan(-1));
        finishPositionText.setBoundsType(TextBoundsType.VISUAL);
        stackPane.getChildren().addAll(pawn, finishPositionText);

        // Position the stackPane in the right place
        stackPane.layoutXProperty().bind(cellWidth.multiply(x).subtract(circle.radiusProperty()));
        stackPane.layoutYProperty().bind(cellWidth.multiply(y).subtract(circle.radiusProperty()));
        stackPane.prefWidthProperty().bind(circle.radiusProperty());
        rotateStackPane(stackPane);

        stackPane.setOnMouseClicked(event -> {
            if (isChoice.get()) {
                selectedPositionSetter.accept(position);
            } else {
                logger.debug("Clicked on position while not a choice: {}", position);
            }
        });

        boardPane.getChildren().add(stackPane);
    }

    /**
     * @return whether this position is a choice for a move for the current player
     */
    Boolean isChoice() {
        return isChoice.get();
    }

    /**
     * @param choice indicates whether this position is a choice for a move for the current player
     */
    void isChoice(boolean choice) {
        isChoice.set(choice);
    }

    /**
     * @return whether this position is selected
     */
    Boolean isSelected() {
        return isSelected.get();
    }

    /*
     * Set the selected property of this position
     */
    void setSelected(boolean selected) {
        isSelected.set(selected);
    }

    /**
     * set the occupiedBy property to the given index
     */
    void setOccupiedBy(int playerIndex) {
        occupiedBy.set(playerIndex);
    }

    /**
     * get the occupiedBy value of this position
     */
    int getOccupiedBy() {
        return occupiedBy.get();
    }

    /**
     * set the 1-based finishOrder property to the given order
     */
    void setFinishOrder(int order) {
        if (0 < order && order < 5) {
            finishOrder.set(order);
        } else {
            finishOrder.set(-1);
        }
    }

    /**
     * Rotate the stackPane based on the position which indicated the layer and spot of the position and hence the player orientation
     *
     * @param stackPane the stackPane to rotate
     */
    private void rotateStackPane(StackPane stackPane) {
        if (position.layer().equals(Layer.EVENT)) {
            switch (position.spot()) {
                case 10 -> stackPane.setRotate(90);
                case 20 -> stackPane.setRotate(180);
                case 30 -> stackPane.setRotate(270);
            }
        } else if (position.layer().equals(Layer.HOME)) {
            switch (position.spot()) {
                case 10, 11, 12, 13 -> stackPane.setRotate(90);
                case 20, 21, 22, 23 -> stackPane.setRotate(180);
                case 30, 31, 32, 33 -> stackPane.setRotate(270);
            }
        }
    }

    /**
     * Create the appropriate letter based on the position
     *
     * @return the letter to display
     */
    private Text createLetter() {
        Text letter = new Text("");
        if (position.layer().equals(Layer.EVENT)) {
            switch (position.spot()) {
                case 0, 10, 20, 30 -> letter = new Text("A");
            }
        } else if (position.layer().equals(Layer.HOME)) {
            switch (position.spot()) {
                case 0, 10, 20, 30 -> letter = new Text("a");
                case 1, 11, 21, 31 -> letter = new Text("b");
                case 2, 12, 22, 32 -> letter = new Text("c");
                case 3, 13, 23, 33 -> letter = new Text("d");
            }
        }
        letter.setFill(Color.BLACK);
        letter.setFont(Font.font("System", FontWeight.BOLD, 11)); // Set the font to bold
        return letter;
    }

    /**
     * Set the fill of the circle based on the position
     *
     * @param position the position to set the fill for
     * @param circle   the circle to set the fill for
     */
    private static void setFill(Position position, Circle circle) {
        if (position.layer().equals(Layer.EVENT)) {
            switch (position.spot()) {
                case 0 -> circle.setFill(PlayerView.getColor(0));
                case 10 -> circle.setFill(PlayerView.getColor(1));
                case 20 -> circle.setFill(PlayerView.getColor(2));
                case 30 -> circle.setFill(PlayerView.getColor(3));
                default -> circle.setFill(Color.WHITE);
            }
        } else if (position.layer().equals(Layer.HOME)) {
            switch (position.spot()) {
                case 0, 1, 2, 3 -> circle.setFill(PlayerView.getColor(0));
                case 10, 11, 12, 13 -> circle.setFill(PlayerView.getColor(1));
                case 20, 21, 22, 23 -> circle.setFill(PlayerView.getColor(2));
                case 30, 31, 32, 33 -> circle.setFill(PlayerView.getColor(3));
                default -> circle.setFill(Color.BLACK);
            }
        } else {
            switch (position.spot()) {
                case 34 -> circle.setFill(PlayerView.getColor(0));
                case 4 -> circle.setFill(PlayerView.getColor(1));
                case 14 -> circle.setFill(PlayerView.getColor(2));
                case 24 -> circle.setFill(PlayerView.getColor(3));
                default -> circle.setFill(Color.BLACK);
            }
        }
    }

    /**
     * @return the position of this PositionView
     */
    Position getPosition() {
        return position;
    }

}
