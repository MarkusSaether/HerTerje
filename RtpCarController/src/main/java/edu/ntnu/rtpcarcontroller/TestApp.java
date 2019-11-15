package edu.ntnu.rtpcarcontroller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class TestApp extends Application {
    private static final Logger logger = Logger.getLogger(TestApp.class.getName());

    static {
        InputStream stream = TestApp.class.getClassLoader().getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
        Parent homeFXML = homeLoader.load();
        Scene homeScene = new Scene(homeFXML);
        primaryStage.setScene(homeScene);
        primaryStage.show();
    }
}
