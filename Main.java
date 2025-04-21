package com.bank.crm;

import com.bank.crm.auth.Login;
import com.bank.crm.database.DatabaseInitializer;
import com.bank.crm.database.DatabaseConnection;
import javax.swing.*;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize database
        try {
            DatabaseInitializer.initialize();
            
            // Test database connection
            try (Connection conn = DatabaseConnection.getConnection()) {
                System.out.println("Database connection successful!");
                
                // Start application
                SwingUtilities.invokeLater(() -> {
                    Login login = new Login();
                    login.setVisible(true);
                });
            }
        } catch (Exception e) {
            System.err.println("Error initializing application: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Failed to connect to database. Please check if MySQL is running and credentials are correct.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
} 