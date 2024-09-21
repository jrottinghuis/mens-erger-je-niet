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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    public static BoardView boardView;

    /**
     * @param primaryStage the primary stage for the application
     * @throws Exception when things go south
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Mens Erger Je Niet");

        MenuBar menuBar = createMenuBar(primaryStage);

        TabPane tabPane = createTabPane();

        VBox vBox = new VBox(menuBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        Scene scene = new Scene(vBox, 700, 900);

        primaryStage.setScene(scene);
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
    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(KeyCombination.keyCombination("Ctrl+X"));
        exitItem.setOnAction(_ -> primaryStage.close());
        menuFile.getItems().add(exitItem);
        menuBar.getMenus().add(menuFile);

        Menu menuConsole = new Menu("Console");

        CheckMenuItem enableConsoleCheckMenuItem = new CheckMenuItem("Enable Console");
        enableConsoleCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
        enableConsoleCheckMenuItem.setSelected(true);

        TextAreaAppender.setTextArea(consoleTextArea, enableConsoleCheckMenuItem.selectedProperty());

        MenuItem clearConsoleItem = new MenuItem("Clear Console");
        clearConsoleItem.setAccelerator(KeyCombination.keyCombination("Ctrl+C"));
        clearConsoleItem.setOnAction(_ -> consoleTextArea.clear());

        // Create Play menu
        Menu menuPlay = new Menu("Play");
        Slider playbackSpeedSlider = new Slider(1, 100, 25);
        playbackSpeedSlider.setShowTickLabels(true);
        playbackSpeedSlider.setShowTickMarks(true);
        playbackSpeedSlider.setMajorTickUnit(10);
        playbackSpeedSlider.setMinorTickCount(1);
        playbackSpeedSlider.setBlockIncrement(1);
        CustomMenuItem playbackSpeedItem = new CustomMenuItem(playbackSpeedSlider);
        playbackSpeedItem.setHideOnClick(false);
        menuPlay.getItems().add(playbackSpeedItem);
        menuBar.getMenus().add(menuPlay);

        menuConsole.getItems().addAll(enableConsoleCheckMenuItem, clearConsoleItem);
        menuBar.getMenus().add(menuConsole);

        // TODO: Remove after debugging
        Menu menuDebug = new Menu("Debug");
        debugItem.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
        CheckMenuItem showPositionNumbersCheckMenuItem = new CheckMenuItem("Show Position Numbers");
        showPositionNumbersCheckMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        showPositionNumbers.bind(showPositionNumbersCheckMenuItem.selectedProperty());
        showPositionNumbersCheckMenuItem.setSelected(false);
        menuDebug.getItems().addAll(debugItem, showPositionNumbersCheckMenuItem);
        menuBar.getMenus().add(menuDebug);

        ToolBar toolBar = new ToolBar();
        Button toolBarButton = new Button("Toolbar Button");
        toolBar.getItems().add(toolBarButton);
        return menuBar;
    }

    private TabPane createTabPane() {

        BorderPane borderPane = new BorderPane();

        boardView = new BoardView(borderPane);

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
