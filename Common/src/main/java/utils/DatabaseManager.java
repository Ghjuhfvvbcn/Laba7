package utils;

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

        // Дополнительные настройки для лучшей производительности
        props.setProperty("prepareThreshold", "5");

        this.connection = DriverManager.getConnection(url, props);

        initializeDatabase();
    }

    // 1. Метод для аутентификации пользователя
    public User authenticateUser(String login, String passwordHash) throws SQLException {
//        String sql = "SELECT id FROM users WHERE login = ? AND password_hash = ?";
        String sql = "SELECT id, login FROM users WHERE login = ? AND password_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, login);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) { // ← ДОБАВЛЕНО try-with-resources!
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String userLogin = rs.getString("login");
                    return new User(userId, userLogin);
                }
            }
        }
        return null; // Неверный логин/пароль
    }

    // 2. Метод для регистрации нового пользователя
    public boolean registerUser(String login, String passwordHash) throws SQLException {
        // Используем INSERT с ON CONFLICT для атомарной проверки и вставки
        String sql = "INSERT INTO users (login, password_hash) VALUES (?, ?) " +
                "ON CONFLICT (login) DO NOTHING RETURNING id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, login);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Если есть результат - пользователь создан
            }
        }
    }

    // 3. Метод для добавления нового MusicBand в БД
    public boolean insertMusicBand(Long key, MusicBand band, int ownerId) throws SQLException {
        // Вам нужно будет сначала получить или вставить studio
        Integer studioId = null;
        if (band.getStudio() != null && band.getStudio().getName() != null) {
            studioId = getOrInsertStudio(band.getStudio().getName());
        }

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

    public boolean updateMusicBand(Long key, MusicBand band, int ownerId) throws SQLException {
        if (!checkOwnership(key, ownerId)) {
            return false;
        }

        Integer studioId = null;
        if (band.getStudio() != null && band.getStudio().getName() != null) {
            studioId = getOrInsertStudio(band.getStudio().getName());
        }

        String sql = "UPDATE music_bands SET name = ?, coordinate_x = ?, coordinate_y = ?, " +
                "number_of_participants = ?, description = ?, genre = ?::music_genre, " +
                "studio_id = ? WHERE id = ? AND owner_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, band.getName());
            stmt.setDouble(2, band.getCoordinates().getX());
            stmt.setInt(3, band.getCoordinates().getY());
            stmt.setInt(4, band.getNumberOfParticipants());
            stmt.setString(5, band.getDescription());
            stmt.setString(6, band.getGenre().name());

            if (studioId != null) {
                stmt.setInt(7, studioId);
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setLong(8, key);
            stmt.setInt(9, ownerId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean removeMusicBand(Long key, int userId) throws SQLException {
        if (!checkOwnership(key, userId)) {
            return false;
        }

        String sql = "DELETE FROM music_bands WHERE id = ? AND owner_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, key);
            stmt.setInt(2, userId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public int clearUserMusicBands(int userId) throws SQLException {
        String sql = "DELETE FROM music_bands WHERE owner_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate();
        }
    }

    public boolean isConnectionValid() throws SQLException {
        return connection != null && !connection.isClosed() && connection.isValid(2);
    }

    private Integer getOrInsertStudio(String studioName) throws SQLException {
        if (studioName == null || studioName.trim().isEmpty()) {
            return null;
        }

        // Пытаемся сначала найти существующую студию
        String selectSql = "SELECT id FROM studios WHERE name = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, studioName);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        // Если не найдена, вставляем новую
        String insertSql = "INSERT INTO studios (name) VALUES (?) RETURNING id";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, studioName);
            try (ResultSet rs = insertStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        throw new SQLException("Failed to insert or find studio: " + studioName);
    }

    // 4. Метод для загрузки всей коллекции из БД при старте сервера
//    public TreeMap<Long, MusicBand> loadCollection() throws SQLException {
//        TreeMap<Long, MusicBand> collection = new TreeMap<>();
//        String sql = "SELECT mb.*, s.name as studio_name FROM music_bands mb LEFT JOIN studios s ON mb.studio_id = s.id";
//
//        try (Statement stmt = connection.createStatement();
//             ResultSet rs = stmt.executeQuery(sql)) {
//
//            while (rs.next()) {
//                Long id = rs.getLong("id");
//                // ... создаем объект MusicBand из ResultSet ...
//                MusicBand band = new MusicBand(
//                        id,
//                        rs.getString("name"),
//                        new Coordinates(rs.getDouble("coordinate_x"), rs.getInt("coordinate_y")),
//                        rs.getTimestamp("creation_date").toInstant().atZone(ZoneId.systemDefault()),
//                        rs.getInt("number_of_participants"),
//                        rs.getString("description"),
//                        MusicGenre.valueOf(rs.getString("genre")),
//                        new Studio(rs.getString("studio_name"))
//                );
//                collection.put(id, band);
//            }
//        }
//        return collection;
//    }
    public TreeMap<Long, MusicBand> loadCollection() throws SQLException {
        TreeMap<Long, MusicBand> collection = new TreeMap<>();
        String sql = "SELECT mb.*, s.name as studio_name FROM music_bands mb " +
                "LEFT JOIN studios s ON mb.studio_id = s.id";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                MusicBand band = new MusicBand(
                        id,
                        rs.getString("name"),
                        new Coordinates(rs.getDouble("coordinate_x"), rs.getInt("coordinate_y")),
                        rs.getTimestamp("creation_date").toInstant().atZone(ZoneId.systemDefault()),
                        rs.getInt("number_of_participants"),
                        rs.getString("description"),
                        MusicGenre.valueOf(rs.getString("genre")),
                        rs.getString("studio_name") != null ?
                                new Studio(rs.getString("studio_name")) : null
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
            try (ResultSet rs = stmt.executeQuery()) { // ← ДОБАВЛЕНО try-with-resources!
                return rs.next() && rs.getInt("owner_id") == userId;
            }
        }
    }
    // ... и другие методы для удаления, обновления ...

    private void initializeDatabase() throws SQLException {
        // Создаем таблицы, если их нет.
        // Используем CREATE TABLE IF NOT EXISTS для надежности.

        // Создаем тип enum если его нет
        String createGenreEnumSQL =
                "DO $$ " +
                        "BEGIN " +
                        "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'music_genre') THEN " +
                        "        CREATE TYPE music_genre AS ENUM ('ROCK', 'PSYCHEDELIC_CLOUD_RAP', 'JAZZ', 'SOUL', 'POST_ROCK'); " +
                        "    END IF; " +
                        "END $$;";

// Создаем индексы для улучшения производительности
        String createIndexesSQL =
                "CREATE INDEX IF NOT EXISTS idx_music_bands_owner_id ON music_bands(owner_id);" +
                        "CREATE INDEX IF NOT EXISTS idx_music_bands_creation_date ON music_bands(creation_date);";

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
//                "genre VARCHAR(50) NOT NULL CHECK (genre IN ('ROCK', 'PSYCHEDELIC_CLOUD_RAP', 'JAZZ', 'SOUL', 'POST_ROCK'))," +
                "genre music_genre NOT NULL," + // ← ИЗМЕНЕНО: используем тип enum
                "studio_id INTEGER REFERENCES studios(id) ON DELETE SET NULL" +
            ");" ;

        // Выполняем все SQL-запросы по очереди.
        try (Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(false);

            stmt.execute(createGenreEnumSQL);
            stmt.execute(createUsersTableSQL);
            stmt.execute(createStudiosTableSQL);
            stmt.execute(createSequenceSQL);
            stmt.execute(createMusicBandsTableSQL);
            stmt.execute(createIndexesSQL);

            connection.commit();
            connection.setAutoCommit(true);

            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            connection.rollback();
            connection.setAutoCommit(true);

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
            System.out.println("Database connection closed successfully."); // ← ДОБАВЛЕНО
        }
    }
}