package securep2p.core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PeerServer {

    private final int port;
    private final MessageListener listener;
    private ServerSocket serverSocket;
    private List<ConnectionHandler> connections = new ArrayList<>();
    private volatile boolean running = false;

    public PeerServer(int port, MessageListener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void start() {
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "PeerServerAccept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[PeerServer] Listening on port " + port);
            while (running) {
                Socket socket = serverSocket.accept();
                System.out.println("[PeerServer] Incoming connection from " + socket.getRemoteSocketAddress());

                // Add new connection to the list
                ConnectionHandler newConnection = new ConnectionHandler(socket, listener, true);
                connections.add(newConnection);
                newConnection.start();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[PeerServer] Error: " + e.getMessage());
            }
        }
    }

    // Broadcasts message to all connected clients
    public boolean send(String message) {
        boolean sent = false;
        for (ConnectionHandler conn : connections) {
            if (conn.isRunning()) {
                conn.send(message);
                sent = true;
            }
        }
        return sent;
    }

    // Broadcasts file to all connected clients
    public boolean sendFile(File file) {
        boolean sent = false;
        for (ConnectionHandler conn : connections) {
            if (conn.isRunning()) {
                conn.sendFile(file);
                sent = true;
            }
        }
        return sent;
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[PeerServer] Stop error: " + e.getMessage());
        }
        for (ConnectionHandler conn : connections) {
            conn.close();
        }
    }
}