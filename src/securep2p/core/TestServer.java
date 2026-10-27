package securep2p.core;

import java.util.Scanner;

public class TestServer {

    public static void main(String[] args) {
        int port = 9000;

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                System.out.println("[SERVER] Received: " + message);
            }

            @Override
            public void onConnectionClosed() {
                System.out.println("[SERVER] Connection closed.");
            }
        };

        PeerServer server = new PeerServer(port, listener);
        server.start();

        System.out.println("Type messages and press Enter to send (type 'exit' to quit):");

        // Start a small thread so the server can send typed input to the client.
        Thread senderThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if ("exit".equalsIgnoreCase(line)) {
                    server.stop();
                    System.exit(0);
                }
                if (server.send(line)) {
                    System.out.println("[SERVER] Sent: " + line);
                } else {
                    System.out.println("[SERVER] No active connection.");
                }
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();

        // Keep the main thread alive.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}