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

import com.rttnghs.mejn.Player;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MainApplication extends Application {

    private static final Logger logger = LogManager.getLogger(MainApplication.class);
    static final String MANUAL_STRATEGY = "ManualStrategy";
    private final TextArea consoleTextArea = new TextArea();

    // Number of pixels between the border of the board and the border of the window
    public final static Double BORDER_OFFSET = 5.0;

    final static MenuItem captureDebugItem = new MenuItem("Capture");
    final static BooleanProperty showPositionNumbers = new SimpleBooleanProperty(false);
    final static DoubleProperty playbackSpeed = new SimpleDoubleProperty(10);
    static BoardView boardView;
    private final MenuItem strategyItem = new MenuItem("Strategies");
    final static ObservableList<String> strategyOptions = FXCollections.observableArrayList();
    static final ObservableList<String> strategySelections = FXCollections.observableArrayList();
    static final ObservableList<Player> players = FXCollections.observableArrayList();


    private Stage primaryStage;
    Controller controller;

    /**
     * @param primaryStage the primary stage for the application
     * @throws Exception when things go south
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Mens erger je niet!");

        controller = new Controller();

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

        controller.initialize(scene);
        // Initialize the strategy options and selections after the controller is initialized
        strategyItem.setOnAction((event) -> getConfigureGameStrategies(controller));

        primaryStage.show();
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        logger.info("Application is closing");
        if (controller != null) {
            controller.stop();
        }
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
        exitItem.setAccelerator(KeyCombination.keyCombination("Shortcut+X"));
        exitItem.setOnAction(_ -> primaryStage.close());
        menuFile.getItems().add(exitItem);
        menuBar.getMenus().add(menuFile);

        // Create Configure menu
        Menu menuConfigure = new Menu("Configure");

        // Create console sub-menu
        Menu menuConsole = new Menu("Console");
        CheckMenuItem enableConsoleCheckMenuItem = new CheckMenuItem("Enable Console");
        enableConsoleCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Shortcut+E"));
        enableConsoleCheckMenuItem.setSelected(true);
        TextAreaAppender.setTextArea(consoleTextArea, enableConsoleCheckMenuItem.selectedProperty());

        MenuItem clearConsoleItem = new MenuItem("Clear Console");
        clearConsoleItem.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        clearConsoleItem.setOnAction(_ -> consoleTextArea.clear());

        menuConsole.getItems().addAll(enableConsoleCheckMenuItem, clearConsoleItem);


        // Create Game sub-menu
        Menu menuGame = new Menu("Game");

        CheckMenuItem autoSelectSingleChoiceCheckMenuItem = new CheckMenuItem("Auto Select Single Choice");
        autoSelectSingleChoiceCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Shortcut+A"));
        autoSelectSingleChoiceCheckMenuItem.setSelected(true);
        controller.autoSelectSingleChoice.bind(autoSelectSingleChoiceCheckMenuItem.selectedProperty());
        menuGame.getItems().add(autoSelectSingleChoiceCheckMenuItem);

        MenuItem playbackSpeedItem = new MenuItem("Playback Speed");
        playbackSpeedItem.setOnAction(_ -> getPlaybackSpeedItem());

        menuGame.getItems().add(playbackSpeedItem);

        CheckMenuItem showPositionNumbersCheckMenuItem = new CheckMenuItem("Show Position Numbers");
        showPositionNumbersCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Shortcut+N"));
        showPositionNumbers.bind(showPositionNumbersCheckMenuItem.selectedProperty());
        showPositionNumbersCheckMenuItem.setSelected(false);
        menuGame.getItems().add(showPositionNumbersCheckMenuItem);

        menuGame.getItems().add(strategyItem);


        menuConfigure.getItems().addAll(menuConsole, menuGame);
        menuBar.getMenus().add(menuConfigure);


        // TODO: Remove after debugging
        Menu menuDebug = new Menu("Debug");
        captureDebugItem.setAccelerator(KeyCombination.keyCombination("Shortcut+D"));

        menuDebug.getItems().addAll(captureDebugItem);
        menuBar.getMenus().add(menuDebug);

        return menuBar;
    }

    /**
     * Create a dialog to configure the game strategies
     */
    private void getConfigureGameStrategies(Controller initializedController) {
        Dialog<Void> gameStrateyConfigDialog = new Dialog<>();
        gameStrateyConfigDialog.setTitle("Game Strategies");

        // Set the button types
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        gameStrateyConfigDialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        // Create the ComboBoxes and pre-set their values

        CheckBox checkBox0 = new CheckBox();
        ComboBox<String> comboBox0 = new ComboBox<>(strategyOptions);
        EventHandler<ActionEvent> checkBox0event = configureStrategyOptions(0, comboBox0, checkBox0);

        CheckBox checkBox1 = new CheckBox();
        ComboBox<String> comboBox1 = new ComboBox<>(strategyOptions);
        EventHandler<ActionEvent> checkBox1event = configureStrategyOptions(1, comboBox1, checkBox1);

        CheckBox checkBox2 = new CheckBox();
        ComboBox<String> comboBox2 = new ComboBox<>(strategyOptions);
        EventHandler<ActionEvent> checkBox2event = configureStrategyOptions(2, comboBox2, checkBox2);

        CheckBox checkBox3 = new CheckBox();
        ComboBox<String> comboBox3 = new ComboBox<>(strategyOptions);
        EventHandler<ActionEvent> checkBox3event = configureStrategyOptions(3, comboBox3, checkBox3);

        // Create a layout and add the HBoxes
        VBox vbox = new VBox(10);
        vbox.getChildren().add(createPlayerHBox(0, checkBox0, comboBox0));
        vbox.getChildren().add(createPlayerHBox(1, checkBox1, comboBox1));
        vbox.getChildren().add(createPlayerHBox(2, checkBox2, comboBox2));
        vbox.getChildren().add(createPlayerHBox(3, checkBox3, comboBox3));
        gameStrateyConfigDialog.getDialogPane().setContent(vbox);

        // Make sure that the dialog is centered on the primaryStage
        gameStrateyConfigDialog.initOwner(primaryStage);

        // Add a Label for error messages
        Label errorMessageLabel = new Label();
        errorMessageLabel.setTextFill(Color.RED);

// Add the Label to the VBox
        vbox.getChildren().add(errorMessageLabel);

        // Add listeners to checkboxes to ensure at least two are checked
        EventHandler<ActionEvent> checkboxListener = event -> {
            long checkedCount = Stream.of(checkBox0, checkBox1, checkBox2, checkBox3)
                    .filter(CheckBox::isSelected)
                    .count();
            boolean disableOkButton = checkedCount < 2;
            gameStrateyConfigDialog.getDialogPane().lookupButton(okButtonType).setDisable(disableOkButton);
            errorMessageLabel.setText(disableOkButton ? "Select a strategy for at least two players" : "");
        };

        checkBox0.setOnAction(event -> {
            checkBox0event.handle(event);
            checkboxListener.handle(event);
        });
        checkBox1.setOnAction(event -> {
            checkBox1event.handle(event);
            checkboxListener.handle(event);
        });
        checkBox2.setOnAction(event -> {
            checkBox2event.handle(event);
            checkboxListener.handle(event);
        });
        checkBox3.setOnAction(event -> {
            checkBox3event.handle(event);
            checkboxListener.handle(event);
        });

        // Initial validation
        checkboxListener.handle(null);

        // Set result converter to update strategySelections when OK is pressed
        gameStrateyConfigDialog.setResultConverter(buttonType -> {
            if (buttonType == okButtonType) {
                boolean hasChanged = !Objects.equals(comboBox0.getValue(), strategySelections.get(0)) ||
                        !Objects.equals(comboBox1.getValue(), strategySelections.get(1)) ||
                        !Objects.equals(comboBox2.getValue(), strategySelections.get(1)) ||
                        !Objects.equals(comboBox3.getValue(), strategySelections.get(3));

                if (hasChanged) {
                    boolean confirmed = showConfirmationDialog(primaryStage);
                    if (confirmed) {
                        updateStrategySelection(0, comboBox0.getValue());
                        updateStrategySelection(1, comboBox1.getValue());
                        updateStrategySelection(2, comboBox2.getValue());
                        updateStrategySelection(3, comboBox3.getValue());
                        initializedController.reset();
                    }
                }
            }
            return null;
        });

        // Show the dialog and wait for the result
        gameStrateyConfigDialog.showAndWait();
    }

    private static EventHandler<ActionEvent> configureStrategyOptions(int playerIndex, ComboBox<String> comboBox, CheckBox checkBox) {
        comboBox.setEditable(false);
        String strategySelection = strategySelections.get(playerIndex);
        if (strategySelection == null) {
            checkBox.setSelected(false);
            comboBox.getSelectionModel().clearSelection();
            comboBox.setDisable(true);
        } else {
            checkBox.setSelected(true);
            comboBox.setValue(strategySelection);
            comboBox.setDisable(false);
        }

        return event -> {
            if (checkBox.isSelected()) {
                String selectedStrategy = strategySelections.get(playerIndex);
                //  If the previous strategy was null, default to the configured strategy
                if (selectedStrategy == null) {
                    selectedStrategy = Controller.getConfiguredStrategies().get(playerIndex);
                }
                comboBox.setValue(selectedStrategy);
                comboBox.setDisable(false);
            } else {
                comboBox.getSelectionModel().clearSelection();
                comboBox.setDisable(true);
            }
        };
    }

    private boolean showConfirmationDialog(Stage primaryStage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Strategy Change");
        alert.setHeaderText(null);
        alert.setContentText("Changing strategies will reset the game.");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButtonType, cancelButtonType);

        alert.initOwner(primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == okButtonType;
    }

    private void updateStrategySelection(int index, String newValue) {
        String currentValue = strategySelections.get(index);
        if (!Objects.equals(currentValue, newValue)) {
            logger.warn("Strategy for player {} has changed from {} to {}. The game needs to be reset for this change to take effect.", index, currentValue, newValue);
            strategySelections.set(index, newValue);
        }
    }

    private HBox createPlayerHBox(int playerIndex, CheckBox checkBox, ComboBox<String> comboBox) {

        StackPane stackPane = new StackPane();

        Circle pawn = new Circle(12, PlayerView.getColor(playerIndex));
        pawn.setStrokeWidth(1);
        pawn.setStroke(Color.BLACK);
        pawn.setFill(PlayerView.getGradient(playerIndex));
        pawn.visibleProperty().bind(checkBox.selectedProperty());
        stackPane.getChildren().add(pawn);

        Circle circle = new Circle(12, PlayerView.getColor(playerIndex));
        circle.setStrokeWidth(2.5);
        circle.setStroke(Color.BLACK);
        //circle.setFill(PlayerView.getColor(playerIndex));
        circle.visibleProperty().bind(checkBox.selectedProperty().not());
        stackPane.getChildren().add(circle);

        // Create an HBox to hold the Circle and ComboBox
        return new HBox(12, checkBox, stackPane, comboBox);
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
