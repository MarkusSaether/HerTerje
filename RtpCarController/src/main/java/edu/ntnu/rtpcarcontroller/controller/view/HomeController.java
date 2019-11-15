package edu.ntnu.rtpcarcontroller.controller.view;

import edu.ntnu.rtpcarcontroller.controller.DrivingController;
import edu.ntnu.rtpcarcontroller.controller.connection.ConnController;
import edu.ntnu.rtpcarcontroller.model.Car;
import edu.ntnu.rtpcarcontroller.model.Steer;
import edu.ntnu.rtpcarcontroller.model.Throttle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public class HomeController implements Initializable {
    private static final Logger logger = Logger.getLogger(HomeController.class.getName());

    @FXML private VBox rootPane;
    @FXML private Button disconnectButton;
    private Scene homeScene;
    private Stage homeStage;
    private ConnController connController;
    private DrivingController drivingController;
    private HashMap<KeyCode, Boolean> keyPressed;
    private EventHandler<KeyEvent> keyPressedEventHandler;
    private EventHandler<KeyEvent> keyReleasedEventHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.log(Level.FINE, "Initializing main GUI controller");

        Car car = new Car();

        connController = ConnController.INSTANCE;
        connController.setCar(car);
        connController.addConnectionLossHandler(event -> Platform.runLater(() -> {
            disableKeyboardHandlers();
            configureConnection();
        }));

        drivingController = DrivingController.INSTANCE;
        drivingController.setCar(car);

        keyPressed = new HashMap<>();
        keyPressed.put(KeyCode.UP, false);
        keyPressed.put(KeyCode.DOWN, false);
        keyPressed.put(KeyCode.LEFT, false);
        keyPressed.put(KeyCode.RIGHT, false);

        // The getScene() and getWindow() methods and dependent methods need to be executed later, since getScene() and
        // getWindow() return null if executed in the initialize method.
        Platform.runLater(() -> {
            homeScene = rootPane.getScene();
            homeStage = (Stage) homeScene.getWindow();

            homeStage.setOnCloseRequest(e -> {
                if (connController.isConnectionActive()) {
                    connController.disconnect();
                }
                homeStage.close();
            });

            configureConnection();
        });
    }

    /**
     * Creates a new Stage in which the user can configure the server address and port to connect to.
     */
    private void configureConnection() {
        logger.log(Level.INFO, "Requesting connection configuration info from user");
        try {
            logger.log(Level.FINE, "Setting up connection configuration window");
            FXMLLoader connConfigLoader = new FXMLLoader(getClass().getResource("/fxml/connectionconfig.fxml"));
            Parent connConfigFXML = connConfigLoader.load();
            Scene connConfigScene = new Scene(connConfigFXML);
            Stage connConfigStage = new Stage();
            connConfigStage.setScene(connConfigScene);
            connConfigStage.initModality(Modality.APPLICATION_MODAL);

            logger.log(Level.FINE, "Waiting for user to confirm or cancel connection configuration");
            connConfigStage.showAndWait();

            logger.log(Level.FINE, "User confirmed or canceled connection configuration");
            // Returning inactive from showAndWait() indicates user cancelled connection config, so close application
            if (connController.isConnectionActive()) {
                logger.log(Level.FINE, "Active connection detected; enabling keyboard handlers");
                enableKeyboardHandlers();
            } else {
                logger.log(Level.FINE, "No active connection detected; closing application");
                homeStage.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns whether the given KeyCode is a key that belongs to a steer function of the Car.
     * @param key The KeyCode to check.
     * @return True if the given KeyCode belongs to a steer function of the Car.
     */
    private boolean isSteerKey(KeyCode key) {
        return key == KeyCode.LEFT || key == KeyCode.RIGHT;
    }

    /**
     * Returns whether the given KeyCode is a key that belongs to a throttle function of the Car.
     * @param key The KeyCode to check.
     * @return True if the given KeyCode belongs to a throttle function of the Car.
     */
    private boolean isThrottleKey(KeyCode key) {
        return key == KeyCode.UP || key == KeyCode.DOWN;
    }

    /**
     * Returns the Throttle that corresponds to the given KeyCode.
     * @param key The KeyCode for which the associated Throttle needs to be returned.
     * @return The Throttle that corresponds to the given KeyCode, or null if none exists.
     */
    private Throttle getThrottleFromKey(KeyCode key) {
        switch (key) {
            case UP:
                return Throttle.FORWARD;
            case DOWN:
                return Throttle.REVERSE;
        }
        return null;
    }

    /**
     * Returns the Steer that corresponds to the given KeyCode.
     * @param key The KeyCode for which the associated Steer needs to be returned.
     * @return The Steer that corresponds to the given KeyCode, or null if none exists.
     */
    private Steer getSteerFromKey(KeyCode key) {
        switch (key) {
            case RIGHT:
                return Steer.RIGHT;
            case LEFT:
                return Steer.LEFT;
        }
        return null;
    }

    /**
     * Returns the KeyCode that is associated with the action that is opposite to the action associated with the given
     * KeyCode.
     * @param key The key for which the opposite key needs to be returned.
     * @return The KeyCode that is associated with the action that is opposite to the action associated with the given
     * KeyCode, or null if the given KeyCode has no opposite in this piece of software.
     */
    private KeyCode getOppositeKey(KeyCode key) {
        KeyCode opposite = null;
        switch (key) {
            case UP:
                opposite = KeyCode.DOWN;
                break;
            case DOWN:
                opposite = KeyCode.UP;
                break;
            case LEFT:
                opposite = KeyCode.RIGHT;
                break;
            case RIGHT:
                opposite = KeyCode.LEFT;
                break;
        }
        return opposite;
    }

    /**
     * Tells the DrivingController to throttle.
     * @param dir The direction to Throttle in.
     */
    private void throttle(Throttle dir) {
        drivingController.throttle(dir);
    }

    /**
     * Tells the DrivingController to steer.
     * @param dir The direction to steer in.
     */
    private void steer(Steer dir) {
        drivingController.steer(dir);
    }

    /**
     * Enables the keyboard event filters related to driving the car. Creates the EventHandlers if they had not yet been
     * created.
     */
    private void enableKeyboardHandlers() {
        logger.log(Level.FINE, "Setting up keyboard handlers");

        if (keyPressedEventHandler == null) {
            logger.log(Level.FINE, "keyPressedEventHandler did not yet exist. Creating new keyPressedEventHandler");
            keyPressedEventHandler = event -> {
                KeyCode key = event.getCode();
                if (keyPressed.containsKey(key)) {
                    keyPressed.put(key, true);
                    if (isThrottleKey(key)) {
                        throttle(getThrottleFromKey(key));
                    } else if (isSteerKey(key)) {
                        steer(getSteerFromKey(key));
                    }
                }
            };
        }

        if (keyReleasedEventHandler == null) {
            logger.log(Level.FINE, "keyReleasedEventHandler did not yet exist. Creating new keyReleasedEventHandler");
            keyReleasedEventHandler = event -> {
                KeyCode key = event.getCode();
                if (keyPressed.containsKey(key)) {
                    keyPressed.put(key, false);
                    KeyCode opposite = getOppositeKey(key);
                    boolean oppositePressed = keyPressed.get(opposite);

                    if (isThrottleKey(key)) {
                        throttle(oppositePressed ? getThrottleFromKey(opposite) : Throttle.NEUTRAL);
                    } else if (isSteerKey(key)) {
                        steer(oppositePressed ? getSteerFromKey(opposite) : Steer.NEUTRAL);
                    }
                }
            };
        }

        homeScene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedEventHandler);
        homeScene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleasedEventHandler);
    }

    /**
     * Disables the keyboard event filters related to driving the car.
     */
    private void disableKeyboardHandlers() {
        logger.log(Level.FINE, "Disabling keyboard handlers");
        homeScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedEventHandler);
        homeScene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedEventHandler);
    }

    @FXML
    public void disconnect(ActionEvent actionEvent) {
        disableKeyboardHandlers();
        connController.disconnect();
        configureConnection();
    }
}
