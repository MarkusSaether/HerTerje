package edu.ntnu.rtpcarcontroller.util;

import edu.ntnu.rtpcarcontroller.model.Steer;
import edu.ntnu.rtpcarcontroller.model.Throttle;

public class Protocol {
    public static final String HANDSHAKE = "HANDSHAKE";
    public static final String CLOSE_CONNECTION = "CLOSE";
    public static final int HEARTBEAT_PERIOD = 1000;

    /**
     * Returns the steering angle transformed from a direction to degrees.
     * @param dir The steering direction
     * @return The given steering direction transformed into an angle in degrees; 90 degrees (neutral) if given
     * direction is invalid.
     */
    public static int getDegreesFromSteer(Steer dir) {
        switch (dir) {
            case LEFT: return 0;
            case RIGHT: return 180;
            default: return 90;
        }
    }

    /**
     * Returns a command to send to the RC Car using the given key and value.
     * @param key The type of command that must be created.
     * @param value The value that must be passed with the command.
     * @return A formatted string that can be sent to the RC Car as a command.
     */
    private static String formatCommand(String key, String value) {
        return String.format("%s %s", key, value);
    }

    /**
     * Returns a state message with the given throttle direction and steering angle.
     * @param dir The direction to throttle in.
     * @param angle The angle to steer to.
     * @return A formatted string that can be sent to the RC Car as a state update command.
     */
    public static String getStateMessage(Throttle dir, int angle) {
        return String.format("T:%s S:%d", dir.toString(), angle);
    }
}
