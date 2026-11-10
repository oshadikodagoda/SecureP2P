package securep2p.core;

import securep2p.crypto.CryptoEngine;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import javax.crypto.SecretKey;

public class ConnectionHandler {

    private final Socket socket;
    private final DataInputStream dataIn;
    private final DataOutputStream dataOut;
    private final MessageListener listener;
    private final boolean isServer;
    private volatile boolean running = true;

    private SecretKey aesKey;

    public ConnectionHandler(Socket socket, MessageListener listener, boolean isServer) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.isServer = isServer;
        
        this.dataIn = new DataInputStream(socket.getInputStream());
        this.dataOut = new DataOutputStream(socket.getOutputStream());
        
        try {
            performKeyExchange();
        } catch (Exception e) {
            System.err.println("[Crypto] Key exchange failed: " + e.getMessage());
            close();
        }
    }

    private void performKeyExchange() throws Exception {
        KeyPair rsaKeyPair = CryptoEngine.generateRSAKeyPair();

        if (isServer) {
            dataOut.writeUTF(CryptoEngine.publicKeyToString(rsaKeyPair.getPublic()));
            dataOut.flush();

            String encryptedAesKey = dataIn.readUTF();
            String aesKeyString = CryptoEngine.decryptRSA(encryptedAesKey, rsaKeyPair.getPrivate());
            this.aesKey = CryptoEngine.stringToAesKey(aesKeyString);
            System.out.println("[Crypto] Server successfully received and decrypted AES key.");
        } else {
            String serverPublicKeyStr = dataIn.readUTF();
            PublicKey serverPublicKey = CryptoEngine.stringToPublicKey(serverPublicKeyStr);

            this.aesKey = CryptoEngine.generateAESKey();
            String aesKeyString = CryptoEngine.aesKeyToString(this.aesKey);
            String encryptedAesKey = CryptoEngine.encryptRSA(aesKeyString, serverPublicKey);
            
            dataOut.writeUTF(encryptedAesKey);
            dataOut.flush();
            System.out.println("[Crypto] Client successfully generated and sent AES key.");
        }
    }

    public void start() {
        Thread readerThread = new Thread(this::readLoop, "ConnectionReader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try {
            while (running) {
                String header = dataIn.readUTF();
                
                if (header.equals("[FILE]")) {
                    String fileName = dataIn.readUTF();
                    int length = dataIn.readInt();
                    byte[] encryptedBytes = new byte[length];
                    dataIn.readFully(encryptedBytes);
                    
                    // PROOF OF ENCRYPTION: Print the incoming encrypted file size
                    System.out.println("[NETWORK IN] Received encrypted file data (" + length + " bytes) for: " + fileName);
                    
                    byte[] decryptedBytes = CryptoEngine.decryptAESBytes(encryptedBytes, aesKey);
                    
                    if (listener != null) {
                        listener.onFileReceived(fileName, decryptedBytes);
                    }
                    
                } else if (header.equals("[MSG]")) {
                    String encryptedMessage = dataIn.readUTF();
                    
                    // PROOF OF ENCRYPTION: Print the incoming encrypted text!
                    System.out.println("[NETWORK IN] Received Encrypted text: " + encryptedMessage);
                    
                    String decryptedMessage = CryptoEngine.decryptAES(encryptedMessage, aesKey);
                    if (listener != null) {
                        listener.onMessageReceived(decryptedMessage);
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("[ConnectionHandler] Peer disconnected.");
        } catch (Exception e) {
            System.err.println("[ConnectionHandler] Read error: " + e.getMessage());
        } finally {
            close();
            if (listener != null) {
                listener.onConnectionClosed();
            }
        }
    }

    public void send(String message) {
        if (running && dataOut != null && aesKey != null) {
            try {
                String encryptedMessage = CryptoEngine.encryptAES(message, aesKey);
                
                // PROOF OF ENCRYPTION: Print the outgoing encrypted text!
                System.out.println("[NETWORK OUT] Sending Encrypted text: " + encryptedMessage);
                
                dataOut.writeUTF("[MSG]");
                dataOut.writeUTF(encryptedMessage);
                dataOut.flush();
            } catch (Exception e) {
                System.err.println("[Crypto] Failed to encrypt message: " + e.getMessage());
            }
        }
    }

    public void sendFile(File file) {
        if (running && dataOut != null && aesKey != null) {
            try {
                byte[] fileBytes = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(fileBytes);
                }

                byte[] encryptedBytes = CryptoEngine.encryptAESBytes(fileBytes, aesKey);

                // PROOF OF ENCRYPTION: Print the outgoing encrypted file size
                System.out.println("[NETWORK OUT] Sending encrypted file data (" + encryptedBytes.length + " bytes) for: " + file.getName());

                dataOut.writeUTF("[FILE]");
                dataOut.writeUTF(file.getName());
                dataOut.writeInt(encryptedBytes.length);
                dataOut.write(encryptedBytes);
                dataOut.flush();
                
            } catch (Exception e) {
                System.err.println("[File] Failed to send file: " + e.getMessage());
            }
        }
    }

    public void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[ConnectionHandler] Close error: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }
}