package edu.ntnu.rtpcarcontroller.exception;

public class NetworkConnectionException extends Exception {
    public NetworkConnectionException(String s) {
        super(s);
    }

    public NetworkConnectionException(String s, Throwable e) {
        super(s, e);
    }
}
