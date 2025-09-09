package org.example;

import data.*;

import java.sql.*;
import java.time.ZoneId;
import java.util.Properties;
import java.util.TreeMap;

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
        // Может понадобиться для корректной работы с timezone
        props.setProperty("options", "-c timezone=UTC");
        this.connection = DriverManager.getConnection(url, props);

        initializeDatabase();
    }

    // 1. Метод для аутентификации пользователя
    public User authenticateUser(String login, String passwordHash) throws SQLException {
        String sql = "SELECT id FROM users WHERE login = ? AND password_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, login);
            stmt.setString(2, passwordHash);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id");
                return new User(userId, login);
            }
        }
        return null; // Неверный логин/пароль
    }

    // 2. Метод для регистрации нового пользователя
    public boolean registerUser(String login, String passwordHash) throws SQLException {
        // Сначала проверяем, нет ли такого пользователя
        String checkSql = "SELECT id FROM users WHERE login = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, login);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return false; // Пользователь уже существует
            }
        }

        String insertSql = "INSERT INTO users (login, password_hash) VALUES (?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, login);
            insertStmt.setString(2, passwordHash);
            insertStmt.executeUpdate();
            return true;
        }
    }

    // 3. Метод для добавления нового MusicBand в БД
    public boolean insertMusicBand(Long key, MusicBand band, int ownerId) throws SQLException {
        // Вам нужно будет сначала получить или вставить studio
        Integer studioId = getOrInsertStudio(band.getStudio().getName());

        String sql = "INSERT INTO music_bands (id, owner_id, name, coordinate_x, coordinate_y, creation_date, number_of_participants, description, genre, studio_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::music_genre, ?)"; // Обратите внимание на приведение типа ::music_genre

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, key); // Используем предоставленный ключ как ID
            stmt.setInt(2, ownerId);
            stmt.setString(3, band.getName());
            stmt.setDouble(4, band.getCoordinates().getX());
            stmt.setInt(5, band.getCoordinates().getY());
            stmt.setTimestamp(6, Timestamp.from(band.getCreationDate().toInstant()));
            stmt.setInt(7, band.getNumberOfParticipants());
            stmt.setString(8, band.getDescription());
            stmt.setString(9, band.getGenre().name()); // Сохраняем имя enum
            stmt.setObject(10, studioId, Types.INTEGER); // Может быть null

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    private Integer getOrInsertStudio(String studioName) throws SQLException {
        // Реализуйте логику вставки или получения ID студии
        // ...
        return 0;
    }

    // 4. Метод для загрузки всей коллекции из БД при старте сервера
    public TreeMap<Long, MusicBand> loadCollection() throws SQLException {
        TreeMap<Long, MusicBand> collection = new TreeMap<>();
        String sql = "SELECT mb.*, s.name as studio_name FROM music_bands mb LEFT JOIN studios s ON mb.studio_id = s.id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                // ... создаем объект MusicBand из ResultSet ...
                MusicBand band = new MusicBand(
                        id,
                        rs.getString("name"),
                        new Coordinates(rs.getDouble("coordinate_x"), rs.getInt("coordinate_y")),
                        rs.getTimestamp("creation_date").toInstant().atZone(ZoneId.systemDefault()),
                        rs.getInt("number_of_participants"),
                        rs.getString("description"),
                        MusicGenre.valueOf(rs.getString("genre")),
                        new Studio(rs.getString("studio_name"))
                );
                collection.put(id, band);
            }
        }
        return collection;
    }

    // 5. Методы update, delete, checkOwnership и т.д.
    public boolean checkOwnership(long bandId, int userId) throws SQLException {
        String sql = "SELECT owner_id FROM music_bands WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, bandId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("owner_id") == userId;
        }
    }
    // ... и другие методы для удаления, обновления ...

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