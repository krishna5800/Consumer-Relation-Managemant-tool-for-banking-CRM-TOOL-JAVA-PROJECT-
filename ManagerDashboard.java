package com.bank.crm.dashboard;

import com.bank.crm.database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagerDashboard extends BaseDashboard {
    private static final Logger LOGGER = Logger.getLogger(ManagerDashboard.class.getName());
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JTable employeeTable;
    private JTable customerTable;
    private JTable ticketTable;
    private JTextArea reportArea;
    
    // Table Models
    private DefaultTableModel employeeModel;
    private DefaultTableModel customerModel;
    private DefaultTableModel ticketModel;

    // Constants for UI styling
    private static final Color PRIMARY_COLOR = new Color(51, 122, 183);
    private static final Color SECONDARY_COLOR = new Color(238, 238, 238);
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font NORMAL_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final int BUTTON_PADDING = 10;
    private static final Dimension BUTTON_SIZE = new Dimension(120, 30);

    public ManagerDashboard(int userId, String userName) {
        super(userId, userName);
        setupLogger();
    }

    private void setupLogger() {
        LOGGER.setLevel(Level.ALL);
        LOGGER.info("ManagerDashboard initialized for user: " + userName);
    }

    @Override
    protected void initializeComponents() {
        try {
            setLayout(new BorderLayout());
            add(createHeaderPanel(), BorderLayout.NORTH);

            // Create main panel with tabs
            tabbedPane = new JTabbedPane();
            tabbedPane.setFont(HEADER_FONT);
            
            // Add all tabs
            tabbedPane.addTab("Employee Management", createEmployeePanel());
            tabbedPane.addTab("Customers", createCustomerPanel());
            tabbedPane.addTab("Support Tickets", createTicketPanel());
            tabbedPane.addTab("Reports", createReportPanel());
            
            add(tabbedPane, BorderLayout.CENTER);

            // Load initial data
            refreshAllData();
            LOGGER.info("ManagerDashboard components initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing dashboard components", e);
            showErrorDialog("Failed to initialize dashboard components");
        }
    }

    private void refreshAllData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                refreshEmployeeList();
                refreshCustomerList();
                refreshTicketList();
                return null;
            }
        };
        worker.execute();
    }

    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table
        String[] columns = {"ID", "Name", "Email", "Phone"};
        employeeModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(employeeModel);
        employeeTable.setFont(NORMAL_FONT);
        employeeTable.getTableHeader().setFont(HEADER_FONT);
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, BUTTON_PADDING, BUTTON_PADDING));
        
        JButton addButton = createStyledButton("Add Employee");
        JButton editButton = createStyledButton("Edit Employee");
        JButton deleteButton = createStyledButton("Delete Employee");
        JButton refreshButton = createStyledButton("Refresh");

        addButton.addActionListener(e -> showAddEmployeeDialog());
        editButton.addActionListener(e -> showEditEmployeeDialog());
        deleteButton.addActionListener(e -> deleteSelectedEmployee());
        refreshButton.addActionListener(e -> refreshEmployeeList());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.NORTH);
        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(NORMAL_FONT);
        button.setPreferredSize(BUTTON_SIZE);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        return button;
    }

    private void refreshEmployeeList() {
        try {
            employeeModel.setRowCount(0);
            String query = "SELECT id, name, email, phone FROM users WHERE role = 'EMPLOYEE' ORDER BY name";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                    };
                    employeeModel.addRow(row);
                }
                LOGGER.info("Employee list refreshed successfully");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing employee list", e);
            showErrorDialog("Failed to refresh employee list");
        }
    }

    private void showAddEmployeeDialog() {
        JDialog dialog = new JDialog(this, "Add Employee", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create input fields
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        // Add components to dialog
        addDialogComponent(dialog, "Name:", nameField, gbc, 0);
        addDialogComponent(dialog, "Email:", emailField, gbc, 1);
        addDialogComponent(dialog, "Phone:", phoneField, gbc, 2);
        addDialogComponent(dialog, "Password:", passwordField, gbc, 3);

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = createStyledButton("Save");
        JButton cancelButton = createStyledButton("Cancel");

        saveButton.addActionListener(e -> {
            if (validateEmployeeInput(nameField.getText(), emailField.getText(), phoneField.getText())) {
                addEmployee(nameField.getText(), emailField.getText(), phoneField.getText(), 
                          new String(passwordField.getPassword()));
                dialog.dispose();
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridy = 4;
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

    private boolean validateEmployeeInput(String name, String email, String phone) {
        if (name.trim().isEmpty() || email.trim().isEmpty() || phone.trim().isEmpty()) {
            showErrorDialog("All fields are required");
            return false;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showErrorDialog("Invalid email format");
            return false;
        }
        return true;
    }

    private void addEmployee(String name, String email, String phone, String password) {
        try {
            String query = "INSERT INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, 'EMPLOYEE')";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setString(3, phone);
                stmt.setString(4, password);
                
                int result = stmt.executeUpdate();
                if (result > 0) {
                    LOGGER.info("New employee added successfully: " + email);
                    refreshEmployeeList();
                    showSuccessDialog("Employee added successfully!");
                } else {
                    showErrorDialog("Failed to add employee");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding employee", e);
            showErrorDialog("Error adding employee: " + e.getMessage());
        }
    }

    private void showEditEmployeeDialog() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            showErrorDialog("Please select an employee to edit");
            return;
        }

        // Implementation for edit dialog (similar to add dialog)
        // TODO: Implement edit employee functionality
        LOGGER.info("Edit employee dialog shown for employee ID: " + employeeTable.getValueAt(selectedRow, 0));
    }

    private void deleteSelectedEmployee() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            showErrorDialog("Please select an employee to delete");
            return;
        }

        int employeeId = (Integer) employeeTable.getValueAt(selectedRow, 0);
        String employeeName = (String) employeeTable.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete employee: " + employeeName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            deleteEmployee(employeeId);
        }
    }

    private void deleteEmployee(int employeeId) {
        try {
            String query = "DELETE FROM users WHERE id = ? AND role = 'EMPLOYEE'";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, employeeId);
                int result = stmt.executeUpdate();
                
                if (result > 0) {
                    LOGGER.info("Employee deleted successfully: ID " + employeeId);
                    refreshEmployeeList();
                    showSuccessDialog("Employee deleted successfully");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting employee", e);
            showErrorDialog("Failed to delete employee");
        }
    }

    private void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE));
    }

    private void showSuccessDialog(String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE));
    }

    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table for customer list
        String[] columns = {"ID", "Name", "Email", "Phone", "Status"};
        customerModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customerTable = new JTable(customerModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(customerTable);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = createStyledButton("Refresh");

        refreshButton.addActionListener(e -> refreshCustomerList());

        buttonPanel.add(refreshButton);

        // Add components to main panel
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Load initial customer list
        refreshCustomerList();

        return panel;
    }

    private void refreshCustomerList() {
        try {
            customerModel.setRowCount(0);
            String query = "SELECT u.id, u.name, u.email, u.phone, u.status\n" +
                    "FROM users u\n" +
                    "WHERE u.role = 'CUSTOMER'\n" +
                    "ORDER BY u.name";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("status")
                    };
                    customerModel.addRow(row);
                }
                LOGGER.info("Customer list refreshed successfully");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing customer list", e);
            showErrorDialog("Failed to refresh customer list");
        }
    }

    private void refreshTicketList() {
        try {
            ticketModel.setRowCount(0);
            String query = "SELECT t.id, u.name as customer_name, t.subject, t.priority, t.status, " +
                    "e.name as assigned_to, t.created_at " +
                    "FROM tickets t " +
                    "JOIN users u ON t.user_id = u.id " +
                    "LEFT JOIN users e ON t.assigned_to = e.id " +
                    "ORDER BY t.created_at DESC";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("customer_name"),
                        rs.getString("subject"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("assigned_to"),
                        rs.getTimestamp("created_at")
                    };
                    ticketModel.addRow(row);
                }
                LOGGER.info("Ticket list refreshed successfully");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing ticket list", e);
            showErrorDialog("Failed to refresh ticket list");
        }
    }

    private JPanel createTicketPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table for ticket list
        String[] columns = {"ID", "Customer", "Subject", "Priority", "Status", "Assigned To", "Created At"};
        ticketModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ticketTable = new JTable(ticketModel);
        ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(ticketTable);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton assignButton = createStyledButton("Assign Ticket");
        JButton updateButton = createStyledButton("Update Status");
        JButton refreshButton = createStyledButton("Refresh");

        assignButton.addActionListener(e -> {
            int selectedRow = ticketTable.getSelectedRow();
            if (selectedRow == -1) {
                showErrorDialog("Please select a ticket first");
                return;
            }
            int ticketId = (int) ticketModel.getValueAt(selectedRow, 0);
            showAssignTicketDialog(ticketId);
        });

        updateButton.addActionListener(e -> {
            int selectedRow = ticketTable.getSelectedRow();
            if (selectedRow == -1) {
                showErrorDialog("Please select a ticket first");
                return;
            }
            int ticketId = (int) ticketModel.getValueAt(selectedRow, 0);
            showUpdateStatusDialog(ticketId);
        });

        refreshButton.addActionListener(e -> refreshTicketList());

        buttonPanel.add(assignButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(refreshButton);

        // Add components to main panel
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Load initial ticket list
        refreshTicketList();

        return panel;
    }

    private void showAssignTicketDialog(int ticketId) {
        JDialog dialog = new JDialog(this, "Assign Ticket", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create employee combo box
        JComboBox<String> employeeCombo = new JComboBox<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT id, name FROM users WHERE role = 'EMPLOYEE' ORDER BY name";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employeeCombo.addItem(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading employees", e);
            showErrorDialog("Failed to load employees");
            return;
        }

        // Add components to dialog
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("Assign to:"), gbc);
        gbc.gridx = 1;
        dialog.add(employeeCombo, gbc);

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = createStyledButton("Save");
        JButton cancelButton = createStyledButton("Cancel");

        saveButton.addActionListener(e -> {
            String employeeName = (String) employeeCombo.getSelectedItem();
            assignTicket(ticketId, employeeName);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void assignTicket(int ticketId, String employeeName) {
        try (Connection conn = DatabaseConnection.getConnection()) {
        	String query = "UPDATE tickets t " +
                    "SET t.assigned_to = (SELECT id FROM users WHERE name = ? AND role = 'EMPLOYEE'), " +
                    "t.status = 'ASSIGNED' " +
                    "WHERE t.id = ?";

            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, employeeName);
                stmt.setInt(2, ticketId);
                int result = stmt.executeUpdate();
                
                if (result > 0) {
                    LOGGER.info("Ticket assigned successfully: ID " + ticketId);
                    refreshTicketList();
                    showSuccessDialog("Ticket assigned successfully");
                } else {
                    showErrorDialog("Failed to assign ticket");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error assigning ticket", e);
            showErrorDialog("Failed to assign ticket");
        }
    }

    private void showUpdateStatusDialog(int ticketId) {
        JDialog dialog = new JDialog(this, "Update Ticket Status", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create status combo box
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"});

        // Add components to dialog
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("New Status:"), gbc);
        gbc.gridx = 1;
        dialog.add(statusCombo, gbc);

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = createStyledButton("Save");
        JButton cancelButton = createStyledButton("Cancel");

        saveButton.addActionListener(e -> {
            String newStatus = (String) statusCombo.getSelectedItem();
            updateTicketStatus(ticketId, newStatus);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateTicketStatus(int ticketId, String newStatus) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "UPDATE tickets SET status = ? WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, newStatus);
                stmt.setInt(2, ticketId);
                int result = stmt.executeUpdate();
                
                if (result > 0) {
                    LOGGER.info("Ticket status updated successfully: ID " + ticketId);
                    refreshTicketList();
                    showSuccessDialog("Ticket status updated successfully");
                } else {
                    showErrorDialog("Failed to update ticket status");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating ticket status", e);
            showErrorDialog("Failed to update ticket status");
        }
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create tabbed pane for different types of reports
        JTabbedPane reportTabs = new JTabbedPane();
        reportTabs.setFont(HEADER_FONT);

        // Customer Statistics Tab
        JPanel customerStatsPanel = new JPanel(new BorderLayout());
        customerStatsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table for customer statistics
        String[] customerColumns = {"Metric", "Value"};
        DefaultTableModel customerStatsModel = new DefaultTableModel(customerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable customerStatsTable = new JTable(customerStatsModel);
        JScrollPane customerScrollPane = new JScrollPane(customerStatsTable);
        customerStatsPanel.add(customerScrollPane, BorderLayout.CENTER);

        // Load customer statistics
        try (Connection conn = DatabaseConnection.getConnection()) {
        	String query = "SELECT " +
                    "(SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') as total_customers, " +
                    "(SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER' AND status = 'ACTIVE') as active_customers, " +
                    "(SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER' AND status = 'INACTIVE') as inactive_customers, " +
                    "(SELECT COUNT(*) FROM accounts WHERE type = 'SAVINGS') as savings_accounts, " +
                    "(SELECT COUNT(*) FROM accounts WHERE type = 'CURRENT') as current_accounts, " +
                    "(SELECT COUNT(*) FROM accounts WHERE type = 'FIXED_DEPOSIT') as fixed_deposit_accounts";
            
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    customerStatsModel.addRow(new Object[]{"Total Customers", rs.getInt("total_customers")});
                    customerStatsModel.addRow(new Object[]{"Active Customers", rs.getInt("active_customers")});
                    customerStatsModel.addRow(new Object[]{"Inactive Customers", rs.getInt("inactive_customers")});
                    customerStatsModel.addRow(new Object[]{"Savings Accounts", rs.getInt("savings_accounts")});
                    customerStatsModel.addRow(new Object[]{"Current Accounts", rs.getInt("current_accounts")});
                    customerStatsModel.addRow(new Object[]{"Fixed Deposit Accounts", rs.getInt("fixed_deposit_accounts")});
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading customer statistics", e);
            showErrorDialog("Failed to load customer statistics");
        }

        // Ticket Statistics Tab
        JPanel ticketStatsPanel = new JPanel(new BorderLayout());
        ticketStatsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table for ticket statistics
        String[] ticketColumns = {"Metric", "Value"};
        DefaultTableModel ticketStatsModel = new DefaultTableModel(ticketColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable ticketStatsTable = new JTable(ticketStatsModel);
        JScrollPane ticketScrollPane = new JScrollPane(ticketStatsTable);
        ticketStatsPanel.add(ticketScrollPane, BorderLayout.CENTER);

        // Load ticket statistics
        try (Connection conn = DatabaseConnection.getConnection()) {
        	String query = "SELECT " +
                    "(SELECT COUNT(*) FROM tickets) as total_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'OPEN') as open_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'ASSIGNED') as assigned_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'IN_PROGRESS') as in_progress_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'RESOLVED') as resolved_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'CLOSED') as closed_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE priority = 'HIGH') as high_priority_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE priority = 'MEDIUM') as medium_priority_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE priority = 'LOW') as low_priority_tickets";
            
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ticketStatsModel.addRow(new Object[]{"Total Tickets", rs.getInt("total_tickets")});
                    ticketStatsModel.addRow(new Object[]{"Open Tickets", rs.getInt("open_tickets")});
                    ticketStatsModel.addRow(new Object[]{"Assigned Tickets", rs.getInt("assigned_tickets")});
                    ticketStatsModel.addRow(new Object[]{"In Progress Tickets", rs.getInt("in_progress_tickets")});
                    ticketStatsModel.addRow(new Object[]{"Resolved Tickets", rs.getInt("resolved_tickets")});
                    ticketStatsModel.addRow(new Object[]{"Closed Tickets", rs.getInt("closed_tickets")});
                    ticketStatsModel.addRow(new Object[]{"High Priority Tickets", rs.getInt("high_priority_tickets")});
                    ticketStatsModel.addRow(new Object[]{"Medium Priority Tickets", rs.getInt("medium_priority_tickets")});
                    ticketStatsModel.addRow(new Object[]{"Low Priority Tickets", rs.getInt("low_priority_tickets")});
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading ticket statistics", e);
            showErrorDialog("Failed to load ticket statistics");
        }

        // Add all tabs to the tabbed pane
        reportTabs.addTab("Customer Statistics", customerStatsPanel);
        reportTabs.addTab("Ticket Statistics", ticketStatsPanel);

        // Add refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = createStyledButton("Refresh Reports");
        refreshButton.addActionListener(e -> {
            // Refresh all statistics
            refreshCustomerStatistics(customerStatsModel);
            refreshTicketStatistics(ticketStatsModel);
        });
        buttonPanel.add(refreshButton);

        // Add components to main panel
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(reportTabs, BorderLayout.CENTER);

        return panel;
    }

    private void refreshCustomerStatistics(DefaultTableModel model) {
        try (Connection conn = DatabaseConnection.getConnection()) {
        	String query = "SELECT " +
                    "(SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') as total_customers, " +
                    "(SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER' AND status = 'ACTIVE') as active_customers, " +
                    "(SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER' AND status = 'INACTIVE') as inactive_customers, " +
                    "(SELECT COUNT(*) FROM accounts WHERE type = 'SAVINGS') as savings_accounts, " +
                    "(SELECT COUNT(*) FROM accounts WHERE type = 'CURRENT') as current_accounts, " +
                    "(SELECT COUNT(*) FROM accounts WHERE type = 'FIXED_DEPOSIT') as fixed_deposit_accounts";
            
            model.setRowCount(0);
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    model.addRow(new Object[]{"Total Customers", rs.getInt("total_customers")});
                    model.addRow(new Object[]{"Active Customers", rs.getInt("active_customers")});
                    model.addRow(new Object[]{"Inactive Customers", rs.getInt("inactive_customers")});
                    model.addRow(new Object[]{"Savings Accounts", rs.getInt("savings_accounts")});
                    model.addRow(new Object[]{"Current Accounts", rs.getInt("current_accounts")});
                    model.addRow(new Object[]{"Fixed Deposit Accounts", rs.getInt("fixed_deposit_accounts")});
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing customer statistics", e);
            showErrorDialog("Failed to refresh customer statistics");
        }
    }

    private void refreshTicketStatistics(DefaultTableModel model) {
        try (Connection conn = DatabaseConnection.getConnection()) {
        	String query = "SELECT " +
                    "(SELECT COUNT(*) FROM tickets) as total_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'OPEN') as open_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'ASSIGNED') as assigned_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'IN_PROGRESS') as in_progress_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'RESOLVED') as resolved_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE status = 'CLOSED') as closed_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE priority = 'HIGH') as high_priority_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE priority = 'MEDIUM') as medium_priority_tickets, " +
                    "(SELECT COUNT(*) FROM tickets WHERE priority = 'LOW') as low_priority_tickets";
            
            model.setRowCount(0);
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    model.addRow(new Object[]{"Total Tickets", rs.getInt("total_tickets")});
                    model.addRow(new Object[]{"Open Tickets", rs.getInt("open_tickets")});
                    model.addRow(new Object[]{"Assigned Tickets", rs.getInt("assigned_tickets")});
                    model.addRow(new Object[]{"In Progress Tickets", rs.getInt("in_progress_tickets")});
                    model.addRow(new Object[]{"Resolved Tickets", rs.getInt("resolved_tickets")});
                    model.addRow(new Object[]{"Closed Tickets", rs.getInt("closed_tickets")});
                    model.addRow(new Object[]{"High Priority Tickets", rs.getInt("high_priority_tickets")});
                    model.addRow(new Object[]{"Medium Priority Tickets", rs.getInt("medium_priority_tickets")});
                    model.addRow(new Object[]{"Low Priority Tickets", rs.getInt("low_priority_tickets")});
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing ticket statistics", e);
            showErrorDialog("Failed to refresh ticket statistics");
        }
    }
} 