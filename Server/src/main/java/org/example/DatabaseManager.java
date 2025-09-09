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
        // Создаем таблицы, если их нет.
        // Используем CREATE TABLE IF NOT EXISTS для надежности.

        String createUsersTableSQL =
            "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY," +
                "login VARCHAR(255) NOT NULL UNIQUE," +
                "password_hash VARCHAR(128) NOT NULL" +
            ");" ;

        String createStudiosTableSQL =
            "CREATE TABLE IF NOT EXISTS studios (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL UNIQUE" +
            ");" ;

        String createSequenceSQL =
            "CREATE SEQUENCE IF NOT EXISTS music_bands_id_seq START 1;" ;

        String createMusicBandsTableSQL =
            "CREATE TABLE IF NOT EXISTS music_bands (" +
                "id BIGINT PRIMARY KEY DEFAULT nextval('music_bands_id_seq')," +
                "owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                "name VARCHAR(255) NOT NULL," +
                "coordinate_x DOUBLE PRECISION NOT NULL," +
                "coordinate_y INTEGER NOT NULL," +
                "creation_date TIMESTAMP WITH TIME ZONE NOT NULL," +
                "number_of_participants INTEGER NOT NULL CHECK (number_of_participants > 0)," +
                "description TEXT," +
                "genre VARCHAR(50) NOT NULL CHECK (genre IN ('ROCK', 'PSYCHEDELIC_CLOUD_RAP', 'JAZZ', 'SOUL', 'POST_ROCK'))," +
                "studio_id INTEGER REFERENCES studios(id) ON DELETE SET NULL" +
            ");" ;

        // Выполняем все SQL-запросы по очереди.
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTableSQL);
            stmt.execute(createStudiosTableSQL);
            stmt.execute(createSequenceSQL);
            stmt.execute(createMusicBandsTableSQL);
            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
            throw e; // Пробрасываем исключение дальше, т.к. без таблиц работа невозможна
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