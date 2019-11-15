package edu.ntnu.rtpcarcontroller.controller.connection;

import edu.ntnu.rtpcarcontroller.event.ConnectionLossEvent;
import edu.ntnu.rtpcarcontroller.exception.NetworkConnectionException;
import edu.ntnu.rtpcarcontroller.model.Car;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller that represents one connection to a remote car ('server') and handles all communication with that car.
 * It observes a Car instance and is responsible for communicating any throttling and steering changes to the remote
 * server.
 *
 * ConnController makes active use of ConnInputController and ConnOutputController for handling the actual input and
 * output endpoints.
 */
public enum ConnController {
    INSTANCE;
    private static final int HANDSHAKE_TIMEOUT = 5000;
    private static final Logger logger = Logger.getLogger(ConnController.class.getName());

    /**
     * Returns whether the provided value would be a valid server address.
     * @param value The value to check.
     * @return True if the provided value would be a valid server address.
     */
    public static boolean isValidServerAddress(String value) {
        boolean result;
        try {
            InetAddress.getByName(value);
            result = true;
        } catch (UnknownHostException e) {
            result = false;
        }
        logger.log(Level.FINE, String.format("Validity of server address %s was considered %b", value, result));
        return result;
    }

    /**
     * Returns whether the provided value could be a valid port number (integer and in the right range).
     * @param value The value to check.
     * @return True if the provided value could be a valid port number.
     */
    public static boolean isValidPortNumber(String value) {
        return value.matches(
                "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$"
        );
    }


    private final List<ConnectionLossEvent.Handler> connectionLossListeners;
    private Car car;
    private Socket socket;
    private ConnInputController inputController;
    private ConnOutputController outputController;
    private boolean active;
    private boolean validated;

    ConnController() {
        connectionLossListeners = new ArrayList<>();
        active = false;
    }

    /**
     *
     * @return Whether this ConnController currently has an active connection.
     */
    public synchronized boolean isConnectionActive() {
        return active;
    }

    /**
     *
     * @param active Whether this ConController currently has an active connection.
     */
    private synchronized void setConnectionActive(boolean active) {
        this.active = active;
        notifyAll();
    }

    /**
     *
     * @return True if this ConController currently has a validated connection (a connection to a server with
     * which the handshake was successful).
     */
    private synchronized boolean isConnectionValidated() {
        return validated;
    }

    /**
     *
     * @param validated Whether this ConController currently has an active connection to a server with which the
     *                  handshake was successful.
     */
    private synchronized void setConnectionValidated(boolean validated) {
        logger.log(Level.FINE, String.format("Setting status of connection validation to %b", validated));
        this.validated = validated;
        notifyAll();
    }

    /**
     *
     * @return The Car associated with this ConnController.
     */
    public Car getCar() {
        return car;
    }

    /**
     * Registers a Car to this ConnController so that this controller is informed when the state of the car changes.
     * @param car The Car to for this ConController to observe.
     */
    public void setCar(Car car) {
        logger.log(Level.FINE, "Registering a Car object to ConnController");
        this.car = car;
        car.addSteerChangeHandler(event -> outputController.steer(event.getNewAngle()));
        car.addThrottleChangeHandler(event -> outputController.throttle(event.getNewDirection()));
    }

    /**
     * Connects to the server at the given address and port and validates whether it is the intended type of server.
     * @param ipAddress The IP address of the server to connect to.
     * @param port The port of the server to connect to.
     * @throws NetworkConnectionException If something went wrong while trying to connect to the server or the
     * validation failed.
     */
    public synchronized void connect(String ipAddress, String port) throws NetworkConnectionException {
        logger.log(Level.INFO, String.format("Trying to connect to server at %s:%s", ipAddress, port));
        try {
            InetAddress serverAddress = InetAddress.getByName(ipAddress);
            SocketAddress socketAddress = new InetSocketAddress(serverAddress, Integer.parseInt(port));
            socket = new Socket();
            socket.connect(socketAddress);
            logger.log(Level.INFO, String.format("Successfully connected to server at %s:%s", ipAddress, port));

            initialiseIOControllers();
            setConnectionActive(true);

            logger.log(Level.FINE, "Trying to shake hands with server");
            sendHandshake();

            // Wait until connection with server is validated through a handshake; disconnect and throw error if it
            // takes too long
            while (!isConnectionValidated()) {
                try {
                    logger.log(Level.INFO, "Waiting for server to validate that it is an RTP Car Server");
                    wait(HANDSHAKE_TIMEOUT);

                    if (!isConnectionValidated()) {
                        logger.log(Level.WARNING, "Timeout while waiting for server-part of handshake; disconnecting.");
                        disconnect();
                        throw new NetworkConnectionException("Timeout while waiting for handshake");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketTimeoutException e) {
            throw new NetworkConnectionException(String.format("Timeout while trying to connect to %s:%s", ipAddress, port));
        } catch (UnknownHostException e) {
            throw new NetworkConnectionException("Exception while trying to get host", e);
        } catch (SocketException e) {
            throw new NetworkConnectionException("Exception while trying to set the socket timeout duration", e);
        } catch (IOException e) {
            throw new NetworkConnectionException("Exception while trying to set up a socket connection and streams", e);
        }
    }

    /**
     * Initialises the controllers handling the in- and output streams from/to the car.
     * @throws IOException If something went wrong while trying to set up the in- and output controllers.
     */
    private void initialiseIOControllers() throws IOException {
        logger.log(Level.FINE, "Initialising input and output stream controllers");
        inputController = new ConnInputController(this,
                new BufferedReader(new InputStreamReader(socket.getInputStream())));
        outputController = new ConnOutputController(this,
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
    }

    /**
     * Sets the Car to an active state and starts the heartbeat service.
     */
    synchronized void validateConnection() {
        logger.log(Level.INFO, "Server successfully validated itself");
        setConnectionValidated(true);
        car.reset();
        startHeartbeat();
    }

    /**
     * Deactivates the input handler, output handler and socket connection and sends a ConnectionLossEvent to the
     * respective listeners.
     */
    synchronized void lostConnection() {
        logger.log(Level.INFO, "Lost connection to the server");
        if (isConnectionActive()) {
            ConnectionLossEvent event = new ConnectionLossEvent(this);
            for (ConnectionLossEvent.Handler listener : connectionLossListeners) {
                listener.handle(event);
            }
            disconnect();
        }
    }

    /**
     * Sets the Car to an inactive state, stops the heartbeat service and closes the socket connection if not yet closed.
     */
    public void disconnect() {
        logger.log(Level.INFO, "Handling server disconnect");
        if (isConnectionActive()) {
            setConnectionActive(false);

            if (!socket.isClosed()) {
                try {
                    // Close the output and input controller (and associated streams), which will also close the socket
                    outputController.close();
                    inputController.close();
                } catch (IOException ex) {
                    // TODO: Implement edu.ntnu.rtpcarcontroller.exception handling for ConnController disconnect mechanism.
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Sends a handshake message to the server to establish whether a connection with the right type of server was set
     * up.
     */
    private void sendHandshake() throws NetworkConnectionException {
        logger.log(Level.FINE, "Sending handshake to server");
        outputController.handshake();
    }

    /**
     * Starts the heartbeat-part of the protocol (timed messages are sent to the RC Car).
     */
    private void startHeartbeat() {
        logger.log(Level.INFO, "Starting heartbeat-part of client–server protocol");
        new Thread(outputController).start();
    }

    /**
     * Adds a handler to use when this client has lost the connection to the server, creating a ConnectionLossEvent.
     * @param listener The handler to add.
     */
    public void addConnectionLossHandler(ConnectionLossEvent.Handler listener) {
        connectionLossListeners.add(listener);
    }
}
