package edu.ntnu.rtpcarcontroller;

import edu.ntnu.rtpcarcontroller.util.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TestServer {
    public static void main(String... args) throws IOException {
        final int portNumber = 65432;
        System.out.println("Creating server socket on port " + portNumber);
        ServerSocket serverSocket = new ServerSocket(portNumber);

        while (true) {
            Socket socket = serverSocket.accept();
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os, true);

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            boolean stop = false;
            boolean handShaken = false;
            socket.setSoTimeout(1100);
            String str;
            while (!stop) {
                try {
                    str = br.readLine();
                    if (str != null) {
                        System.out.println("Received " + str);
                        if (!handShaken) {
                            if (str.equals(Protocol.HANDSHAKE)) {
                                handShaken = true;
                                System.out.println("Sending handshake command");
                            } else {
                                System.out.println("Received invalid command before handshake. Closing connection.");
                                pw.println(Protocol.CLOSE_CONNECTION);
                                break;
                            }
                        }
                        pw.println(str);
                    } else {
                        System.out.println("Connection to client lost");
                        stop = true;
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Connection to client lost");
                    stop = true;
                }
            }
            pw.close();
            socket.close();
        }
    }
}
