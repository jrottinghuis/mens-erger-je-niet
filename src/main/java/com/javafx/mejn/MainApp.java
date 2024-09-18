package com.javafx.mejn;

import com.rttnghs.mejn.Tournament;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger(MainApp.class);

    private final TextArea consoleTextArea = new TextArea();

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
        Scene scene = new Scene(vBox, 800, 600);

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private TabPane createTabPane() {

        // TODO: Remove this when the game is implemented.
        Button btn1 = new Button();
        btn1.setText("Say 'Hello World'");
        btn1.setOnAction(event -> logger.error("Throw the Hello World Error message."));

        Button btn2 = new Button();
        btn2.setText("Play Tournament");
        btn2.setOnAction(event -> logger.error("Throw the Play Tournament looooooooooong message."));

        VBox buttonvBox = new VBox(btn1, btn2);

        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("Board", buttonvBox);
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
        consoleTextArea.setText("Enable console through the menu.\n");
        consoleTextArea.setScrollTop(Double.MAX_VALUE);

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

        TextAreaAppender.setTextArea(consoleTextArea, enqbleConfoleCheckMenuItem.selectedProperty());

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
