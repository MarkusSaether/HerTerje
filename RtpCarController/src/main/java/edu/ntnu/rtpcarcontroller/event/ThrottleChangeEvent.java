package edu.ntnu.rtpcarcontroller.event;

import edu.ntnu.rtpcarcontroller.model.Throttle;

public class ThrottleChangeEvent {
    private final Object source;
    private final Throttle newDirection;

    public ThrottleChangeEvent(Object source, Throttle newDirection) {
        this.source = source;
        this.newDirection = newDirection;
    }

    public Object getSource() {
        return source;
    }

    public Throttle getNewDirection() {
        return newDirection;
    }

    public interface Handler {
        void handle(ThrottleChangeEvent event);
    }
}
