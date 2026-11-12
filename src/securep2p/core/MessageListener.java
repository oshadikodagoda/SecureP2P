package securep2p.core;

import java.util.List;

public interface MessageListener {
    void onMessageReceived(String message);
    void onFileReceived(String fileName, byte[] fileData);
    void onConnectionClosed();
    
    // NEW: Tells the UI to update the dropdown when someone joins
    void onPeerListUpdated(List<String> connectedPeers);
}