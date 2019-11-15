package edu.ntnu.rtpcarcontroller.controller.connection;

import edu.ntnu.rtpcarcontroller.exception.NetworkConnectionException;
import edu.ntnu.rtpcarcontroller.model.Throttle;
import edu.ntnu.rtpcarcontroller.util.Protocol;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller used by ConnController that acts as the final exit point for all output to the remote server.
 */
class ConnOutputController extends Thread {
    private static final Logger logger = Logger.getLogger(ConnOutputController.class.getName());

    private final ConnController connection;
    private final BufferedWriter writer;
    private Throttle throttle;
    private int steer;
    private boolean stateChanged;
    private boolean heartbeat;

    /**
     * Creates a new ConnOutputController that handles the outgoing data to the car.
     * @param connection The controller handling the general connection to the car.
     * @param writer The output stream writer to the car.
     */
    ConnOutputController(ConnController connection, BufferedWriter writer) {
        this.connection = connection;
        this.writer = writer;
        stateChanged = false;
        heartbeat = false;
        throttle(Throttle.NEUTRAL);
        steer(90);
    }

    /**
     * Sends the given String to the remote car.
     * @param message The String to send to the remote car.
     * @throws NetworkConnectionException If sending the message resulted in an error.
     */
    private void write(String message) throws NetworkConnectionException {
        logger.log(Level.FINER, "Sending message to server: " + message);
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new NetworkConnectionException("Error while trying to write to the output stream", e);
        }
    }

    /**
     * Sends a handshake command to the remote car to establish whether the connection is valid.
     * @throws NetworkConnectionException If sending the command resulted in an error.
     */
    synchronized void handshake() throws NetworkConnectionException {
        logger.log(Level.FINE, "Handshaking server");
        write(Protocol.HANDSHAKE);
    }

    /**
     * Closes this controller's associated OutputStream.
     * @throws IOException If something went wrong while closing the OutputStream.
     */
    void close() throws IOException {
        logger.log(Level.FINE, "Closing output stream writer");
        heartbeat = false;
        writer.close();
    }

    /**
     * Informs the remote car to throttle in the given direction.
     * @param dir The direction to throttle in, either neutral, forward, or backward.
     */
    synchronized void throttle(Throttle dir) {
        throttle = dir;
        stateChanged = true;
        notifyAll();
    }

    /**
     * Informs the remote car to put the wheels at the given angle.
     * @param angle The angle to put the wheels in, which must be between 0 and 180.
     */
    synchronized void steer(int angle) {
        steer = angle;
        stateChanged = true;
        notifyAll();
    }

    /**
     * Sends a message to the remote car with the desired throttle and steering direction states.
     */
    private synchronized void sendStateUpdate() {
        try {
            String message = Protocol.getStateMessage(throttle, steer);
            write(message);
            stateChanged = false;
        } catch (NetworkConnectionException e) {
            connection.lostConnection();
        }
    }

    @Override
    public synchronized void run() {
        heartbeat = true;

        while (heartbeat) {
            // If the state has not changed, wait until change to send update, or until timeout expired
            if (!stateChanged) {
                try {
                    wait(Protocol.HEARTBEAT_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (heartbeat) {
                sendStateUpdate();
            }
        }
    }
}
