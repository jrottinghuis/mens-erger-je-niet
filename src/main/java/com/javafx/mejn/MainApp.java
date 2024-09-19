package com.javafx.mejn;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger(MainApp.class);
    private final TextArea consoleTextArea = new TextArea();

    // Number of pixels between the border of the board and the border of the window
    private final static Double BORDER_OFFSET = 5.0;

    private final static MenuItem debugItem = new MenuItem("Capture");

    private Scene scene;

    /**
     * @param primaryStage the primary stage for the application
     * @throws Exception when things go south
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Mens Erger Je Niet");

        // // Add a menubar, a status bar, and a toolbar
        MenuBar menuBar = createMenuBar(primaryStage);

        TabPane tabPane = createTabPane();

        VBox vBox = new VBox(menuBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        scene = new Scene(vBox, 800, 858);

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(KeyCombination.keyCombination("Ctrl+X"));
        exitItem.setOnAction(e -> primaryStage.close());
        menuFile.getItems().add(exitItem);
        menuBar.getMenus().add(menuFile);

        Menu menuConsole = new Menu("Console");

        CheckMenuItem enableConsoleCheckMenuItem = new CheckMenuItem("Enable Console");
        enableConsoleCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
        enableConsoleCheckMenuItem.setSelected(true);

        TextAreaAppender.setTextArea(consoleTextArea, enableConsoleCheckMenuItem.selectedProperty());

        MenuItem clearConsoleItem = new MenuItem("Clear Console");
        clearConsoleItem.setAccelerator(KeyCombination.keyCombination("Ctrl+C"));
        clearConsoleItem.setOnAction(e -> consoleTextArea.clear());

        menuConsole.getItems().addAll(enableConsoleCheckMenuItem, clearConsoleItem);
        menuBar.getMenus().add(menuConsole);

        // TODO: Remove after debugging
        Menu menuDebug = new Menu("Debug");
        debugItem.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
        menuDebug.getItems().add(debugItem);
        menuBar.getMenus().add(menuDebug);

        ToolBar toolBar = new ToolBar();
        Button toolBarButton = new Button("Toolbar Button");
        toolBar.getItems().add(toolBarButton);
        return menuBar;
    }

    private TabPane createTabPane() {

        DoubleProperty cellWidth = new SimpleDoubleProperty();
        DoubleProperty circleRadius = new SimpleDoubleProperty();
        DoubleProperty smallCircleRadius = new SimpleDoubleProperty();
        DoubleProperty strokeWidth = new SimpleDoubleProperty();

        Pane boardPane = new Pane();
        boardPane.setPrefSize(790, 790);
        AnchorPane boardAnchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(boardPane, BORDER_OFFSET);
        AnchorPane.setRightAnchor(boardPane, BORDER_OFFSET);
        AnchorPane.setLeftAnchor(boardPane, BORDER_OFFSET);
        AnchorPane.setBottomAnchor(boardPane, BORDER_OFFSET);
        boardAnchorPane.getChildren().add(boardPane);
        // Add a listener to boardPane to resize the height when the width changes
        boardPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            boardPane.setPrefHeight(newValue.doubleValue());
        });

        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("Board", boardAnchorPane);
        tab1.setClosable(false);

        // A cell on a real board is 2.5mm. All other sizes are derived from this.
        cellWidth.bind(boardPane.widthProperty().divide(13));
        circleRadius.bind(cellWidth.multiply(0.375)); // 0.75/2.5=0.6
        smallCircleRadius.bind(cellWidth.multiply(0.22)); // 0.55/2.5=0.22
        strokeWidth.bind(cellWidth.divide(18)); // 0.1/2.5=0.04 = 1/25

        addBorders(strokeWidth, cellWidth, boardPane);
        addLine(boardPane, strokeWidth, cellWidth, 6, 6, 8, 12);
        addLine(boardPane, strokeWidth, cellWidth, 2, 6, 8, 8);
        addLine(boardPane, strokeWidth, cellWidth, 2, 2, 6, 8);
        addLine(boardPane, strokeWidth, cellWidth, 2, 6, 6, 6);
        addLine(boardPane, strokeWidth, cellWidth, 6, 6, 2, 6);
        addLine(boardPane, strokeWidth, cellWidth, 6, 8, 2, 2);
        addLine(boardPane, strokeWidth, cellWidth, 6, 8, 2, 2);
        addLine(boardPane, strokeWidth, cellWidth, 8, 8, 2, 6);
        addLine(boardPane, strokeWidth, cellWidth, 8, 12, 6, 6);
        addLine(boardPane, strokeWidth, cellWidth, 12, 12, 6, 8);
        addLine(boardPane, strokeWidth, cellWidth, 8, 12, 8, 8);
        addLine(boardPane, strokeWidth, cellWidth, 8, 8, 8, 12);
        addLine(boardPane, strokeWidth, cellWidth, 8, 6, 12, 12);


        Circle circle = new Circle();
        circle.strokeWidthProperty().bind(strokeWidth);
        circle.setStroke(javafx.scene.paint.Color.BLACK);
        circle.setFill(javafx.scene.paint.Color.WHITE);
        circle.radiusProperty().bind(circleRadius);
        circle.centerXProperty().bind(cellWidth.multiply(6));
        circle.centerYProperty().bind(cellWidth.multiply(12));
        boardPane.getChildren().add(circle);

        Circle circle1 = new Circle();
        circle1.strokeWidthProperty().bind(strokeWidth);
        circle1.setStroke(javafx.scene.paint.Color.BLACK);
        circle1.setFill(javafx.scene.paint.Color.WHITE);
        circle1.radiusProperty().bind(circleRadius);
        circle1.centerXProperty().bind(cellWidth.multiply(6));
        circle1.centerYProperty().bind(cellWidth.multiply(11));
        boardPane.getChildren().add(circle1);

        debugItem.setOnAction(e -> {
            // log scene height and width
            logger.error("Scene Width, Height: {}, {}", scene.getWidth(), scene.getHeight());
            logger.error("Cell Width: {}", cellWidth.get());
            logger.error("BoardPane Width, Height: {}, {}", boardPane.getWidth(), boardPane.getHeight());
            logger.error("Debugging Circle X, Y, r: {}, {}, {}", circle.getCenterX(), circle.getCenterY(), circle.getRadius());
            logger.error("Debugging Circle1 X, Y, r: {}, {}, {}", circle1.getCenterX(), circle1.getCenterY(), circle1.getRadius());
            logger.error("Distance between circle and circle1: {}", circle.getCenterY() - circle1.getCenterY());
            logger.error("");
        });

        Circle circle2 = new Circle();
        circle2.strokeWidthProperty().bind(strokeWidth);
        circle2.setStroke(javafx.scene.paint.Color.BLACK);
        circle2.setFill(javafx.scene.paint.Color.WHITE);
        circle2.setOpacity(1.0);
        circle2.radiusProperty().bind(circleRadius);
        circle2.centerXProperty().bind(cellWidth.multiply(6));
        circle2.centerYProperty().bind(cellWidth.multiply(8));
        boardPane.getChildren().add(circle2);

        /*GridPane.setHalignment(boardGridPane, HPos.CENTER);
        GridPane.setHgrow(boardGridPane, Priority.ALWAYS);
        GridPane.setValignment(boardGridPane, VPos.CENTER);
        GridPane.setVgrow(boardGridPane, Priority.ALWAYS);



        AnchorPane boardAnchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(boardGridPane, BORDER_OFFSET);
        AnchorPane.setRightAnchor(boardGridPane, BORDER_OFFSET);
        AnchorPane.setLeftAnchor(boardGridPane, BORDER_OFFSET);
        AnchorPane.setBottomAnchor(boardGridPane, BORDER_OFFSET);
        boardAnchorPane.getChildren().add(boardGridPane);

        // Red outside border, scaling with overall tab size

        BorderWidths borderWidths = new BorderWidths(10.6, 10.6, 10.6, 10.6);

        BorderStroke borderStroke = new BorderStroke(Color.CRIMSON, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, borderWidths);
        //boardGridPane.setBorder(new Border(borderStroke));
        boardGridPane.setBorder(new Border(borderStroke));

         */

        Tab tab2 = getConsoleTab();
        tab2.setClosable(false);
        tab2.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && !oldValue) {
                consoleTextArea.setScrollTop(Double.MAX_VALUE);
            }
        });

        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);

        return tabPane;
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

    private static void addBorders(DoubleProperty strokeWidth, DoubleProperty cellWidth, Pane boardPane) {
        Rectangle redBorder = new Rectangle();
        redBorder.strokeWidthProperty().bind(strokeWidth.multiply(2));
        redBorder.setStroke(Color.CRIMSON);
        redBorder.setStrokeType(StrokeType.INSIDE);
        redBorder.setFill(Color.KHAKI);
        redBorder.widthProperty().bind(cellWidth.multiply(13));
        redBorder.heightProperty().bind(cellWidth.multiply(13));
        boardPane.getChildren().add(redBorder);

        // Add another rectangle with black stroke and transparent color half the cellWidth inside the boardPane
        Rectangle blackBorder = new Rectangle();
        blackBorder.strokeWidthProperty().bind(strokeWidth);
        blackBorder.setStroke(Color.BLACK);
        blackBorder.setFill(Color.TRANSPARENT);
        blackBorder.widthProperty().bind(cellWidth.multiply(12.5));
        blackBorder.heightProperty().bind(cellWidth.multiply(12.5));
        blackBorder.xProperty().bind(cellWidth.multiply(0.25));
        blackBorder.yProperty().bind(cellWidth.multiply(0.25));
        boardPane.getChildren().add(blackBorder);
    }

    private Tab getConsoleTab() {
        consoleTextArea.setWrapText(false);
        consoleTextArea.setEditable(false);
        //consoleTextArea.setText("Enable console through the menu.\n");
        consoleTextArea.setScrollTop(Double.MAX_VALUE);

        AnchorPane consoleAnchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(consoleTextArea, BORDER_OFFSET);
        AnchorPane.setRightAnchor(consoleTextArea, BORDER_OFFSET);
        AnchorPane.setLeftAnchor(consoleTextArea, BORDER_OFFSET);
        AnchorPane.setBottomAnchor(consoleTextArea, BORDER_OFFSET);

        consoleAnchorPane.getChildren().add(consoleTextArea);

        return new Tab("Console", consoleAnchorPane);
    }
}
