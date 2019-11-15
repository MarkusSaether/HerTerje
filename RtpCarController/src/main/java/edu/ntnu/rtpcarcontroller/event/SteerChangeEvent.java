package edu.ntnu.rtpcarcontroller.event;

public class SteerChangeEvent {
    private final Object source;
    private final int newAngle;

    public SteerChangeEvent(Object source, int newAngle) {
        this.source = source;
        this.newAngle = newAngle;
    }

    public Object getSource() {
        return source;
    }

    public int getNewAngle() {
        return newAngle;
    }

    public interface Handler {
        void handle(SteerChangeEvent event);
    }
}
