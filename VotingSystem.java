import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class Block {
    String voterId, vote, previousHash, hash;

    public Block(String voterId, String vote, String previousHash) {
        this.voterId = voterId;
        this.vote = vote;
        this.previousHash = previousHash;
        this.hash = calculateHash();
    }

    public String calculateHash() {
        try {
            String data = voterId + vote + previousHash;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes("UTF-8"));
            StringBuilder buffer = new StringBuilder();
            for (byte b : hashBytes)
                buffer.append(String.format("%02x", b));
            return buffer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class Blockchain {
    ArrayList<Block> chain = new ArrayList<>();

    public Blockchain() {
        chain.add(new Block("0", "Genesis", "0"));
    }

    public void addBlock(String voterId, String vote) {
        Block previousBlock = chain.get(chain.size() - 1);
        Block newBlock = new Block(voterId, vote, previousBlock.hash);
        chain.add(newBlock);
    }

    public ArrayList<Block> getChain() {
        return chain;
    }

    public int getVotesFor(String candidate) {
        int count = 0;
        for (Block block : chain) {
            if (block.vote.equals(candidate)) {
                count++;
            }
        }
        return count - (candidate.equals("Genesis") ? 1 : 0);
    }
}

public class VotingSystem extends JFrame {
    private Blockchain blockchain = new Blockchain();
    private HashSet<String> votedIds = new HashSet<>();
    private JTextField voterIdField;
    private JComboBox<String> voteDropdown;
    private JTextArea blockchainArea, resultArea;
    private JButton endElectionButton, voteButton;
    private boolean electionEnded = false;
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SECONDARY_COLOR = new Color(245, 245, 245);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final int PADDING = 20;

    public VotingSystem() {
        setTitle("Blockchain Voting System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout(PADDING, PADDING));
        getContentPane().setBackground(SECONDARY_COLOR);
        
        // Main title
        JLabel titleLabel = new JLabel("Blockchain Voting System", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, PADDING, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBackground(SECONDARY_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, PADDING, PADDING, PADDING));

        // Input Panel with modern styling
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Output Panel with modern styling
        JPanel outputPanel = createOutputPanel();
        mainPanel.add(outputPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setVisible(true);
        updatePollCount();
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(createRoundedBorder("Cast Your Vote"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Voter ID
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel voterIdLabel = new JLabel("Voter ID (exactly 12 digits):");
        voterIdLabel.setFont(REGULAR_FONT);
        inputPanel.add(voterIdLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        voterIdField = new JTextField();
        voterIdField.setFont(REGULAR_FONT);
        styleTextField(voterIdField);
        inputPanel.add(voterIdField, gbc);

        // Candidate Selection
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel candidateLabel = new JLabel("Select Candidate:");
        candidateLabel.setFont(REGULAR_FONT);
        inputPanel.add(candidateLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0;
        voteDropdown = new JComboBox<>(new String[]{"Alice", "Bob", "Charlie"});
        voteDropdown.setFont(REGULAR_FONT);
        styleComboBox(voteDropdown);
        inputPanel.add(voteDropdown, gbc);

        // Buttons Panel
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(Color.WHITE);

        voteButton = createStyledButton("Cast Vote", SUCCESS_COLOR);
        JButton showBlockchainButton = createStyledButton("Show Blockchain", PRIMARY_COLOR);
        endElectionButton = createStyledButton("End Election", new Color(211, 47, 47));

        buttonPanel.add(voteButton);
        buttonPanel.add(showBlockchainButton);
        buttonPanel.add(endElectionButton);

        voteButton.addActionListener(this::castVote);
        showBlockchainButton.addActionListener(this::displayBlockchain);
        endElectionButton.addActionListener(this::endElection);

        inputPanel.add(buttonPanel, gbc);

        return inputPanel;
    }

    private JPanel createOutputPanel() {
        JPanel outputPanel = new JPanel(new GridLayout(1, 2, PADDING, 0));
        outputPanel.setBackground(SECONDARY_COLOR);

        // Blockchain Area
        blockchainArea = new JTextArea();
        blockchainArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        blockchainArea.setMargin(new Insets(10, 10, 10, 10));
        blockchainArea.setEditable(false);
        JScrollPane blockchainScroll = new JScrollPane(blockchainArea);
        blockchainScroll.setBorder(createRoundedBorder("Blockchain"));

        // Results Area
        resultArea = new JTextArea();
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultArea.setMargin(new Insets(10, 10, 10, 10));
        resultArea.setEditable(false);
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(createRoundedBorder("Live Vote Count"));

        outputPanel.add(blockchainScroll);
        outputPanel.add(resultScroll);

        return outputPanel;
    }

    private Border createRoundedBorder(String title) {
        Border line = BorderFactory.createLineBorder(PRIMARY_COLOR, 1);
        Border empty = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border compound = BorderFactory.createCompoundBorder(line, empty);
        TitledBorder titled = BorderFactory.createTitledBorder(compound, title);
        titled.setTitleFont(new Font("Segoe UI", Font.BOLD, 16));
        titled.setTitleColor(PRIMARY_COLOR);
        return titled;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(REGULAR_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void styleTextField(JTextField field) {
        field.setFont(REGULAR_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
            field.getBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setBackground(Color.WHITE);
        ((JComponent) comboBox.getRenderer()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    private void castVote(ActionEvent e) {
        if (electionEnded) {
            JOptionPane.showMessageDialog(null, "Election has ended. No more votes can be cast.", "Election Ended", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String voterId = voterIdField.getText().trim();
        String vote = (String) voteDropdown.getSelectedItem();

        // Validate voter ID: exactly 12 digits
        if (!voterId.matches("\\d{12}")) {
            JOptionPane.showMessageDialog(null, "⚠ Voter ID must be exactly 12 digits long.", "Invalid Voter ID", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check for double voting
        if (votedIds.contains(voterId)) {
            JOptionPane.showMessageDialog(null, "⚠ This Voter ID has already cast a vote.", "Double Voting Detected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        blockchain.addBlock(voterId, vote);
        votedIds.add(voterId);
        voterIdField.setText("");
        
        JOptionPane.showMessageDialog(null, 
            "Vote cast successfully!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        
        updatePollCount();
    }

    private void displayBlockchain(ActionEvent e) {
        StringBuilder builder = new StringBuilder();
        for (Block block : blockchain.getChain()) {
            builder.append("Voter ID: ").append(block.voterId).append("\n");
            builder.append("Vote: ").append(block.vote).append("\n");
            builder.append("Previous Hash: ").append(block.previousHash).append("\n");
            builder.append("Hash: ").append(block.hash).append("\n");
            builder.append("---------------------------\n");
        }
        blockchainArea.setText(builder.toString());
    }

    private void updatePollCount() {
        StringBuilder result = new StringBuilder("Live Vote Count:\n\n");
        for (int i = 0; i < voteDropdown.getItemCount(); i++) {
            String candidate = voteDropdown.getItemAt(i);
            int count = blockchain.getVotesFor(candidate);
            result.append(candidate).append(": ").append(count).append(" votes\n");
        }
        resultArea.setText(result.toString());
    }

    private void endElection(ActionEvent e) {
        if (electionEnded) {
            JOptionPane.showMessageDialog(null, "Election has already ended.", "Election Status", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Calculate winner
        String winner = "";
        int maxVotes = -1;
        int totalVotes = 0;
        
        for (int i = 0; i < voteDropdown.getItemCount(); i++) {
            String candidate = voteDropdown.getItemAt(i);
            int votes = blockchain.getVotesFor(candidate);
            totalVotes += votes;
            if (votes > maxVotes) {
                maxVotes = votes;
                winner = candidate;
            }
        }

        // Save results
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "election_results_" + timestamp + ".txt";
            
            FileWriter writer = new FileWriter(filename);
            writer.write("Election Results\n");
            writer.write("----------------\n");
            writer.write("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Total Votes Cast: " + totalVotes + "\n");
            writer.write("Winner: " + winner + " (" + maxVotes + " votes)\n");
            writer.write("\nVote Distribution:\n");
            
            for (int i = 0; i < voteDropdown.getItemCount(); i++) {
                String candidate = voteDropdown.getItemAt(i);
                int votes = blockchain.getVotesFor(candidate);
                writer.write(candidate + ": " + votes + " votes\n");
            }
            
            writer.close();
            
            electionEnded = true;
            JOptionPane.showMessageDialog(null, 
                "Election has ended!\n" +
                "Results have been saved to: " + filename + "\n" +
                "Winner: " + winner + " with " + maxVotes + " votes",
                "Election Results",
                JOptionPane.INFORMATION_MESSAGE);
                
            // Disable voting
            voterIdField.setEnabled(false);
            voteDropdown.setEnabled(false);
            voteButton.setEnabled(false);
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, 
                "Error saving election results: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VotingSystem::new);
    }
}