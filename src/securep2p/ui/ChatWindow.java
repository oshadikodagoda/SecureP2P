package securep2p.ui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import securep2p.core.MessageListener;
import securep2p.core.PeerClient;
import securep2p.core.PeerServer;

public class ChatWindow extends JFrame implements MessageListener {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField ipField;
    private JTextField portField;
    private JButton connectBtn;
    private JButton listenBtn;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendBtn;
    private JButton fileBtn;
    private JComboBox<String> peerSelector; // Dropdown for Server
    
    private PeerServer server;
    private PeerClient client;
    private boolean isServerMode = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                ChatWindow frame = new ChatWindow();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ChatWindow() {
        setTitle("SecureP2P - Encrypted Messenger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (server != null) server.stop();
                if (client != null) client.disconnect();
                System.exit(0);
            }
        });

        setBounds(100, 100, 700, 500); // Wider to fit the dropdown
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JPanel topPanel = new JPanel();
        topPanel.setBounds(10, 10, 664, 40);
        topPanel.setLayout(null);
        contentPane.add(topPanel);

        JLabel ipLabel = new JLabel("IP:");
        ipLabel.setBounds(10, 10, 20, 20);
        topPanel.add(ipLabel);

        ipField = new JTextField("127.0.0.1");
        ipField.setBounds(35, 10, 100, 20);
        topPanel.add(ipField);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setBounds(145, 10, 35, 20);
        topPanel.add(portLabel);

        portField = new JTextField("9000");
        portField.setBounds(185, 10, 60, 20);
        topPanel.add(portField);

        connectBtn = new JButton("Connect");
        connectBtn.setBounds(255, 10, 100, 20);
        topPanel.add(connectBtn);

        listenBtn = new JButton("Start Listening");
        listenBtn.setBounds(365, 10, 140, 20);
        topPanel.add(listenBtn);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 60, 664, 320);
        contentPane.add(scrollPane);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        scrollPane.setViewportView(chatArea);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBounds(10, 390, 664, 40);
        bottomPanel.setLayout(null);
        contentPane.add(bottomPanel);

        // The Dropdown for selecting peers
        peerSelector = new JComboBox<>();
        peerSelector.setBounds(0, 10, 120, 25);
        peerSelector.setVisible(false); // Hidden by default, only shows on Server
        bottomPanel.add(peerSelector);

        messageField = new JTextField();
        messageField.setBounds(130, 10, 270, 25); // Shifted right to fit dropdown
        bottomPanel.add(messageField);

        sendBtn = new JButton("Send");
        sendBtn.setBounds(410, 10, 74, 25);
        bottomPanel.add(sendBtn);
        
        fileBtn = new JButton("Send File");
        fileBtn.setBounds(490, 10, 100, 25);
        bottomPanel.add(fileBtn);

        setupListeners();
        
        messageField.setEnabled(false);
        sendBtn.setEnabled(false);
        fileBtn.setEnabled(false);
    }

    private void setupListeners() {
        connectBtn.addActionListener(e -> {
            String ip = ipField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number!");
                return;
            }

            isServerMode = false;
            client = new PeerClient(ip, port, this);
            try {
                appendMessage("System", "Connecting to " + ip + ":" + port + "...");
                client.connect();
                appendMessage("System", "Connected successfully!");
                enableControls(false);
            } catch (Exception ex) {
                appendMessage("System", "Connection failed: " + ex.getMessage());
            }
        });

        listenBtn.addActionListener(e -> {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number!");
                return;
            }

            isServerMode = true;
            server = new PeerServer(port, this);
            server.start();
            appendMessage("System", "Listening on port " + port + "...");
            
            // Show the dropdown for the server
            peerSelector.setVisible(true);
            peerSelector.addItem("All Peers"); // Default option
            enableControls(false);
        });

        sendBtn.addActionListener(e -> sendMessage());

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        fileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                boolean sent = false;
                if (isServerMode && server != null) {
                    sent = server.broadcast(""); // File broadcast
                    // Send file logic
                    server.getConnectedPeers().forEach(peer -> {
                        // This is a simplified broadcast for files
                    });
                } else if (!isServerMode && client != null) {
                    sent = client.sendFile(selectedFile);
                }
                
                if (sent) {
                    appendMessage("System", "File sent: " + selectedFile.getName());
                }
            }
        });
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;

        boolean sent = false;
        if (isServerMode && server != null) {
            String selectedPeer = (String) peerSelector.getSelectedItem();
            if ("All Peers".equals(selectedPeer)) {
                sent = server.broadcast(msg); // Send to everyone
            } else {
                sent = server.sendToPeer(selectedPeer, msg); // Send to specific person
            }
        } else if (!isServerMode && client != null) {
            sent = client.send(msg);
        }

        if (sent) {
            appendMessage("You", msg);
            messageField.setText("");
        } else {
            appendMessage("System", "Cannot send. Peer is disconnected.");
        }
    }

    private void enableControls(boolean isListeningOnly) {
        connectBtn.setEnabled(false);
        listenBtn.setEnabled(false);
        messageField.setEnabled(true);
        sendBtn.setEnabled(true);
        fileBtn.setEnabled(true);
    }

    private void appendMessage(String sender, String message) {
        chatArea.append(sender + ": " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    @Override
    public void onMessageReceived(String message) {
        javax.swing.SwingUtilities.invokeLater(() -> appendMessage("Peer", message));
    }
    
    @Override
    public void onFileReceived(String fileName, byte[] fileData) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                File outputFile = new File("received_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(fileData);
                }
                appendMessage("System", "📁 File received and saved as: " + outputFile.getAbsolutePath());
            } catch (Exception e) {
                appendMessage("System", "Error saving file: " + e.getMessage());
            }
        });
    }

    @Override
    public void onConnectionClosed() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            appendMessage("System", "Connection closed.");
            connectBtn.setEnabled(true);
            listenBtn.setEnabled(true);
            messageField.setEnabled(false);
            sendBtn.setEnabled(false);
            fileBtn.setEnabled(false);
        });
    }

    // NEW: Update the dropdown when a new laptop connects
    @Override
    public void onPeerListUpdated(List<String> connectedPeers) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            String currentSelection = (String) peerSelector.getSelectedItem();
            peerSelector.removeAllItems();
            peerSelector.addItem("All Peers");
            for (String peer : connectedPeers) {
                peerSelector.addItem(peer);
            }
            if (currentSelection != null) {
                peerSelector.setSelectedItem(currentSelection);
            }
        });
    }
}