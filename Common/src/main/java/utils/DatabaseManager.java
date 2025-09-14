package utils;

import data.*;

import java.sql.*;
import java.time.ZoneId;
import java.util.Properties;
import java.util.TreeMap;

public class DatabaseManager {
    private final Connection connection;

    public DatabaseManager(String url, String user, String password) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL driver registered successfully");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("options", "-c timezone=UTC");

        props.setProperty("prepareThreshold", "5");

        this.connection = DriverManager.getConnection(url, props);

        if (connection != null && !connection.isClosed()) {
            System.out.println("Connected to database successfully");
            System.out.println("Database URL: " + url);
            initializeDatabase();
        } else {
            throw new SQLException("Failed to establish database connection");
        }
    }

    public User authenticateUser(String login, String passwordHash) throws SQLException {
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
        return null;
    }

    public boolean registerUser(String login, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (login, password_hash) VALUES (?, ?) " +
                "ON CONFLICT (login) DO NOTHING RETURNING id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, login);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean insertMusicBand(Long key, MusicBand band, int ownerId) throws SQLException {
        Integer studioId = null;
        if (band.getStudio() != null && band.getStudio().getName() != null) {
            studioId = getOrInsertStudio(band.getStudio().getName());
        }

        String sql = "INSERT INTO music_bands (id, owner_id, name, coordinate_x, coordinate_y, " +
                "creation_date, number_of_participants, description, genre, studio_id, collection_key) " +
                "VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ownerId);
            stmt.setString(2, band.getName());
            stmt.setDouble(3, band.getCoordinates().getX());
            stmt.setInt(4, band.getCoordinates().getY());
            stmt.setTimestamp(5, Timestamp.from(band.getCreationDate().toInstant()));
            stmt.setInt(6, band.getNumberOfParticipants());
            stmt.setString(7, band.getDescription());
            stmt.setString(8, band.getGenre().name());
            stmt.setObject(9, studioId, Types.INTEGER);
            stmt.setLong(10, key);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long generatedId = rs.getLong("id");
                    band.setId(generatedId);
                    return true;
                }
            }
        }
        return false;
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
                "number_of_participants = ?, description = ?, genre = ?, " +
                "studio_id = ? WHERE collection_key = ? AND owner_id = ?";

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

        String sql = "DELETE FROM music_bands WHERE collection_key = ? AND owner_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, key); // collection_key
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

        String selectSql = "SELECT id FROM studios WHERE name = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, studioName);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

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

    public TreeMap<Long, MusicBand> loadCollection() throws SQLException {
        TreeMap<Long, MusicBand> collection = new TreeMap<>();
        String sql = "SELECT mb.*, s.name as studio_name FROM music_bands mb " +
                "LEFT JOIN studios s ON mb.studio_id = s.id";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Long collectionKey = rs.getLong("collection_key");
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
                                new Studio(rs.getString("studio_name")) : null,
                        rs.getInt("owner_id")
                );

                collection.put(collectionKey, band);
            }
        }
        return collection;
    }

    public boolean checkOwnership(long collectionKey, int userId) throws SQLException {
        String sql = "SELECT owner_id FROM music_bands WHERE collection_key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, collectionKey);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("owner_id") == userId;
            }
        }
    }

    private void initializeDatabase() throws SQLException {

        String createUsersTableSQL =
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id SERIAL PRIMARY KEY," +
                        "login VARCHAR(255) NOT NULL UNIQUE," +
                        "password_hash VARCHAR(128) NOT NULL" +
                        ");";

        String createStudiosTableSQL =
                "CREATE TABLE IF NOT EXISTS studios (" +
                        "id SERIAL PRIMARY KEY," +
                        "name VARCHAR(255) NOT NULL UNIQUE" +
                        ");";

        String createSequenceSQL =
                "CREATE SEQUENCE IF NOT EXISTS music_bands_id_seq START 1;";

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
                        "studio_id INTEGER REFERENCES studios(id) ON DELETE SET NULL," +
                        "collection_key BIGINT NOT NULL UNIQUE" +
                        ");";

        String createIndexesSQL =
                "CREATE INDEX IF NOT EXISTS idx_music_bands_owner_id ON music_bands(owner_id);" +
                        "CREATE INDEX IF NOT EXISTS idx_music_bands_creation_date ON music_bands(creation_date);" +
                        "CREATE INDEX IF NOT EXISTS idx_music_bands_collection_key ON music_bands(collection_key);";

        try (Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(true);

            stmt.execute(createUsersTableSQL);
            stmt.execute(createStudiosTableSQL);
            stmt.execute(createSequenceSQL);
            stmt.execute(createMusicBandsTableSQL);
            stmt.execute(createIndexesSQL);

            System.out.println("Database tables initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Error creating database tables: " + e.getMessage());
            throw e;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Database connection closed successfully.");
        }
    }

    public TreeMap<Long, MusicBand> loadUserCollection(int userId) throws SQLException {
        TreeMap<Long, MusicBand> userCollection = new TreeMap<>();
        String sql = "SELECT mb.*, s.name as studio_name FROM music_bands mb " +
                "LEFT JOIN studios s ON mb.studio_id = s.id " +
                "WHERE mb.owner_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Long collectionKey = rs.getLong("collection_key");
                    Long id = rs.getLong("id");
                    int ownerId = rs.getInt("owner_id");

                    MusicBand band = new MusicBand(
                            id,
                            rs.getString("name"),
                            new Coordinates(rs.getDouble("coordinate_x"), rs.getInt("coordinate_y")),
                            rs.getTimestamp("creation_date").toInstant().atZone(ZoneId.systemDefault()),
                            rs.getInt("number_of_participants"),
                            rs.getString("description"),
                            MusicGenre.valueOf(rs.getString("genre")),
                            rs.getString("studio_name") != null ?
                                    new Studio(rs.getString("studio_name")) : null,
                            ownerId
                    );
                    userCollection.put(collectionKey, band);
                }
            }
        }
        return userCollection;
    }
}