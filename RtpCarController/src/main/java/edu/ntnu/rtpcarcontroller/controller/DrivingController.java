package edu.ntnu.rtpcarcontroller.controller;

import edu.ntnu.rtpcarcontroller.model.Car;
import edu.ntnu.rtpcarcontroller.model.Steer;
import edu.ntnu.rtpcarcontroller.model.Throttle;

import java.util.logging.Logger;

/**
 * A controller with the final responsibility of changing the throttle and steering direction of the Car model instance.
 */
public enum DrivingController {
    INSTANCE;
    private static final Logger logger = Logger.getLogger(DrivingController.class.getName());
    private Car car;

    /**
     * Sets the Car that this DrivingController should control.
     * @param car The Car that this DrivingController should control.
     */
    public void setCar(Car car) {
        this.car = car;
    }

    /**
     * Throttles the Car into the given direction.
     * @param dir The direction to throttle the Car in.
     */
    public void throttle(Throttle dir) {
        car.throttle(dir);
    }

    /**
     * Steers the Car into the given direction.
     * @param angle The angle (in degrees) to steer the Car to.
     */
    public void steer(Steer angle) {
        car.steer(angle);
    }
}
