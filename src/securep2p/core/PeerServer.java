package securep2p.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens on a port and accepts incoming peer connections.
 */
public class PeerServer {

    private final int port;
    private final MessageListener listener;
    private ServerSocket serverSocket;
    private ConnectionHandler currentConnection;
    private volatile boolean running = false;

    public PeerServer(int port, MessageListener listener) {
        this.port = port;
        this.listener = listener;
    }

    /**
     * Starts listening in a background thread.
     */
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

                // For this step we allow only one active connection.
                if (currentConnection != null) {
                    currentConnection.close();
                }
                currentConnection = new ConnectionHandler(socket, listener, true);
                currentConnection.start();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[PeerServer] Error: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a message to the currently connected peer.
     * Returns false if there is no active connection.
     */
    public boolean send(String message) {
        if (currentConnection != null && currentConnection.isRunning()) {
            currentConnection.send(message);
            return true;
        }
        return false;
    }

    public boolean hasActiveConnection() {
        return currentConnection != null && currentConnection.isRunning();
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
        if (currentConnection != null) {
            currentConnection.close();
        }
    }
    
    public boolean sendFile(java.io.File file) {
        if (currentConnection != null && currentConnection.isRunning()) {
            currentConnection.sendFile(file);
            return true;
        }
        return false;
    }
}