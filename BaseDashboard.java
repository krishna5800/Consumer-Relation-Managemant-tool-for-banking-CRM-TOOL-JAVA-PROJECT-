package com.bank.crm.dashboard;

import javax.swing.*;
import java.awt.*;

public abstract class BaseDashboard extends JFrame {
    protected final int userId;
    protected final String userName;

    public BaseDashboard(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        
        setTitle("CRM Bank - " + getClass().getSimpleName().replace("Dashboard", ""));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(logoutItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        initializeComponents();
    }

    protected abstract void initializeComponents();

    protected JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel welcomeLabel = new JLabel("Welcome, " + userName);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            dispose();
            new com.bank.crm.auth.Login().setVisible(true);
        });
        headerPanel.add(logoutButton, BorderLayout.EAST);
        
        return headerPanel;
    }

    protected void logout() {
        dispose();
        new com.bank.crm.auth.Login().setVisible(true);
    }
} 