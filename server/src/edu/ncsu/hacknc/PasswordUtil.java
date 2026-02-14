package edu.ncsu.hacknc;

import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

	private static final int ITERATIONS = 120_000;
	private static final int KEY_LENGTH_BITS = 256;
	private static final int SALT_BYTES = 16;
	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordUtil() {
	}

	public static HashedPassword hashPassword(String password) {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		byte[] hash = pbkdf2(password, salt);
		return new HashedPassword(toHex(hash), toHex(salt));
	}

	public static boolean verifyPassword(String password, String saltHex, String hashHex) {
		if (saltHex == null || hashHex == null) {
			return false;
		}
		byte[] salt = fromHex(saltHex);
		byte[] expected = fromHex(hashHex);
		byte[] actual = pbkdf2(password, salt);
		return constantTimeEquals(expected, actual);
	}

	private static byte[] pbkdf2(String password, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			return skf.generateSecret(spec).getEncoded();
		} catch (Exception e) {
			throw new IllegalStateException("Password hashing failed", e);
		}
	}

	private static boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < a.length; i++) {
			result |= a[i] ^ b[i];
		}
		return result == 0;
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static byte[] fromHex(String hex) {
		int len = hex.length();
		byte[] out = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
		}
		return out;
	}

	public static final class HashedPassword {
		private final String hashHex;
		private final String saltHex;

		private HashedPassword(String hashHex, String saltHex) {
			this.hashHex = hashHex;
			this.saltHex = saltHex;
		}

		public String getHashHex() {
			return hashHex;
		}

		public String getSaltHex() {
			return saltHex;
		}
	}
}
