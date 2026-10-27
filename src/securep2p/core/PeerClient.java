package securep2p.core;

import java.io.IOException;
import java.net.Socket;

/**
 * Connects to a remote peer and manages that outgoing connection.
 */
public class PeerClient {

    private final String host;
    private final int port;
    private final MessageListener listener;
    private ConnectionHandler connection;

    public PeerClient(String host, int port, MessageListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    /**
     * Connects to the remote peer.
     *
     * @throws IOException if the connection fails
     */
    public void connect() throws IOException {
        Socket socket = new Socket(host, port);
        System.out.println("[PeerClient] Connected to " + host + ":" + port);
        connection = new ConnectionHandler(socket, listener, false);
        connection.start();
    }

    /**
     * Sends a message to the remote peer.
     * Returns false if not connected.
     */
    public boolean send(String message) {
        if (connection != null && connection.isRunning()) {
            connection.send(message);
            return true;
        }
        return false;
    }

    public boolean isConnected() {
        return connection != null && connection.isRunning();
    }

    public void disconnect() {
        if (connection != null) {
            connection.close();
        }
    }
    
    public boolean sendFile(java.io.File file) {
        if (connection != null && connection.isRunning()) {
            connection.sendFile(file);
            return true;
        }
        return false;
    }
}
