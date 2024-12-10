package miner;


/*
 * XMR Coin CPU Miner - v1
 * Purpose of this project is to learning java with fun.
 * I used JFrame for building GUI Miner.
 * Behind the scene, I build XMRig from source for MacOS and make it executable.
 * */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class MoneroMiner extends JFrame{

    // Declare UI components for the user details
    private JTextField urlField;
    private JTextField walletField;
    private JTextField passwordField;
    private JComboBox<String> securityTypeBox;
    private JButton startButton;
    private JButton stopButton;

    // Declare components for terminal functionality
    private JTextArea terminalOutput;
    private JScrollPane terminalScrollPane;

    // Process variable to store the mining process
    private Process miningProcess;

    // Path to the xmrig executable after extracting from resources
    private File xmrigExecutable;

    // Regular expressions to match timestamps and color codes
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*?\\]");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");


    public MoneroMiner() {
        // Set frame title
        setTitle("Monero Miner");

        // Initialize the user details components
        urlField = new JTextField(15);
        walletField = new JTextField(15);  // New field for wallet address
        passwordField = new JPasswordField(15);
        String[] securityOptions = { "SSL", "Non-SSL" };
        securityTypeBox = new JComboBox<>(securityOptions);
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");

        // Create the user details panel using GridLayout (2 columns: label + field)
        JPanel userDetailsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        userDetailsPanel.setBorder(BorderFactory.createTitledBorder("User Details"));

        // Add all fields with their labels
        userDetailsPanel.add(new JLabel("URL (with Port):"));
        userDetailsPanel.add(urlField);
        userDetailsPanel.add(new JLabel("Wallet Address:"));
        userDetailsPanel.add(walletField);  // New wallet field
        userDetailsPanel.add(new JLabel("Password:"));
        userDetailsPanel.add(passwordField);
        userDetailsPanel.add(new JLabel("Security Type:"));
        userDetailsPanel.add(securityTypeBox);

        // Create a panel for the buttons (Start and Stop) and center them
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        // Add action listeners to the buttons
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startMining();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopMining();
            }
        });

        // Initialize terminal output area (text area)
        terminalOutput = new JTextArea(10, 30);
        terminalOutput.setEditable(false); // Make it read-only
        terminalOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));
        terminalScrollPane = new JScrollPane(terminalOutput);
        terminalScrollPane.setBorder(BorderFactory.createTitledBorder("Terminal"));

        // Set up the split pane to divide user details and terminal
        JPanel userDetailsWithButtonPanel = new JPanel(new BorderLayout());
        userDetailsWithButtonPanel.add(userDetailsPanel, BorderLayout.CENTER);
        userDetailsWithButtonPanel.add(buttonPanel, BorderLayout.SOUTH); // Add buttons below the form

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, userDetailsWithButtonPanel, terminalScrollPane);
        splitPane.setResizeWeight(0.5);  // Allocate equal space to both panels

        // Add split pane to the frame
        add(splitPane);

        // Set frame properties
        setSize(600, 600);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);

        // Extract xmrig from resources and make it executable
        try {
            xmrigExecutable = extractXmrigFromResources();
        } catch (IOException e) {
            appendToTerminal("Error extracting xmrig: " + e.getMessage());
        }
    }

    // Method to extract the xmrig executable from the resources folder
    private File extractXmrigFromResources() throws IOException {
        InputStream xmrigStream = getClass().getResourceAsStream("/xmrig");
        if (xmrigStream == null) {
            throw new FileNotFoundException("xmrig executable not found in resources");
        }

        File tempXmrig = File.createTempFile("xmrig", null);
        tempXmrig.deleteOnExit(); // Ensure the file is deleted when the program exits

        Files.copy(xmrigStream, tempXmrig.toPath(), StandardCopyOption.REPLACE_EXISTING);

        tempXmrig.setExecutable(true);

        return tempXmrig;
    }

    // Method to handle "Start" button click
    private void startMining() {
        if (miningProcess == null) {
            String url = urlField.getText();   // URL field includes port
            String walletAddress = walletField.getText();  // Wallet address from new field
            String password = passwordField.getText();
            String securityType = (String) securityTypeBox.getSelectedItem();

            // Building the command for mining with the extracted xmrig executable
            String miningCommand = String.format("%s -o %s -u %s -k -p %s", xmrigExecutable.getAbsolutePath(), url, walletAddress, "x");

            try {
                // Initialize the ProcessBuilder to run the mining command
                ProcessBuilder processBuilder = new ProcessBuilder(miningCommand.split(" "));
                processBuilder.redirectErrorStream(true); // Redirect error stream to output
                miningProcess = processBuilder.start();

                // Create a new thread to read the process output and display it in the terminal
                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(miningProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                        	String cleanedLine = cleanLog(line);
                            terminalOutput.append(cleanedLine + "\n"); // Print each line on a new line
                            terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength()); // Auto-scroll
                        }
                    } catch (IOException e) {
                        appendToTerminal("Error reading mining output: " + e.getMessage());
                    }
                }).start();

                appendToTerminal("Mining process started...\n");
            } catch (IOException e) {
                appendToTerminal("Error starting mining process: " + e.getMessage());
            }
        } else {
            appendToTerminal("Mining process is already running.\n");
        }
    }

    // Method to clean the log by removing timestamp and color codes
    private static String cleanLog(String logLine) {
        // Remove timestamp and color codes
        String cleanedLine = TIMESTAMP_PATTERN.matcher(logLine).replaceAll("");
        cleanedLine = COLOR_CODE_PATTERN.matcher(cleanedLine).replaceAll("");
        return cleanedLine.trim(); // Trim to remove any leading/trailing spaces
    }

    // Method to handle "Stop" button click
    private void stopMining() {
        if (miningProcess != null) {
            miningProcess.destroy(); // Kill the process
            appendToTerminal("Mining process stopped.\n");
            miningProcess = null; // Reset process
        } else {
            appendToTerminal("No mining process to stop.\n");
        }
    }

    // Utility method to append text to the terminal
    private void appendToTerminal(String text) {
        terminalOutput.append(text + "\n");
        terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength()); // Auto scroll to bottom
    }

    public static void main(String[] args) {
        // Create the frame instance
        new MoneroMiner();
    }
}
