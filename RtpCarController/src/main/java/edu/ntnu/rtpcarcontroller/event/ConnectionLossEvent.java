package edu.ntnu.rtpcarcontroller.event;

public class ConnectionLossEvent {
    private final Object source;

    public ConnectionLossEvent(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }

    public interface Handler {
        void handle(ConnectionLossEvent event);
    }
}
