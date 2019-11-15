package edu.ntnu.rtpcarcontroller.model;

import edu.ntnu.rtpcarcontroller.event.SteerChangeEvent;
import edu.ntnu.rtpcarcontroller.event.ThrottleChangeEvent;
import edu.ntnu.rtpcarcontroller.util.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Car {
    private static final Logger logger = Logger.getLogger(Car.class.getName());
    private final List<SteerChangeEvent.Handler> steerChangeHandlers;
    private final List<ThrottleChangeEvent.Handler> throttleChangeHandlers;
    private Throttle throttle;
    private int steer;

    /**
     * Creates a new Car.
     */
    public Car() {
        logger.log(Level.FINE, "Initialising new Car");
        steerChangeHandlers = new ArrayList<>();
        throttleChangeHandlers = new ArrayList<>();
        throttle = Throttle.NEUTRAL;
        steer = 90;
    }

    /**
     *
     * @return The current Throttle of this Car.
     */
    public Throttle getThrottle() {
        return throttle;
    }

    /**
     * Throttles the car in the given direction, or neutralises throttle.
     * @param dir The direction to throttle in (Throttle.NEUTRAL, Throttle.FORWARD, or Throttle.NEUTRAL).
     */
    public void throttle(Throttle dir) {
        if (!throttle.equals(dir)) {
            throttle = dir;
            ThrottleChangeEvent event = new ThrottleChangeEvent(this, dir);
            for (ThrottleChangeEvent.Handler handler : throttleChangeHandlers) {
                handler.handle(event);
            }
        }
    }

    /**
     *
     * @return The current steering direction angle (in degrees) of this Car.
     */
    public int getSteer() {
        return steer;
    }

    /**
     * Steers the car into the given direction.
     * @param angle The direction to steer towards, given as an angle in degrees.
     */
    private void steer(int angle) {
        if (steer != angle) {
            steer = angle;
            SteerChangeEvent event = new SteerChangeEvent(this, angle);
            for (SteerChangeEvent.Handler handler : steerChangeHandlers) {
                handler.handle(event);
            }
        }
    }

    /**
     * Steers the car into the given direction.
     * @param dir The direction to steer towards, being Steer.NEUTRAL, Steer.LEFT, or Steer.RIGHT.
     */
    public void steer(Steer dir) {
        steer(Protocol.getDegreesFromSteer(dir));
    }

    /**
     * Resets both the throttle and steering direction of the car to the neutral state.
     */
    public void reset() {
        throttle(Throttle.NEUTRAL);
        steer(Steer.NEUTRAL);
    }

    /**
     * Adds a handler to use for when this Car changes steering direction.
     * @param handler The handler to add.
     */
    public void addSteerChangeHandler(SteerChangeEvent.Handler handler) {
        steerChangeHandlers.add(handler);
    }

    /**
     * Adds a handler to use for when this Car changes throttle direction.
     * @param handler The handler to add.
     */
    public void addThrottleChangeHandler(ThrottleChangeEvent.Handler handler) {
        throttleChangeHandlers.add(handler);
    }
}
