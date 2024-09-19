package com.javafx.mejn;

import javafx.application.Application;
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
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger(MainApp.class);
    private final TextArea consoleTextArea = new TextArea();

    // Number of pixels between the border of the board and the border of the window
    private final static Double BORDER_OFFSET = 5.0;

    private final static MenuItem debugItem = new MenuItem("Capture");


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
        Scene scene = new Scene(vBox, 400, 400);

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
        GridPane boardGridPane = new GridPane(12, 12);
        boardGridPane.setAlignment(Pos.CENTER);
        GridPane.setHalignment(boardGridPane, HPos.CENTER);
        GridPane.setValignment(boardGridPane, VPos.CENTER);
        //GridPane.setVgrow(boardGridPane, Priority.ALWAYS);
        //GridPane.setHgrow(boardGridPane, Priority.ALWAYS);
        // TODO: REMOVE THIS after debugging
        boardGridPane.setGridLinesVisible(true);


        boardGridPane.setHgap(0);
        boardGridPane.setVgap(0);

        Circle circle = new Circle(15);
        circle.setStrokeWidth(5);
        circle.setStroke(javafx.scene.paint.Color.BLACK);
        circle.setFill(javafx.scene.paint.Color.WHITE);

        Circle circle1 = new Circle(15);
        circle1.setStrokeWidth(5); // Cell width / 12.5
        circle1.setStroke(javafx.scene.paint.Color.BLACK);
        circle1.setFill(javafx.scene.paint.Color.WHITE);

        Circle circle2 = new Circle(15);
        circle2.setStrokeWidth(5); // Cell width / 12.5
        circle2.setStroke(javafx.scene.paint.Color.BLACK);
        circle2.setFill(javafx.scene.paint.Color.WHITE);
        circle2.setOpacity(1.0);

        for (int i = 0; i < 12; i++) {
            ColumnConstraints columnI = new ColumnConstraints();
            columnI.setPercentWidth(100.0 / 12);
            boardGridPane.getColumnConstraints().add(columnI);
            RowConstraints rowI = new RowConstraints();
            rowI.setPercentHeight(100.0 / 12);
            boardGridPane.getRowConstraints().add(rowI);
        }

        StackPane cell = new StackPane();
        cell.setAlignment(Pos.CENTER);
        cell.getChildren().add(circle);
        boardGridPane.add(cell, 5, 10);

        debugItem.setOnAction(e -> {
            logger.error("Debugging Circle X, Y, r: {}, {}, {}", circle.getCenterX(), circle.getCenterY(), circle.getRadius());
            logger.error("cell Width, Height: {}, {}", cell.getWidth(), cell.getHeight());
        });

        StackPane cell1 = new StackPane();
        cell1.setAlignment(Pos.CENTER);
        cell1.getChildren().add(circle1);
        boardGridPane.add(cell1, 11, 11);

        StackPane cell2 = new StackPane();
        cell2.setAlignment(Pos.CENTER);
        cell2.getChildren().add(circle2);
        // boardGridPane.add(cell2, 5, 7);


        Line line1 = new Line();
        line1.setStartX(0);
        line1.setStartY(0);
        line1.setEndX(0);

        line1.setStrokeWidth(5);
        line1.setStroke(Color.BLACK);
        line1.endYProperty().bind(cell.heightProperty().multiply(4));
        StackPane lineContainer = new StackPane();
        lineContainer.setAlignment(Pos.CENTER);
        lineContainer.getChildren().add(line1);
        boardGridPane.add(lineContainer, 5, 7, 1, 4);
        GridPane.setValignment(lineContainer, VPos.CENTER);


        StackPane boardStackPane = new StackPane();
        //boardGridPane.setStyle("-fx-background-color: cornsilk;");
        boardStackPane.getChildren().addAll(boardGridPane);

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


        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("Board", boardStackPane);
        tab1.setClosable(false);

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
