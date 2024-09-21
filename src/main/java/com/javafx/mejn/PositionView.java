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

import static com.javafx.mejn.BoardView.currentPlayerIndex;
import static com.javafx.mejn.MainApp.showPositionNumbers;

public class PositionView {

    private final Position position;

    // Indicate if this field is a choice for the BoardView.PLAYER_INDEX and set border accordingly
    final BooleanProperty isChoice = new SimpleBooleanProperty(false);

    // The player index occupying this property. Any other value indicates, not occupied.
    final IntegerProperty occupiedBy = new SimpleIntegerProperty(-1);


    public PositionView(Position position, int x, int y, Pane boardPane, DoubleProperty cellWidth, DoubleProperty strokeWidth) {
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

        // Set the appropriate stroke Dash Array and color
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
        pawn.strokeWidthProperty().bind(strokeWidth);
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
        stackPane.getChildren().add(pawn);

        // Position the stackPane in the right place
        stackPane.layoutXProperty().bind(cellWidth.multiply(x).subtract(circle.radiusProperty()));
        stackPane.layoutYProperty().bind(cellWidth.multiply(y).subtract(circle.radiusProperty()));
        stackPane.prefWidthProperty().bind(circle.radiusProperty());
        rotateStackPane(stackPane);
        boardPane.getChildren().add(stackPane);
    }

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


    private Text createLetter() {
        Text letter = new Text("");
        if (position.layer().equals(Layer.EVENT)) {
            switch (position.spot()) {
                case 0, 10, 20, 30 -> letter =  new Text("A");
            }
        } else if (position.layer().equals(Layer.HOME)) {
            switch (position.spot()) {
                case 0, 10, 20, 30 -> letter =  new Text("a");
                case 1, 11, 21, 31 -> letter =  new Text("b");
                case 2, 12, 22, 32 -> letter =  new Text("c");
                case 3, 13, 23, 33 -> letter =  new Text("d");
            }
        }
        letter.setFill(Color.BLACK);
        letter.setFont(Font.font("System", FontWeight.BOLD, 11)); // Set the font to bold
        return letter;
    }

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


}
