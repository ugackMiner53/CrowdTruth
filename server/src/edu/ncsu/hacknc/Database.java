package edu.ncsu.hacknc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String DB_URL = "jdbc:sqlite:crowdtruth.db";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void init() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");

                        boolean hasLegacyPassword = hasColumn(conn, "users", "password");
                        boolean hasHash = hasColumn(conn, "users", "password_hash");
                        if (hasLegacyPassword && !hasHash) {
                                stmt.execute("DROP TABLE users");
                        }

                        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                                        "id TEXT PRIMARY KEY, " +
                                        "email TEXT UNIQUE NOT NULL, " +
                                        "password_hash TEXT NOT NULL, " +
                                        "password_salt TEXT NOT NULL)");

                        tryAddColumn(stmt, "users", "password_hash TEXT");
                        tryAddColumn(stmt, "users", "password_salt TEXT");

            stmt.execute("CREATE TABLE IF NOT EXISTS sources (" +
                    "id TEXT PRIMARY KEY, " +
                    "url TEXT UNIQUE NOT NULL, " +
                    "title TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS posts (" +
                    "id TEXT PRIMARY KEY, " +
                    "source_id TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "comment TEXT NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "FOREIGN KEY(source_id) REFERENCES sources(id), " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS votes (" +
                    "id TEXT PRIMARY KEY, " +
                    "post_id TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "agree INTEGER NOT NULL, " +
                    "rating INTEGER NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "UNIQUE(post_id, user_id), " +
                    "FOREIGN KEY(post_id) REFERENCES posts(id), " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS tokens (" +
                    "token TEXT PRIMARY KEY, " +
                    "user_id TEXT NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sources_url ON sources(url)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_posts_source ON posts(source_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_votes_post ON votes(post_id)");
        }
    }

        private static void tryAddColumn(Statement stmt, String table, String columnDef) {
                try {
                        stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + columnDef);
                } catch (SQLException e) {
                        // Ignore: column already exists or table is new.
                }
        }

        private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
                try (Statement stmt = conn.createStatement();
                                var rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
                        while (rs.next()) {
                                String name = rs.getString("name");
                                if (column.equalsIgnoreCase(name)) {
                                        return true;
                                }
                        }
                }
                return false;
        }
}