package securep2p.core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerServer {

    private final int port;
    private final MessageListener listener;
    private ServerSocket serverSocket;
    
    // We use a Map to store IP:Port as the key, and the ConnectionHandler as the value
    private Map<String, ConnectionHandler> connections = new ConcurrentHashMap<>();
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
                
                // Get the IP and Port of the connected client to use as a name
                String clientId = socket.getRemoteSocketAddress().toString().replace("/", "");
                System.out.println("[PeerServer] Incoming connection from " + clientId);

                ConnectionHandler newConnection = new ConnectionHandler(socket, listener, true);
                connections.put(clientId, newConnection);
                newConnection.start();
                
                // Notify the UI that a new peer joined so it can update the dropdown
                if (listener != null) {
                    listener.onPeerListUpdated(getConnectedPeers());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[PeerServer] Error: " + e.getMessage());
            }
        }
    }

    // Returns a list of connected peer IDs
    public List<String> getConnectedPeers() {
        return new ArrayList<>(connections.keySet());
    }

    // Send to a SPECIFIC peer
    public boolean sendToPeer(String targetPeerId, String message) {
        ConnectionHandler conn = connections.get(targetPeerId);
        if (conn != null && conn.isRunning()) {
            conn.send(message);
            return true;
        }
        return false;
    }

    // Send to ALL peers (Broadcast)
    public boolean broadcast(String message) {
        boolean sent = false;
        for (ConnectionHandler conn : connections.values()) {
            if (conn.isRunning()) {
                conn.send(message);
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
        for (ConnectionHandler conn : connections.values()) {
            conn.close();
        }
    }
}