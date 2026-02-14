package edu.ncsu.hacknc;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtil {

    private static final Pattern PAIR = Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*(\"(?:\\\\.|[^\"])*\"|true|false|-?\\d+(?:\\.\\d+)?)");

    private JsonUtil() {
    }

    // Minimal flat JSON object parser: {"key":"value","n":1,"b":true}
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) {
            return map;
        }
        Matcher matcher = PAIR.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String raw = matcher.group(2);
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                map.put(key, unquote(raw));
            } else {
                map.put(key, raw);
            }
        }
        return map;
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '\\':
                sb.append("\\\\");
                break;
            case '"':
                sb.append("\\\"");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    public static String error(String message) {
        return "{\"ok\":false,\"error\":" + quote(message) + "}";
    }

    public static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean parseBoolean(String raw) {
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    private static String unquote(String raw) {
        String s = raw.substring(1, raw.length() - 1);
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}