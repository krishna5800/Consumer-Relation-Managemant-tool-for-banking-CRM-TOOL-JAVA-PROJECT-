package com.bank.crm.auth;

import com.bank.crm.database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Signup extends JFrame {
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<String> accountTypeCombo;
    private JTextField initialAmountField;
    private JButton signupButton;
    private JButton backButton;

    public Signup() {
        setTitle("New Customer Account");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel headerLabel = new JLabel("Open a Bank Account");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(headerLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        // Email field
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        formPanel.add(emailField, gbc);

        // Phone field
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(20);
        formPanel.add(phoneField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Confirm Password field
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        formPanel.add(confirmPasswordField, gbc);

        // Account Type field
        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Account Type:"), gbc);
        gbc.gridx = 1;
        accountTypeCombo = new JComboBox<>(new String[]{"SAVINGS", "CURRENT"});
        formPanel.add(accountTypeCombo, gbc);

        // Initial Amount field
        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("Initial Deposit:"), gbc);
        gbc.gridx = 1;
        initialAmountField = new JTextField(20);
        formPanel.add(initialAmountField, gbc);

        // Add buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        signupButton = new JButton("Open Account");
        backButton = new JButton("Back to Login");

        signupButton.addActionListener(e -> createAccount());
        backButton.addActionListener(e -> {
            new Login().setVisible(true);
            dispose();
        });

        buttonPanel.add(signupButton);
        buttonPanel.add(backButton);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private void createAccount() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String accountType = (String) accountTypeCombo.getSelectedItem();
        String initialAmountStr = initialAmountField.getText().trim();

        // Validate all fields
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || initialAmountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }

        // Validate initial amount
        double initialAmount;
        try {
            initialAmount = Double.parseDouble(initialAmountStr);
            if (initialAmount < 0) {
                JOptionPane.showMessageDialog(this, "Initial deposit cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid initial deposit amount.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check if email already exists
                String checkQuery = "SELECT COUNT(*) FROM users WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            JOptionPane.showMessageDialog(this, "Email already exists. Please use a different email.");
                            return;
                        }
                    }
                }

                // Create customer account
                String userQuery = "INSERT INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, 'CUSTOMER')";
                try (PreparedStatement pstmt = conn.prepareStatement(userQuery, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, email);
                    pstmt.setString(3, phone);
                    pstmt.setString(4, password);
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int userId = rs.getInt(1);

                            // Generate unique account number
                            String accountNumber = generateUniqueAccountNumber(conn);
                            if (accountNumber == null) {
                                throw new SQLException("Error generating account number.");
                            }

                            // Create bank account
                            String accountQuery = "INSERT INTO accounts (user_id, account_number, type, balance, status) " +
                                               "VALUES (?, ?, ?, ?, 'ACTIVE')";
                            try (PreparedStatement accountStmt = conn.prepareStatement(accountQuery)) {
                                accountStmt.setInt(1, userId);
                                accountStmt.setString(2, accountNumber);
                                accountStmt.setString(3, accountType);
                                accountStmt.setDouble(4, initialAmount);
                                accountStmt.executeUpdate();
                            }

                            // Record initial deposit
                            if (initialAmount > 0) {
                                String transactionQuery = "INSERT INTO transactions (account_id, type, amount, description) " +
                                                       "VALUES ((SELECT id FROM accounts WHERE user_id = ?), 'CREDIT', ?, 'Initial deposit')";
                                try (PreparedStatement transStmt = conn.prepareStatement(transactionQuery)) {
                                    transStmt.setInt(1, userId);
                                    transStmt.setDouble(2, initialAmount);
                                    transStmt.executeUpdate();
                                }
                            }

                            conn.commit();
                            JOptionPane.showMessageDialog(this, 
                                "Account created successfully!\n\n" +
                                "Account Details:\n" +
                                "Account Number: " + accountNumber + "\n" +
                                "Account Type: " + accountType + "\n" +
                                "Initial Balance: $" + String.format("%.2f", initialAmount));
                            dispose();
                            new Login().setVisible(true);
                        }
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error creating account: " + e.getMessage());
        }
    }

    private String generateUniqueAccountNumber(Connection conn) {
        try {
            String accountNumber;
            do {
                // Generate 8-digit number
                int randomNum = (int) (Math.random() * 90000000) + 10000000;
                accountNumber = String.valueOf(randomNum);

                // Check if account number exists
                String checkQuery = "SELECT COUNT(*) FROM accounts WHERE account_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                    pstmt.setString(1, accountNumber);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            return accountNumber;
                        }
                    }
                }
            } while (true);
        } catch (SQLException e) {
            return null;
        }
    }
} 