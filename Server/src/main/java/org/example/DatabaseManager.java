package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseManager {
    private final Connection connection;

    public DatabaseManager(String url, String user, String password) throws SQLException {
        // ЯВНАЯ РЕГИСТРАЦИЯ ДРАЙВЕРА
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL driver registered successfully");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        this.connection = DriverManager.getConnection(url, props);

        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, login VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(128) NOT NULL);";
        // ... остальные SQL запросы

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTableSQL);
            // ... выполнение остальных запросов
            System.out.println("Database tables initialized successfully.");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}