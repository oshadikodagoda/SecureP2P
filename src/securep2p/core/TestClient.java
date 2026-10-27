package securep2p.core;

import java.util.Scanner;

public class TestClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 9000;

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                System.out.println("[CLIENT] Received: " + message);
            }

            @Override
            public void onConnectionClosed() {
                System.out.println("[CLIENT] Connection closed.");
            }
        };

        PeerClient client = new PeerClient(host, port, listener);

        try {
            client.connect();
        } catch (Exception e) {
            System.err.println("[CLIENT] Could not connect: " + e.getMessage());
            return;
        }

        System.out.println("Type messages and press Enter to send (type 'exit' to quit):");

        Thread senderThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if ("exit".equalsIgnoreCase(line)) {
                    client.disconnect();
                    System.exit(0);
                }
                if (client.send(line)) {
                    System.out.println("[CLIENT] Sent: " + line);
                } else {
                    System.out.println("[CLIENT] Not connected.");
                }
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}