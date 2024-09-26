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

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger(MainApp.class);
    private final TextArea consoleTextArea = new TextArea();

    // Number of pixels between the border of the board and the border of the window
    public final static Double BORDER_OFFSET = 5.0;

    final static MenuItem debugItem = new MenuItem("Capture");
    final static BooleanProperty showPositionNumbers = new SimpleBooleanProperty(false);
    final static DoubleProperty playbackSpeed = new SimpleDoubleProperty(25);
    static BoardView boardView;
    final static ObservableList<String> strategyOptions = FXCollections.observableArrayList();
    static final ObservableList<String> strategySelections = FXCollections.observableArrayList();

    private Stage primaryStage;

    /**
     * @param primaryStage the primary stage for the application
     * @throws Exception when things go south
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Mens erger je niet!");

        Controller controller = new Controller();

        MenuBar menuBar = createMenuBar(primaryStage, controller);
        TabPane tabPane = createTabPane(controller);

        tabPane.setEventDispatcher(new EventDispatcher() {
            @Override
            public Event dispatchEvent(Event event, EventDispatchChain tail) {
                try {
                    return tail.dispatchEvent(event);
                } catch (final Exception e) {
                    logger.error("Error in event dispatching", e);
                    return null;
                }
            }
        });


        VBox vBox = new VBox(menuBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        Scene scene = new Scene(vBox, 700, 900);

        primaryStage.setScene(scene);

        controller.initialize();

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Create the menu bar
     *
     * @param primaryStage the primary stage for the application
     * @return the menu bar
     */
    private MenuBar createMenuBar(Stage primaryStage, Controller controller) {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(KeyCombination.keyCombination("Ctrl+X"));
        // TODO: Cancel all background tasks and stop all background (daemon) threads.
        exitItem.setOnAction(_ -> primaryStage.close());
        menuFile.getItems().add(exitItem);
        menuBar.getMenus().add(menuFile);

        // Create Configure menu
        Menu menuConfigure = new Menu("Configure");

        // Create console sub-menu
        Menu menuConsole = new Menu("Console");
        CheckMenuItem enableConsoleCheckMenuItem = new CheckMenuItem("Enable Console");
        enableConsoleCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
        enableConsoleCheckMenuItem.setSelected(true);
        TextAreaAppender.setTextArea(consoleTextArea, enableConsoleCheckMenuItem.selectedProperty());

        MenuItem clearConsoleItem = new MenuItem("Clear Console");
        clearConsoleItem.setAccelerator(KeyCombination.keyCombination("Ctrl+C"));
        clearConsoleItem.setOnAction(_ -> consoleTextArea.clear());

        menuConsole.getItems().addAll(enableConsoleCheckMenuItem, clearConsoleItem);


        // Create Game sub-menu
        Menu menuGame = new Menu("Game");
        MenuItem strategyItem = new MenuItem("Strategies");
        menuGame.getItems().add(strategyItem);
        //strategyItem.setOnAction(_ -> getConfigureGameStrategies());
        strategyItem.setOnAction((event) -> getConfigureGameStrategies());

        MenuItem playbackSpeedItem = new MenuItem("Playback Speed");
        menuGame.getItems().add(playbackSpeedItem);
        playbackSpeedItem.setOnAction(_ -> getPlaybackSpeedItem());

        menuConfigure.getItems().addAll(menuConsole, menuGame);
        menuBar.getMenus().add(menuConfigure);


        // TODO: Remove after debugging
        Menu menuDebug = new Menu("Debug");
        debugItem.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
        CheckMenuItem showPositionNumbersCheckMenuItem = new CheckMenuItem("Show Position Numbers");
        showPositionNumbersCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        showPositionNumbers.bind(showPositionNumbersCheckMenuItem.selectedProperty());
        showPositionNumbersCheckMenuItem.setSelected(false);
        menuDebug.getItems().addAll(debugItem, showPositionNumbersCheckMenuItem);
        menuBar.getMenus().add(menuDebug);

        return menuBar;
    }

    /**
     * Create a dialog to configure the game strategies
     */
   private void getConfigureGameStrategies() {
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Game Strategies");

    // Set the button types
    ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

    // Create the ComboBoxes and pre-set their values
    ComboBox<String> comboBox0 = new ComboBox<>(strategyOptions);
    comboBox0.setEditable(false);
    comboBox0.setValue(strategySelections.get(0));

    ComboBox<String> comboBox1 = new ComboBox<>(strategyOptions);
    comboBox1.setEditable(false);
    comboBox1.setValue(strategySelections.get(1));

    ComboBox<String> comboBox2 = new ComboBox<>(strategyOptions);
    comboBox2.setEditable(false);
    comboBox2.setValue(strategySelections.get(2));

    ComboBox<String> comboBox3 = new ComboBox<>(strategyOptions);
    comboBox3.setEditable(false);
    comboBox3.setValue(strategySelections.get(3));

    // Create a layout and add the HBoxes
    VBox vbox = new VBox(10, createPlayerHBox(0, comboBox0), createPlayerHBox(1, comboBox1), createPlayerHBox(2, comboBox2), createPlayerHBox(3, comboBox3));
    dialog.getDialogPane().setContent(vbox);

    // Set result converter to update strategySelections when OK is pressed
    dialog.setResultConverter(buttonType -> {
        if (buttonType == okButtonType) {
            strategySelections.set(0, comboBox0.getValue());
            strategySelections.set(1, comboBox1.getValue());
            strategySelections.set(2, comboBox2.getValue());
            strategySelections.set(3, comboBox3.getValue());
        }
        return null;
    });

    // Make sure that the dialog is centered on the primaryStage
    dialog.initOwner(primaryStage);

    // Show the dialog and wait for the result
    dialog.showAndWait();
}

private HBox createPlayerHBox(int playerIndex, ComboBox<String> comboBox) {
    // Create the Circle (pawn) for the ComboBox
    Circle pawn = new Circle(12, PlayerView.getColor(playerIndex));
    pawn.setStrokeWidth(1);
    pawn.setStroke(Color.BLACK);
    pawn.setFill(PlayerView.getGradient(playerIndex));

    // Create an HBox to hold the Circle and ComboBox
    return new HBox(12, pawn, comboBox);
}
    /**
     * Create a dialog to configure the game playback speed
     */
    private void getPlaybackSpeedItem() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Playback Speed");

        // Set the button types
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        // Create the playback speed slider
        Slider playbackSpeedSlider = new Slider(1, 100, playbackSpeed.get());
        playbackSpeedSlider.setShowTickLabels(true);
        playbackSpeedSlider.setShowTickMarks(true);
        playbackSpeedSlider.setMajorTickUnit(10);
        playbackSpeedSlider.setMinorTickCount(1);
        playbackSpeedSlider.setBlockIncrement(1);

        // Create a layout and add the slider
        VBox vbox = new VBox(10, playbackSpeedSlider);
        dialog.getDialogPane().setContent(vbox);

        // create a listener for the OK button to set set playbackSpeed to the value of the slider
        dialog.setResultConverter(buttonType -> {
            if (buttonType == okButtonType) {
                playbackSpeed.set(playbackSpeedSlider.getValue());
            }
            return null;
        });

        // Make sure that the dialog is centered on the primaryStage
        dialog.initOwner(primaryStage);

        // Show the dialog and wait for the result
        dialog.showAndWait();
    }

    private TabPane createTabPane(Controller controller) {

        BorderPane borderPane = new BorderPane();

        boardView = new BoardView(borderPane, controller);

        Tab tab2 = getConsoleTab();
        tab2.setClosable(false);
        tab2.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (newValue && !oldValue) {
                consoleTextArea.setScrollTop(Double.MAX_VALUE);
            }
        });

        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("Board", borderPane);
        tab1.setClosable(false);

        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);

        return tabPane;
    }

    private Tab getConsoleTab() {
        consoleTextArea.setWrapText(false);
        consoleTextArea.setEditable(false);
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
