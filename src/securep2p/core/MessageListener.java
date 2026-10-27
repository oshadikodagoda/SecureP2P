package securep2p.core;

/**
 * Callback interface for receiving messages from a peer.
 */
public interface MessageListener {

    /**
     * Called when a text message is received from the remote peer.
     *
     * @param message the received message (plaintext for now)
     */
    void onMessageReceived(String message);

    /**
     * Called when the connection to the remote peer is closed.
     */
    void onConnectionClosed();
    
 // In MessageListener.java
    void onFileReceived(String fileName, byte[] fileData);
}