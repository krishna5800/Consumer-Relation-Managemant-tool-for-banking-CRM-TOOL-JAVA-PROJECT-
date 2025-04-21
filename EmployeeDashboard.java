package com.bank.crm.dashboard;

import com.bank.crm.database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmployeeDashboard extends BaseDashboard {
    private static final Logger LOGGER = Logger.getLogger(EmployeeDashboard.class.getName());
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JTable customerTable;
    private JTable ticketTable;
    
    // Table Models
    private DefaultTableModel customerModel;
    private DefaultTableModel ticketModel;

    public EmployeeDashboard(int userId, String userName) {
        super(userId, userName);
        setupLogger();
    }

    private void setupLogger() {
        LOGGER.setLevel(Level.ALL);
        LOGGER.info("EmployeeDashboard initialized for user: " + userName);
    }

    @Override
    protected void initializeComponents() {
        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create panels
        JPanel customerPanel = createCustomerPanel();
        JPanel ticketPanel = createTicketPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("Customers", customerPanel);
        tabbedPane.addTab("Tickets", ticketPanel);
        
        // Add tabbed pane to main panel
        add(tabbedPane, BorderLayout.CENTER);
        
        LOGGER.info("EmployeeDashboard components initialized successfully");
    }

    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columnNames = {"ID", "Name", "Email", "Phone", "Status"};
        customerModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        customerTable = new JTable(customerModel);
        JScrollPane scrollPane = new JScrollPane(customerTable);
        
        // Add components to panel
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Refresh customer list
        refreshCustomerList();
        
        return panel;
    }

    private JPanel createTicketPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columnNames = {"ID", "Customer", "Subject", "Priority", "Status", "Created At"};
        ticketModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        ticketTable = new JTable(ticketModel);
        JScrollPane scrollPane = new JScrollPane(ticketTable);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton updateStatusButton = new JButton("Update Status");
        JButton refreshButton = new JButton("Refresh");
        
        updateStatusButton.addActionListener(e -> showUpdateStatusDialog());
        refreshButton.addActionListener(e -> refreshTicketList());
        
        buttonPanel.add(updateStatusButton);
        buttonPanel.add(refreshButton);
        
        // Add components to panel
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Refresh ticket list
        refreshTicketList();
        
        return panel;
    }

    private void refreshCustomerList() {
        LOGGER.info("Customer list refresh requested");
        // Implementation for refreshing customer list
    }

    private void refreshTicketList() {
        try {
        	String query = "SELECT t.id, u.name as customer_name, t.subject, t.priority, t.status, t.created_at\n" +
                    "FROM tickets t\n" +
                    "JOIN users u ON t.user_id = u.id\n" +
                    "WHERE t.assigned_to = ?\n" +
                    "ORDER BY t.created_at DESC";

            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ticketModel.setRowCount(0);
                    while (rs.next()) {
                        Object[] row = {
                            rs.getInt("id"),
                            rs.getString("customer_name"),
                            rs.getString("subject"),
                            rs.getString("priority"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                        };
                        ticketModel.addRow(row);
                    }
                }
            }
            LOGGER.info("Ticket list refreshed successfully");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing ticket list", e);
            JOptionPane.showMessageDialog(this, "Error refreshing tickets: " + e.getMessage());
        }
    }

    private void showUpdateStatusDialog() {
        int selectedRow = ticketTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a ticket to update");
            return;
        }

        int ticketId = (int) ticketModel.getValueAt(selectedRow, 0);
        String currentStatus = (String) ticketModel.getValueAt(selectedRow, 4);

        JDialog dialog = new JDialog(this, "Update Ticket Status", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Status selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("New Status:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"IN_PROGRESS", "RESOLVED", "CLOSED"});
        statusCombo.setSelectedItem(currentStatus);
        panel.add(statusCombo, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton updateButton = new JButton("Update");
        JButton cancelButton = new JButton("Cancel");

        updateButton.addActionListener(e -> {
            String newStatus = (String) statusCombo.getSelectedItem();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "UPDATE tickets SET status = ? WHERE id = ? AND assigned_to = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, ticketId);
                    stmt.setInt(3, userId);

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        LOGGER.info("Ticket status updated successfully");
                        JOptionPane.showMessageDialog(dialog, "Status updated successfully!");
                        refreshTicketList();
                        dialog.dispose();
                    }
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error updating ticket status", ex);
                JOptionPane.showMessageDialog(dialog, "Error updating status: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void loadCustomerData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT id, name, email, phone, status FROM users WHERE role = 'CUSTOMER'";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    customerModel.setRowCount(0);
                    while (rs.next()) {
                        customerModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("status")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading customer data: " + e.getMessage());
        }
    }

    private void loadTicketData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT t.id, u.name as customer_name, t.subject, t.priority, t.status, t.created_at " +
                          "FROM tickets t " +
                          "JOIN users u ON t.user_id = u.id " +
                          "WHERE t.assigned_to = ? OR t.assigned_to IS NULL " +
                          "ORDER BY t.created_at DESC";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    ticketModel.setRowCount(0);
                    while (rs.next()) {
                        ticketModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("customer_name"),
                            rs.getString("subject"),
                            rs.getString("priority"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading ticket data: " + e.getMessage());
        }
    }

    private void showCustomerAccounts() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer first.");
            return;
        }

        int customerId = (int) customerModel.getValueAt(selectedRow, 0);
        String customerName = (String) customerModel.getValueAt(selectedRow, 1);

        JDialog dialog = new JDialog(this, "Accounts for " + customerName, true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());

        // Create accounts table
        String[] columns = {"Account Number", "Type", "Balance", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT account_number, type, balance, status FROM accounts WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("account_number"),
                            rs.getString("type"),
                            rs.getDouble("balance"),
                            rs.getString("status")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dialog, "Error loading accounts: " + e.getMessage());
        }

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showCreateAccountDialog() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer first.");
            return;
        }

        int customerId = (int) customerModel.getValueAt(selectedRow, 0);
        String customerName = (String) customerModel.getValueAt(selectedRow, 1);

        JDialog dialog = new JDialog(this, "Create Account for " + customerName, true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Account type selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Account Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"SAVINGS", "CURRENT"});
        panel.add(typeCombo, gbc);

        // Initial balance field
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Initial Balance:"), gbc);
        gbc.gridx = 1;
        JTextField balanceField = new JTextField(20);
        panel.add(balanceField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createButton = new JButton("Create");
        JButton cancelButton = new JButton("Cancel");

        createButton.addActionListener(e -> {
            try {
                double balance = Double.parseDouble(balanceField.getText());
                if (balance < 0) {
                    JOptionPane.showMessageDialog(dialog, "Balance cannot be negative.");
                    return;
                }

                String type = (String) typeCombo.getSelectedItem();
                String accountNumber = generateAccountNumber();

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String query = "INSERT INTO accounts (user_id, account_number, type, balance) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setInt(1, customerId);
                        pstmt.setString(2, accountNumber);
                        pstmt.setString(3, type);
                        pstmt.setDouble(4, balance);

                        int rowsAffected = pstmt.executeUpdate();
                        if (rowsAffected > 0) {
                            JOptionPane.showMessageDialog(dialog, "Account created successfully!");
                            showCustomerAccounts();
                            dialog.dispose();
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid balance.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating account: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis();
    }
} 