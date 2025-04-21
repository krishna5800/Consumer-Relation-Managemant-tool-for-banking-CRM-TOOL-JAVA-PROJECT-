package com.bank.crm.dashboard;

import com.bank.crm.database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CustomerDashboard extends BaseDashboard {
    private JLabel accountNumberLabel;
    private JLabel accountTypeLabel;
    private JLabel balanceLabel;
    private JTextField amountField;
    private JTextField descriptionField;
    private JTable transactionTable;
    private JTable ticketTable;
    private DefaultTableModel ticketModel;

    public CustomerDashboard(int userId, String userName) {
        super(userId, userName);
    }

    @Override
    protected void initializeComponents() {
        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Create main panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Account Overview", createAccountPanel());
        tabbedPane.addTab("Transactions", createTransactionPanel());
        tabbedPane.addTab("Support Tickets", createTicketPanel());
        add(tabbedPane, BorderLayout.CENTER);

        // Refresh account details
        refreshAccountDetails();
    }

    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Account details section
        JPanel detailsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        accountNumberLabel = new JLabel("Account Number: ");
        accountTypeLabel = new JLabel("Account Type: ");
        balanceLabel = new JLabel("Balance: ");
        detailsPanel.add(accountNumberLabel);
        detailsPanel.add(accountTypeLabel);
        detailsPanel.add(balanceLabel);
        panel.add(detailsPanel, BorderLayout.NORTH);

        // Transaction form section
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Amount field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1;
        amountField = new JTextField(20);
        formPanel.add(amountField, gbc);

        // Description field
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        descriptionField = new JTextField(20);
        formPanel.add(descriptionField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton creditButton = new JButton("Credit");
        JButton debitButton = new JButton("Debit");
        JButton transferButton = new JButton("Transfer");

        creditButton.addActionListener(e -> performTransaction("CREDIT"));
        debitButton.addActionListener(e -> performTransaction("DEBIT"));
        transferButton.addActionListener(e -> showTransferDialog());

        buttonPanel.add(creditButton);
        buttonPanel.add(debitButton);
        buttonPanel.add(transferButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }

    private void showTransferDialog() {
        JDialog dialog = new JDialog(this, "Transfer Money", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create input fields
        JTextField accountNumberField = new JTextField(20);
        JTextField amountField = new JTextField(20);
        JTextField descriptionField = new JTextField(20);

        // Add components to dialog
        addDialogComponent(dialog, "Recipient Account Number:", accountNumberField, gbc, 0);
        addDialogComponent(dialog, "Amount:", amountField, gbc, 1);
        addDialogComponent(dialog, "Description:", descriptionField, gbc, 2);

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton transferButton = new JButton("Transfer");
        JButton cancelButton = new JButton("Cancel");

        transferButton.addActionListener(e -> {
            try {
                String recipientAccount = accountNumberField.getText().trim();
                double amount = Double.parseDouble(amountField.getText());
                String description = descriptionField.getText().trim();

                if (recipientAccount.isEmpty() || amount <= 0 || description.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please fill all fields with valid values.");
                    return;
                }

                performTransfer(recipientAccount, amount, description);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid amount.");
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(transferButton);
        buttonPanel.add(cancelButton);

        gbc.gridy = 3;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addDialogComponent(JDialog dialog, String label, JComponent component, 
                                  GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        dialog.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        dialog.add(component, gbc);
    }

    private void performTransfer(String recipientAccount, double amount, String description) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get sender's account details
                String senderQuery = "SELECT id, balance FROM accounts WHERE user_id = ?";
                int senderAccountId;
                double senderBalance;
                try (PreparedStatement pstmt = conn.prepareStatement(senderQuery)) {
                    pstmt.setInt(1, userId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Sender account not found");
                        }
                        senderAccountId = rs.getInt("id");
                        senderBalance = rs.getDouble("balance");
                    }
                }

                // Check if sufficient balance
                if (amount > senderBalance) {
                    throw new SQLException("Insufficient balance");
                }

                // Get recipient's account details
                String recipientQuery = "SELECT id FROM accounts WHERE account_number = ? AND user_id != ?";
                int recipientAccountId;
                try (PreparedStatement pstmt = conn.prepareStatement(recipientQuery)) {
                    pstmt.setString(1, recipientAccount);
                    pstmt.setInt(2, userId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Recipient account not found");
                        }
                        recipientAccountId = rs.getInt("id");
                    }
                }

                // Update sender's balance
                String updateSenderQuery = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSenderQuery)) {
                    pstmt.setDouble(1, amount);
                    pstmt.setInt(2, senderAccountId);
                    pstmt.executeUpdate();
                }

                // Update recipient's balance
                String updateRecipientQuery = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateRecipientQuery)) {
                    pstmt.setDouble(1, amount);
                    pstmt.setInt(2, recipientAccountId);
                    pstmt.executeUpdate();
                }

                // Record sender's transaction
                String senderTransactionQuery = "INSERT INTO transactions (account_id, type, amount, description) VALUES (?, 'TRANSFER_OUT', ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(senderTransactionQuery)) {
                    pstmt.setInt(1, senderAccountId);
                    pstmt.setDouble(2, amount);
                    pstmt.setString(3, "Transfer to " + recipientAccount + ": " + description);
                    pstmt.executeUpdate();
                }

                // Record recipient's transaction
                String recipientTransactionQuery = "INSERT INTO transactions (account_id, type, amount, description) VALUES (?, 'TRANSFER_IN', ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(recipientTransactionQuery)) {
                    pstmt.setInt(1, recipientAccountId);
                    pstmt.setDouble(2, amount);
                    pstmt.setString(3, "Transfer from " + accountNumberLabel.getText().split(": ")[1] + ": " + description);
                    pstmt.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Transfer successful!");
                refreshTransactionHistory();
                refreshAccountDetails();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create transaction table
        String[] columns = {"Type", "Amount", "Description", "Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transactionTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTransactionHistory());
        panel.add(refreshButton, BorderLayout.SOUTH);

        // Load initial transaction history
        refreshTransactionHistory();

        return panel;
    }

    private void performTransaction(String type) {
        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive amount.");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Get current balance
                    String balanceQuery = "SELECT balance FROM accounts WHERE user_id = ?";
                    double currentBalance;
                    try (PreparedStatement pstmt = conn.prepareStatement(balanceQuery)) {
                        pstmt.setInt(1, userId);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException("Account not found");
                            }
                            currentBalance = rs.getDouble("balance");
                        }
                    }

                    // Check if sufficient balance for debit
                    if (type.equals("DEBIT") && amount > currentBalance) {
                        throw new SQLException("Insufficient balance");
                    }

                    // Update balance
                    String updateQuery = "UPDATE accounts SET balance = balance " + 
                        (type.equals("CREDIT") ? "+" : "-") + 
                        "? WHERE user_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                        pstmt.setDouble(1, amount);
                        pstmt.setInt(2, userId);
                        pstmt.executeUpdate();
                    }

                    // Record transaction
                    String transactionQuery = "INSERT INTO transactions (account_id, type, amount, description) " +
                        "SELECT id, ?, ?, ? FROM accounts WHERE user_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(transactionQuery)) {
                        pstmt.setString(1, type);
                        pstmt.setDouble(2, amount);
                        pstmt.setString(3, descriptionField.getText());
                        pstmt.setInt(4, userId);
                        pstmt.executeUpdate();
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(this, type + " successful!");
                    refreshTransactionHistory();
                    refreshAccountDetails();
                    amountField.setText("");
                    descriptionField.setText("");

                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void refreshTransactionHistory() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT t.type, t.amount, t.description, t.created_at " +
                          "FROM transactions t " +
                          "JOIN accounts a ON t.account_id = a.id " +
                          "WHERE a.user_id = ? " +
                          "ORDER BY t.created_at DESC";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    DefaultTableModel model = (DefaultTableModel) transactionTable.getModel();
                    model.setRowCount(0);
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("type"),
                            rs.getDouble("amount"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error refreshing transaction history: " + e.getMessage());
        }
    }

    private void refreshAccountDetails() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT account_number, type, balance FROM accounts WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        accountNumberLabel.setText("Account Number: " + rs.getString("account_number"));
                        accountTypeLabel.setText("Account Type: " + rs.getString("type"));
                        balanceLabel.setText("Balance: $" + String.format("%.2f", rs.getDouble("balance")));
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error refreshing account details: " + e.getMessage());
        }
    }

    private JPanel createTicketPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table for existing tickets
        String[] columns = {"ID", "Subject", "Priority", "Status", "Created At"};
        ticketModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ticketTable = new JTable(ticketModel);
        JScrollPane scrollPane = new JScrollPane(ticketTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createTicketButton = new JButton("Create New Ticket");
        createTicketButton.addActionListener(e -> showCreateTicketDialog());
        buttonPanel.add(createTicketButton);
        panel.add(buttonPanel, BorderLayout.NORTH);

        // Load existing tickets
        loadTickets(ticketModel);

        return panel;
    }

    private void loadTickets(DefaultTableModel model) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT id, subject, priority, status, created_at FROM tickets WHERE user_id = ? ORDER BY created_at DESC";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    model.setRowCount(0);
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("subject"),
                            rs.getString("priority"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading tickets: " + e.getMessage());
        }
    }

    private void showCreateTicketDialog() {
        JDialog dialog = new JDialog(this, "Create Support Ticket", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Subject field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1;
        JTextField subjectField = new JTextField(20);
        panel.add(subjectField, gbc);

        // Priority selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Priority:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});
        panel.add(priorityCombo, gbc);

        // Description field
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        JTextArea descriptionArea = new JTextArea(5, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        panel.add(descScrollPane, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");

        submitButton.addActionListener(e -> {
            if (subjectField.getText().trim().isEmpty() || descriptionArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields.");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "INSERT INTO tickets (user_id, subject, description, priority) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, subjectField.getText().trim());
                    pstmt.setString(3, descriptionArea.getText().trim());
                    pstmt.setString(4, (String) priorityCombo.getSelectedItem());

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(dialog, "Ticket created successfully!");
                        dialog.dispose();
                        // Refresh the ticket list
                        loadTickets((DefaultTableModel) ticketTable.getModel());
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating ticket: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }
} 