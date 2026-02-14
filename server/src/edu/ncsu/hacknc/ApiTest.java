package edu.ncsu.hacknc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;

public class ApiTest {

	private static final String BASE = "http://localhost:8080";

	public static void main(String[] args) throws Exception {
		Database.init();

		TestUser user = seedUser();
		String token = login(user.email, user.password);

		String sourceId = createSource("https://example.com/article", "Example Article");
		String postId = createPost(token, sourceId, "My take", "This seems reliable.");
		vote(token, postId, true, 4);

		String sourceJson = getSourceById(sourceId);
		assertContains(sourceJson, "\"sourceId\"");

		System.out.println("API tests passed");
	}

	private static TestUser seedUser() throws Exception {
		String id = UUID.randomUUID().toString();
		String email = "user" + System.currentTimeMillis() + "@example.com";
		String password = "Test!1a";

		try (Connection conn = Database.getConnection();
				PreparedStatement find = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
			find.setString(1, email);
			try (ResultSet rs = find.executeQuery()) {
				if (rs.next()) {
					return new TestUser(rs.getString("id"), email, password);
				}
			}
		}

		PasswordUtil.HashedPassword hashed = PasswordUtil.hashPassword(password);
		try (Connection conn = Database.getConnection();
				PreparedStatement insert = conn.prepareStatement(
						"INSERT INTO users (id, email, password_hash, password_salt) VALUES (?, ?, ?, ?)")) {
			insert.setString(1, id);
			insert.setString(2, email);
			insert.setString(3, hashed.getHashHex());
			insert.setString(4, hashed.getSaltHex());
			insert.executeUpdate();
		}
		return new TestUser(id, email, password);
	}

	private static String login(String email, String password) throws Exception {
		String body = "{\"email\":" + JsonUtil.quote(email) + ",\"password\":" + JsonUtil.quote(password) + "}";
		String response = postJson("/auth/login", body, null);
		Map<String, String> data = JsonUtil.parseObject(response);
		String token = data.get("token");
		if (token == null) {
			throw new IllegalStateException("Login failed: " + response);
		}
		return token;
	}

	private static String createSource(String url, String title) throws Exception {
		String body = "{\"url\":" + JsonUtil.quote(url) + ",\"title\":" + JsonUtil.quote(title) + "}";
		String response = postJson("/sources", body, null);
		Map<String, String> data = JsonUtil.parseObject(response);
		String sourceId = data.get("sourceId");
		if (sourceId == null) {
			throw new IllegalStateException("Create source failed: " + response);
		}
		return sourceId;
	}

	private static String createPost(String token, String sourceId, String title, String comment) throws Exception {
		String body = "{\"sourceId\":" + JsonUtil.quote(sourceId) +
				",\"title\":" + JsonUtil.quote(title) +
				",\"comment\":" + JsonUtil.quote(comment) + "}";
		String response = postJson("/posts", body, token);
		Map<String, String> data = JsonUtil.parseObject(response);
		String postId = data.get("postId");
		if (postId == null) {
			throw new IllegalStateException("Create post failed: " + response);
		}
		return postId;
	}

	private static void vote(String token, String postId, boolean agree, int rating) throws Exception {
		String body = "{\"postId\":" + JsonUtil.quote(postId) +
				",\"agree\":" + (agree ? "true" : "false") +
				",\"rating\":" + rating + "}";
		String response = postJson("/votes", body, token);
		if (!response.contains("\"ok\":true")) {
			throw new IllegalStateException("Vote failed: " + response);
		}
	}

	private static String getSourceById(String sourceId) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/sources/" + sourceId))
				.GET()
				.build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IllegalStateException("Get source failed: " + response.body());
		}
		return response.body();
	}

	private static String postJson(String path, String body, String token) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(BASE + path))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.header("Content-Type", "application/json");

		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}

		HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(),
				HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() >= 400) {
			throw new IllegalStateException("Request failed: " + response.body());
		}
		return response.body();
	}

	private static void assertContains(String text, String expected) {
		if (text == null || !text.contains(expected)) {
			throw new IllegalStateException("Missing expected content: " + expected);
		}
	}

	private static final class TestUser {
		private final String id;
		private final String email;
		private final String password;

		private TestUser(String id, String email, String password) {
			this.id = id;
			this.email = email;
			this.password = password;
		}
	}
}
