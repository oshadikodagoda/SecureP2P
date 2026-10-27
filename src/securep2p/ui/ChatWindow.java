package securep2p.ui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JButton;
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
    private JButton fileBtn; // NEW
    
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
        
        // Safely close connections when window is closed
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (server != null) server.stop();
                if (client != null) client.disconnect();
                System.exit(0);
            }
        });

        setBounds(100, 100, 650, 500);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JPanel topPanel = new JPanel();
        topPanel.setBounds(10, 10, 614, 40);
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
        scrollPane.setBounds(10, 60, 614, 320);
        contentPane.add(scrollPane);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        scrollPane.setViewportView(chatArea);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBounds(10, 390, 614, 40);
        bottomPanel.setLayout(null);
        contentPane.add(bottomPanel);

        messageField = new JTextField();
        messageField.setBounds(0, 10, 400, 25);
        bottomPanel.add(messageField);

        sendBtn = new JButton("Send");
        sendBtn.setBounds(410, 10, 74, 25);
        bottomPanel.add(sendBtn);
        
        // NEW: File Send Button
        fileBtn = new JButton("Send File");
        fileBtn.setBounds(490, 10, 100, 25);
        bottomPanel.add(fileBtn);

        setupListeners();
        
        // Initially disabled until connected
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
        
        // NEW: File chooser logic
        fileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                boolean sent = false;
                if (isServerMode && server != null) {
                    sent = server.sendFile(selectedFile);
                } else if (!isServerMode && client != null) {
                    sent = client.sendFile(selectedFile);
                }
                
                if (sent) {
                    appendMessage("System", "File sent: " + selectedFile.getName());
                } else {
                    appendMessage("System", "Failed to send file.");
                }
            }
        });
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;

        boolean sent = false;
        if (isServerMode && server != null) {
            sent = server.send(msg);
        } else if (!isServerMode && client != null) {
            sent = client.send(msg);
        }

        if (sent) {
            appendMessage("You", msg);
            messageField.setText("");
        } else {
            appendMessage("System", "Cannot send. Not connected to a peer.");
        }
    }

    private void enableControls(boolean isListeningOnly) {
        connectBtn.setEnabled(false);
        listenBtn.setEnabled(false);
        messageField.setEnabled(true);
        sendBtn.setEnabled(true);
        if (!isListeningOnly) {
            fileBtn.setEnabled(true);
        }
    }

    private void appendMessage(String sender, String message) {
        chatArea.append(sender + ": " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    @Override
    public void onMessageReceived(String message) {
        javax.swing.SwingUtilities.invokeLater(() -> appendMessage("Peer", message));
    }
    
    // NEW: Handle received files
    @Override
    public void onFileReceived(String fileName, byte[] fileData) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                // Save file in the project directory with "received_" prefix
                File outputFile = new File("received_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(fileData);
                }
                appendMessage("System", "📁 File received and saved as: " + outputFile.getAbsolutePath());
                enableControls(false); // Enable file button now that connection is active
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
}