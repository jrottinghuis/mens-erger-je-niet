package com.javafx.mejn;

import javafx.application.Application;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    private final TextArea consoleTextArea = new TextArea();

    /**
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Mens Erger Je Niet");

        // // Add a menubar, a status bar, and a toolbar
        MenuBar menuBar = createMenuBar(primaryStage);


        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });


        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("Board", new Label("Show all planes available"));
        tab1.setClosable(false);

        Tab tab2 = getConsoleTab();
        tab2.setClosable(false);

        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);

        AnchorPane anchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(menuBar, 0.0);

        VBox vBox = new VBox(menuBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        Scene scene = new Scene(vBox, 800, 600);

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private Tab getConsoleTab() {
        consoleTextArea.setWrapText(true);
        consoleTextArea.setEditable(false);
        consoleTextArea.setText("Enable console through the menu.");

        AnchorPane consoleAnchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(consoleTextArea, 5.0);
        AnchorPane.setRightAnchor(consoleTextArea, 5.0);
        AnchorPane.setLeftAnchor(consoleTextArea, 5.0);
        AnchorPane.setBottomAnchor(consoleTextArea, 5.0);

        consoleAnchorPane.getChildren().add(consoleTextArea);

        //VBox vBox = new VBox(textArea);

        return new Tab("Console", consoleAnchorPane);
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> primaryStage.close());
        menuFile.getItems().add(exitItem);
        menuBar.getMenus().add(menuFile);

        Menu menuConsole = new Menu("Console");

        CheckMenuItem enqbleConfoleCheckMenuItem = new CheckMenuItem("Enable Console");
        enqbleConfoleCheckMenuItem.setSelected(false);
        enqbleConfoleCheckMenuItem.setOnAction(e -> System.out.println("Enable console: " + enqbleConfoleCheckMenuItem.isSelected()));

        MenuItem clearConsoleItem = new MenuItem("Clear Console");
        clearConsoleItem.setOnAction(e -> consoleTextArea.clear());

        menuConsole.getItems().addAll(enqbleConfoleCheckMenuItem, clearConsoleItem);
        menuBar.getMenus().add(menuConsole);

        ToolBar toolBar = new ToolBar();
        Button toolBarButton = new Button("Toolbar Button");
        toolBar.getItems().add(toolBarButton);
        return menuBar;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
