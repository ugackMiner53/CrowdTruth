package edu.ncsu.hacknc;

import java.net.URI;
import java.net.URISyntaxException;

import com.sun.net.httpserver.HttpExchange;

public final class SecurityUtil {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_COMMENT_LENGTH = 5000;
    private static final int MAX_URL_LENGTH = 2048;
    private static final long TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private SecurityUtil() {
    }

    public static void addSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().add("Content-Security-Policy", "default-src 'none'");
    }

    public static String sanitizeInput(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    public static String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "Title is required";
        }
        String sanitized = sanitizeInput(title, MAX_TITLE_LENGTH);
        if (sanitized.length() < 3) {
            return "Title must be at least 3 characters";
        }
        return null;
    }

    public static String validateComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return "Comment is required";
        }
        String sanitized = sanitizeInput(comment, MAX_COMMENT_LENGTH);
        if (sanitized.length() < 10) {
            return "Comment must be at least 10 characters";
        }
        return null;
    }

    public static String validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "URL is required";
        }
        if (url.length() > MAX_URL_LENGTH) {
            return "URL is too long";
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return "URL must be HTTP or HTTPS";
            }
        } catch (URISyntaxException e) {
            return "Invalid URL format";
        }
        return null;
    }

    public static boolean isTokenExpired(long createdAt) {
        return System.currentTimeMillis() - createdAt > TOKEN_EXPIRY_MS;
    }

    public static long getTokenExpiryMs() {
        return TOKEN_EXPIRY_MS;
    }
}
