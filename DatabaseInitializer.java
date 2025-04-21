package com.bank.crm.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        try {
            // Read schema.sql from filesystem
            String schema;
            try (BufferedReader reader = new BufferedReader(
                    new FileReader("src/main/java/com/bank/crm/database/schema.sql"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                schema = sb.toString();
            }

            // Execute schema
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Split and execute each statement
                for (String statement : schema.split(";")) {
                    statement = statement.trim();
                    if (!statement.isEmpty()) {
                        stmt.execute(statement);
                    }
                }
                System.out.println("Database initialized successfully!");
            }
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 