package edu.ncsu.hacknc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Database.init();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/auth/register", new RegisterHandler());
        server.createContext("/auth/login", new LoginHandler());
        server.createContext("/sources", new SourcesHandler());
        server.createContext("/posts", new PostsHandler());
        server.createContext("/votes", new VotesHandler());
        server.createContext("/users", new UsersHandler());
        server.createContext("/search", new SearchHandler());
        server.createContext("/stats", new StatsHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("CrowdTruth API listening on http://localhost:" + PORT);
    }

    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
                return;
            }

            String body = HttpUtil.readBody(exchange);
            Map<String, String> data = JsonUtil.parseObject(body);

            String id = data.get("id");
            String email = data.get("email");
            String password = data.get("password");

            try {
                new Account(id, email, password);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error(e.getMessage()));
                return;
            }

            PasswordUtil.HashedPassword hashed = PasswordUtil.hashPassword(password);

            try (Connection conn = Database.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO users (id, email, password_hash, password_salt) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, id);
                stmt.setString(2, email);
                stmt.setString(3, hashed.getHashHex());
                stmt.setString(4, hashed.getSaltHex());
                stmt.executeUpdate();
                HttpUtil.sendJson(exchange, 201,
                        "{\"ok\":true,\"userId\":" + JsonUtil.quote(id) + "}");
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 409, JsonUtil.error("Registration failed. Email may already be in use."));
            }
        }
    }

    private static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
                return;
            }

            String body = HttpUtil.readBody(exchange);
            Map<String, String> data = JsonUtil.parseObject(body);

            String email = data.get("email");
            String password = data.get("password");

            if (email == null || password == null) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error("Missing email or password"));
                return;
            }

            try (Connection conn = Database.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT id, password_hash, password_salt FROM users WHERE email = ?")) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        HttpUtil.sendJson(exchange, 401, JsonUtil.error("Invalid credentials"));
                        return;
                    }
                    String hash = rs.getString("password_hash");
                    String salt = rs.getString("password_salt");
                    if (hash == null || salt == null || !PasswordUtil.verifyPassword(password, salt, hash)) {
                        HttpUtil.sendJson(exchange, 401, JsonUtil.error("Invalid credentials"));
                        return;
                    }
                    String userId = rs.getString("id");
                    String token = UUID.randomUUID().toString();

                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO tokens (token, user_id, created_at) VALUES (?, ?, ?)")) {
                        insert.setString(1, token);
                        insert.setString(2, userId);
                        insert.setLong(3, Instant.now().toEpochMilli());
                        insert.executeUpdate();
                    }

                    HttpUtil.sendJson(exchange, 200,
                            "{\"ok\":true,\"token\":" + JsonUtil.quote(token) + ",\"userId\":" + JsonUtil.quote(userId) + "}");
                }
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }
    }

    private static class SourcesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            Map<String, String> query = HttpUtil.parseQuery(uri.getRawQuery());

            if ("GET".equalsIgnoreCase(method)) {
                String sourceId = null;
                if (path.startsWith("/sources/")) {
                    sourceId = path.substring("/sources/".length());
                }
                String url = query.get("url");
                if (sourceId == null && url == null) {
                    HttpUtil.sendJson(exchange, 400, JsonUtil.error("Missing source id or url"));
                    return;
                }
                handleGetSource(exchange, sourceId, url);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                String body = HttpUtil.readBody(exchange);
                Map<String, String> data = JsonUtil.parseObject(body);
                String url = data.get("url");
                String title = data.get("title");
                
                String urlError = SecurityUtil.validateUrl(url);
                if (urlError != null) {
                    HttpUtil.sendJson(exchange, 400, JsonUtil.error(urlError));
                    return;
                }
                
                if (title != null) {
                    title = SecurityUtil.sanitizeInput(title, 200);
                }
                
                try (Connection conn = Database.getConnection()) {
                    String sourceId = upsertSource(conn, url, title);
                    HttpUtil.sendJson(exchange, 201,
                            "{\"ok\":true,\"sourceId\":" + JsonUtil.quote(sourceId) + "}");
                } catch (Exception e) {
                    HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
                }
                return;
            }

            HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
        }

        private void handleGetSource(HttpExchange exchange, String sourceId, String url) throws IOException {
            try (Connection conn = Database.getConnection()) {
                if (sourceId == null) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT id FROM sources WHERE url = ?")) {
                        stmt.setString(1, url);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next()) {
                                HttpUtil.sendJson(exchange, 404, JsonUtil.error("Source not found"));
                                return;
                            }
                            sourceId = rs.getString("id");
                        }
                    }
                }

                String json = buildSourceJson(conn, sourceId);
                if (json == null) {
                    HttpUtil.sendJson(exchange, 404, JsonUtil.error("Source not found"));
                    return;
                }
                HttpUtil.sendJson(exchange, 200, json);
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }
    }

    private static class PostsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
                return;
            }

            String userId = requireAuth(exchange);
            if (userId == null) {
                return;
            }

            String body = HttpUtil.readBody(exchange);
            Map<String, String> data = JsonUtil.parseObject(body);

            String sourceId = data.get("sourceId");
            String url = data.get("url");
            String title = data.get("title");
            String comment = data.get("comment");

            if ((sourceId == null || sourceId.isEmpty()) && (url == null || url.isEmpty())) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error("Missing sourceId or url"));
                return;
            }
            
            String titleError = SecurityUtil.validateTitle(title);
            if (titleError != null) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error(titleError));
                return;
            }
            
            String commentError = SecurityUtil.validateComment(comment);
            if (commentError != null) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error(commentError));
                return;
            }
            
            if (url != null) {
                String urlError = SecurityUtil.validateUrl(url);
                if (urlError != null) {
                    HttpUtil.sendJson(exchange, 400, JsonUtil.error(urlError));
                    return;
                }
            }
            
            title = SecurityUtil.sanitizeInput(title, 200);
            comment = SecurityUtil.sanitizeInput(comment, 5000);

            try (Connection conn = Database.getConnection()) {
                if (sourceId == null || sourceId.isEmpty()) {
                    sourceId = upsertSource(conn, url, title);
                }
                long createdAt = Instant.now().toEpochMilli();
                String postId = UUID.randomUUID().toString();
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO posts (id, source_id, user_id, title, comment, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, postId);
                    stmt.setString(2, sourceId);
                    stmt.setString(3, userId);
                    stmt.setString(4, title);
                    stmt.setString(5, comment);
                    stmt.setLong(6, createdAt);
                    stmt.executeUpdate();
                }

                String sourceUrl = url;
                String sourceTitle = null;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT url, title FROM sources WHERE id = ?")) {
                    stmt.setString(1, sourceId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            sourceUrl = rs.getString("url");
                            sourceTitle = rs.getString("title");
                        }
                    }
                }

                HttpUtil.sendJson(exchange, 201,
                        "{\"ok\":true,\"postId\":" + JsonUtil.quote(postId) +
                        ",\"sourceId\":" + JsonUtil.quote(sourceId) +
                        ",\"userId\":" + JsonUtil.quote(userId) +
                        ",\"title\":" + JsonUtil.quote(title) +
                        ",\"comment\":" + JsonUtil.quote(comment) +
                        ",\"createdAt\":" + createdAt +
                        ",\"sourceUrl\":" + JsonUtil.quote(sourceUrl) +
                        ",\"sourceTitle\":" + JsonUtil.quote(sourceTitle) + "}");
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }
    }

    private static class VotesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
                return;
            }

            String userId = requireAuth(exchange);
            if (userId == null) {
                return;
            }

            String body = HttpUtil.readBody(exchange);
            Map<String, String> data = JsonUtil.parseObject(body);

            String postId = data.get("postId");
            String agreeRaw = data.get("agree");
            String ratingRaw = data.get("rating");

            if (postId == null || agreeRaw == null || ratingRaw == null) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error("Missing postId, agree, or rating"));
                return;
            }

            boolean agree = JsonUtil.parseBoolean(agreeRaw);
            Integer rating = JsonUtil.parseInt(ratingRaw);

            if (rating == null || rating < 0 || rating > 5) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error("Rating must be 0 to 5"));
                return;
            }

            try (Connection conn = Database.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO votes (id, post_id, user_id, agree, rating, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, postId);
                stmt.setString(3, userId);
                stmt.setInt(4, agree ? 1 : 0);
                stmt.setInt(5, rating);
                stmt.setLong(6, Instant.now().toEpochMilli());
                stmt.executeUpdate();
                HttpUtil.sendJson(exchange, 201, "{\"ok\":true}");
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 409, JsonUtil.error("Already voted or invalid post"));
            }
        }
    }

    private static String requireAuth(HttpExchange exchange) throws IOException {
        String token = HttpUtil.extractBearerToken(exchange);
        if (token == null) {
            HttpUtil.sendJson(exchange, 401, JsonUtil.error("Missing auth token"));
            return null;
        }
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT user_id, created_at FROM tokens WHERE token = ?")) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    HttpUtil.sendJson(exchange, 401, JsonUtil.error("Invalid token"));
                    return null;
                }
                long createdAt = rs.getLong("created_at");
                if (SecurityUtil.isTokenExpired(createdAt)) {
                    HttpUtil.sendJson(exchange, 401, JsonUtil.error("Token expired"));
                    return null;
                }
                return rs.getString("user_id");
            }
        } catch (Exception e) {
            HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            return null;
        }
    }

    private static String upsertSource(Connection conn, String url, String title) throws Exception {
        try (PreparedStatement find = conn.prepareStatement(
                "SELECT id, title FROM sources WHERE url = ?")) {
            find.setString(1, url);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    String existingId = rs.getString("id");
                    String existingTitle = rs.getString("title");
                    if (title != null && !title.isEmpty() && (existingTitle == null || existingTitle.isEmpty())) {
                        try (PreparedStatement update = conn.prepareStatement(
                                "UPDATE sources SET title = ? WHERE id = ?")) {
                            update.setString(1, title);
                            update.setString(2, existingId);
                            update.executeUpdate();
                        }
                    }
                    return existingId;
                }
            }
        }

        String id = UUID.randomUUID().toString();
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO sources (id, url, title) VALUES (?, ?, ?)")) {
            insert.setString(1, id);
            insert.setString(2, url);
            insert.setString(3, title);
            insert.executeUpdate();
        }
        return id;
    }

    private static String buildSourceJson(Connection conn, String sourceId) throws Exception {
        String url;
        String title;

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT url, title FROM sources WHERE id = ?")) {
            stmt.setString(1, sourceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                url = rs.getString("url");
                title = rs.getString("title");
            }
        }

        double reputation = 0.0;
        int agreeCount = 0;
        int disagreeCount = 0;
        int postCount = 0;

        try (PreparedStatement stats = conn.prepareStatement(
                "SELECT AVG(v.rating) AS avg_rating, " +
                "SUM(CASE WHEN v.agree = 1 THEN 1 ELSE 0 END) AS agree_count, " +
                "SUM(CASE WHEN v.agree = 0 THEN 1 ELSE 0 END) AS disagree_count " +
                "FROM votes v JOIN posts p ON p.id = v.post_id WHERE p.source_id = ?")) {
            stats.setString(1, sourceId);
            try (ResultSet rs = stats.executeQuery()) {
                if (rs.next()) {
                    reputation = rs.getDouble("avg_rating");
                    agreeCount = rs.getInt("agree_count");
                    disagreeCount = rs.getInt("disagree_count");
                }
            }
        }

        try (PreparedStatement count = conn.prepareStatement(
                "SELECT COUNT(*) AS post_count FROM posts WHERE source_id = ?")) {
            count.setString(1, sourceId);
            try (ResultSet rs = count.executeQuery()) {
                if (rs.next()) {
                    postCount = rs.getInt("post_count");
                }
            }
        }

        StringBuilder postsJson = new StringBuilder();
        postsJson.append("[");
        boolean first = true;
        try (PreparedStatement posts = conn.prepareStatement(
                "SELECT p.id, p.title, p.comment, p.user_id, p.created_at, " +
                "AVG(v.rating) AS avg_rating, " +
                "SUM(CASE WHEN v.agree = 1 THEN 1 ELSE 0 END) AS agree_count, " +
                "SUM(CASE WHEN v.agree = 0 THEN 1 ELSE 0 END) AS disagree_count " +
                "FROM posts p LEFT JOIN votes v ON v.post_id = p.id " +
                "WHERE p.source_id = ? " +
                "GROUP BY p.id ORDER BY p.created_at DESC")) {
            posts.setString(1, sourceId);
            try (ResultSet rs = posts.executeQuery()) {
                while (rs.next()) {
                    if (!first) {
                        postsJson.append(",");
                    }
                    first = false;
                    postsJson.append("{");
                    postsJson.append("\"postId\":").append(JsonUtil.quote(rs.getString("id"))).append(",");
                    postsJson.append("\"title\":").append(JsonUtil.quote(rs.getString("title"))).append(",");
                    postsJson.append("\"comment\":").append(JsonUtil.quote(rs.getString("comment"))).append(",");
                    postsJson.append("\"userId\":").append(JsonUtil.quote(rs.getString("user_id"))).append(",");
                    postsJson.append("\"createdAt\":").append(rs.getLong("created_at")).append(",");
                    postsJson.append("\"rating\":").append(rs.getDouble("avg_rating")).append(",");
                    postsJson.append("\"agreeCount\":").append(rs.getInt("agree_count")).append(",");
                    postsJson.append("\"disagreeCount\":").append(rs.getInt("disagree_count"));
                    postsJson.append("}");
                }
            }
        }
        postsJson.append("]");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"sourceId\":").append(JsonUtil.quote(sourceId)).append(",");
        json.append("\"url\":").append(JsonUtil.quote(url)).append(",");
        json.append("\"title\":").append(JsonUtil.quote(title)).append(",");
        json.append("\"reputation\":").append(reputation).append(",");
        json.append("\"agreeCount\":").append(agreeCount).append(",");
        json.append("\"disagreeCount\":").append(disagreeCount).append(",");
        json.append("\"postCount\":").append(postCount).append(",");
        json.append("\"posts\":").append(postsJson);
        json.append("}");
        return json.toString();
    }

    private static class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();

            if ("GET".equalsIgnoreCase(method)) {
                String[] parts = path.split("/");
                if (parts.length < 3) {
                    HttpUtil.sendJson(exchange, 400, JsonUtil.error("Invalid path"));
                    return;
                }

                String userId = parts[2];
                if (parts.length >= 4 && "posts".equals(parts[3])) {
                    handleGetUserPosts(exchange, userId);
                } else if (parts.length >= 4 && "stats".equals(parts[3])) {
                    handleGetUserStats(exchange, userId);
                } else {
                    handleGetUserProfile(exchange, userId);
                }
                return;
            }

            HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
        }

        private void handleGetUserPosts(HttpExchange exchange, String userId) throws IOException {
            try (Connection conn = Database.getConnection()) {
                Map<String, String> query = HttpUtil.parseQuery(exchange.getRequestURI().getRawQuery());
                int limit = JsonUtil.parseInt(query.getOrDefault("limit", "50"));
                int offset = JsonUtil.parseInt(query.getOrDefault("offset", "0"));
                
                if (limit > 100) limit = 100;

                StringBuilder postsJson = new StringBuilder();
                postsJson.append("[");
                boolean first = true;

                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT p.id, p.source_id, p.title, p.comment, p.created_at, " +
                        "s.url, s.title AS source_title, " +
                        "AVG(v.rating) AS avg_rating, " +
                        "SUM(CASE WHEN v.agree = 1 THEN 1 ELSE 0 END) AS agree_count, " +
                        "SUM(CASE WHEN v.agree = 0 THEN 1 ELSE 0 END) AS disagree_count " +
                        "FROM posts p " +
                        "LEFT JOIN sources s ON s.id = p.source_id " +
                        "LEFT JOIN votes v ON v.post_id = p.id " +
                        "WHERE p.user_id = ? " +
                        "GROUP BY p.id " +
                        "ORDER BY p.created_at DESC " +
                        "LIMIT ? OFFSET ?")) {
                    stmt.setString(1, userId);
                    stmt.setInt(2, limit);
                    stmt.setInt(3, offset);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            if (!first) postsJson.append(",");
                            first = false;
                            postsJson.append("{");
                            postsJson.append("\"postId\":").append(JsonUtil.quote(rs.getString("id"))).append(",");
                            postsJson.append("\"sourceId\":").append(JsonUtil.quote(rs.getString("source_id"))).append(",");
                            postsJson.append("\"sourceUrl\":").append(JsonUtil.quote(rs.getString("url"))).append(",");
                            postsJson.append("\"sourceTitle\":").append(JsonUtil.quote(rs.getString("source_title"))).append(",");
                            postsJson.append("\"title\":").append(JsonUtil.quote(rs.getString("title"))).append(",");
                            postsJson.append("\"comment\":").append(JsonUtil.quote(rs.getString("comment"))).append(",");
                            postsJson.append("\"createdAt\":").append(rs.getLong("created_at")).append(",");
                            postsJson.append("\"rating\":").append(rs.getDouble("avg_rating")).append(",");
                            postsJson.append("\"agreeCount\":").append(rs.getInt("agree_count")).append(",");
                            postsJson.append("\"disagreeCount\":").append(rs.getInt("disagree_count"));
                            postsJson.append("}");
                        }
                    }
                }
                postsJson.append("]");

                HttpUtil.sendJson(exchange, 200, 
                    "{\"ok\":true,\"userId\":" + JsonUtil.quote(userId) + 
                    ",\"posts\":" + postsJson + "}");
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }

        private void handleGetUserStats(HttpExchange exchange, String userId) throws IOException {
            try (Connection conn = Database.getConnection()) {
                int postCount = 0;
                int voteCount = 0;

                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) AS count FROM posts WHERE user_id = ?")) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) postCount = rs.getInt("count");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) AS count FROM votes WHERE user_id = ?")) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) voteCount = rs.getInt("count");
                    }
                }

                String json = "{\"ok\":true," +
                    "\"userId\":" + JsonUtil.quote(userId) + "," +
                    "\"postCount\":" + postCount + "," +
                    "\"voteCount\":" + voteCount + "}";
                HttpUtil.sendJson(exchange, 200, json);
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }

        private void handleGetUserProfile(HttpExchange exchange, String userId) throws IOException {
            try (Connection conn = Database.getConnection()) {
                String email = null;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT email FROM users WHERE id = ?")) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            HttpUtil.sendJson(exchange, 404, JsonUtil.error("User not found"));
                            return;
                        }
                        email = rs.getString("email");
                    }
                }

                String json = "{\"ok\":true," +
                    "\"userId\":" + JsonUtil.quote(userId) + "," +
                    "\"email\":" + JsonUtil.quote(email) + "}";
                HttpUtil.sendJson(exchange, 200, json);
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }
    }

    private static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
                return;
            }

            Map<String, String> query = HttpUtil.parseQuery(exchange.getRequestURI().getRawQuery());
            String searchQuery = query.get("q");
            String type = query.getOrDefault("type", "posts");
            int limit = Math.min(JsonUtil.parseInt(query.getOrDefault("limit", "20")), 50);

            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                HttpUtil.sendJson(exchange, 400, JsonUtil.error("Missing search query"));
                return;
            }

            try (Connection conn = Database.getConnection()) {
                if ("sources".equals(type)) {
                    handleSearchSources(exchange, conn, searchQuery, limit);
                } else {
                    handleSearchPosts(exchange, conn, searchQuery, limit);
                }
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }

        private void handleSearchPosts(HttpExchange exchange, Connection conn, String searchQuery, int limit) 
                throws Exception {
            StringBuilder resultsJson = new StringBuilder();
            resultsJson.append("[");
            boolean first = true;

            String pattern = "%" + searchQuery.toLowerCase() + "%";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT p.id, p.title, p.comment, p.user_id, p.created_at, s.url, s.title AS source_title " +
                    "FROM posts p " +
                    "LEFT JOIN sources s ON s.id = p.source_id " +
                    "WHERE LOWER(p.title) LIKE ? OR LOWER(p.comment) LIKE ? " +
                    "ORDER BY p.created_at DESC LIMIT ?")) {
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                stmt.setInt(3, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (!first) resultsJson.append(",");
                        first = false;
                        resultsJson.append("{");
                        resultsJson.append("\"postId\":").append(JsonUtil.quote(rs.getString("id"))).append(",");
                        resultsJson.append("\"title\":").append(JsonUtil.quote(rs.getString("title"))).append(",");
                        resultsJson.append("\"comment\":").append(JsonUtil.quote(rs.getString("comment"))).append(",");
                        resultsJson.append("\"userId\":").append(JsonUtil.quote(rs.getString("user_id"))).append(",");
                        resultsJson.append("\"sourceUrl\":").append(JsonUtil.quote(rs.getString("url"))).append(",");
                        resultsJson.append("\"sourceTitle\":").append(JsonUtil.quote(rs.getString("source_title"))).append(",");
                        resultsJson.append("\"createdAt\":").append(rs.getLong("created_at"));
                        resultsJson.append("}");
                    }
                }
            }
            resultsJson.append("]");

            HttpUtil.sendJson(exchange, 200, 
                "{\"ok\":true,\"type\":\"posts\",\"results\":" + resultsJson + "}");
        }

        private void handleSearchSources(HttpExchange exchange, Connection conn, String searchQuery, int limit) 
                throws Exception {
            StringBuilder resultsJson = new StringBuilder();
            resultsJson.append("[");
            boolean first = true;

            String pattern = "%" + searchQuery.toLowerCase() + "%";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, url, title FROM sources " +
                    "WHERE LOWER(url) LIKE ? OR LOWER(title) LIKE ? " +
                    "LIMIT ?")) {
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                stmt.setInt(3, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (!first) resultsJson.append(",");
                        first = false;
                        resultsJson.append("{");
                        resultsJson.append("\"sourceId\":").append(JsonUtil.quote(rs.getString("id"))).append(",");
                        resultsJson.append("\"url\":").append(JsonUtil.quote(rs.getString("url"))).append(",");
                        resultsJson.append("\"title\":").append(JsonUtil.quote(rs.getString("title")));
                        resultsJson.append("}");
                    }
                }
            }
            resultsJson.append("]");

            HttpUtil.sendJson(exchange, 200, 
                "{\"ok\":true,\"type\":\"sources\",\"results\":" + resultsJson + "}");
        }
    }

    private static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendJson(exchange, 405, JsonUtil.error("Method not allowed"));
                return;
            }

            try (Connection conn = Database.getConnection()) {
                int totalUsers = 0;
                int totalSources = 0;
                int totalPosts = 0;
                int totalVotes = 0;

                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM users")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) totalUsers = rs.getInt("count");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM sources")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) totalSources = rs.getInt("count");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM posts")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) totalPosts = rs.getInt("count");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM votes")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) totalVotes = rs.getInt("count");
                    }
                }

                String json = "{\"ok\":true," +
                    "\"totalUsers\":" + totalUsers + "," +
                    "\"totalSources\":" + totalSources + "," +
                    "\"totalPosts\":" + totalPosts + "," +
                    "\"totalVotes\":" + totalVotes + "}";
                HttpUtil.sendJson(exchange, 200, json);
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500, JsonUtil.error("Server error"));
            }
        }
    }
}
