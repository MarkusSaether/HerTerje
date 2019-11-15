package edu.ntnu.rtpcarcontroller.controller.connection;

import edu.ntnu.rtpcarcontroller.util.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller used by ConnController that acts as the first entry point for all input from the remote server.
 */
class ConnInputController extends Thread {
    private static final Logger logger = Logger.getLogger(ConnInputController.class.getName());

    private final ConnController connection;
    private final BufferedReader reader;
    private boolean active;

    /**
     * Creates a new ConnInputController that controls the input for the given ConController using the given reader.
     * @param connection The main controller for which this ConnInputController handles the input.
     * @param reader The reader this controller should handle input with.
     */
    ConnInputController(ConnController connection, BufferedReader reader) {
        setName("Connection input controller");
        this.connection = connection;
        this.reader = reader;
        active = true;
        start();
    }

    /**
     * Parses an input string and performs the associated functions.
     * @param input The input string to parse.
     */
    private void parseInput(String input) {
        if (input == null || input.split(" ").length > 2) {
            return;
        }

        switch (input) {
            case Protocol.HANDSHAKE:
                connection.validateConnection();
                break;
            case Protocol.CLOSE_CONNECTION:
                active = false;
                connection.disconnect();
                break;
            default:
                break;
        }
    }

    /**
     * Closes this ConnInputController's associated reader (and thus input stream).
     */
    void close() {
        logger.log(Level.FINE, "Closing input stream reader");
        try {
            reader.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        String input;
        while (active) {
            try {
                input = reader.readLine();
                parseInput(input);
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                connection.lostConnection();
                break;
            }
        }
    }
}
