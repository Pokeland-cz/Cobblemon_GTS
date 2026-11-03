package org.pokesplash.gts.web;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time login codes issued in game and HMAC-signed session cookies for the website.
 */
public class WebAuth {
	public static final String COOKIE = "gts_session";
	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // No 0/O, 1/I.
	private static final int CODE_LENGTH = 8;

	public record Session(UUID uuid, String name, long expiresAt) {}

	private record PendingLogin(UUID uuid, String name, long expiresAt) {}

	private final SecureRandom random = new SecureRandom();
	private final Map<String, PendingLogin> codes = new ConcurrentHashMap<>();

	/**
	 * Creates a one-time login code for a player, replacing any older code of theirs.
	 */
	public String createCode(UUID uuid, String name, int validMinutes) {
		long now = System.currentTimeMillis();
		codes.values().removeIf(p -> p.uuid().equals(uuid) || p.expiresAt() < now);

		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
		}
		codes.put(code.toString(), new PendingLogin(uuid, name, now + validMinutes * 60_000L));
		return code.toString();
	}

	/**
	 * Uses up a login code.
	 * @return The new session, or null if the code is unknown or expired.
	 */
	public Session redeem(String code, int sessionHours) {
		if (code == null) {
			return null;
		}
		PendingLogin login = codes.remove(code.trim().toUpperCase(Locale.ROOT));
		if (login == null || login.expiresAt() < System.currentTimeMillis()) {
			return null;
		}
		return new Session(login.uuid(), login.name(),
				System.currentTimeMillis() + sessionHours * 3_600_000L);
	}

	/**
	 * @return Cookie value: base64url(uuid|expiresAt|name).base64url(hmac)
	 */
	public String encode(Session session, byte[] secret) {
		String payload = session.uuid() + "|" + session.expiresAt() + "|" + session.name();
		String encoded = base64(payload.getBytes(StandardCharsets.UTF_8));
		return encoded + "." + base64(hmac(encoded, secret));
	}

	/**
	 * @return The session in a cookie value, or null if it is forged, malformed or expired.
	 */
	public Session decode(String value, byte[] secret) {
		if (value == null) {
			return null;
		}
		int dot = value.indexOf('.');
		if (dot <= 0) {
			return null;
		}
		try {
			String encoded = value.substring(0, dot);
			byte[] signature = Base64.getUrlDecoder().decode(value.substring(dot + 1));
			if (!MessageDigest.isEqual(signature, hmac(encoded, secret))) {
				return null;
			}
			String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
					.split("\\|", 3);
			Session session = new Session(UUID.fromString(parts[0]), parts[2], Long.parseLong(parts[1]));
			return session.expiresAt() < System.currentTimeMillis() ? null : session;
		} catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	private static byte[] hmac(String data, byte[] secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IllegalStateException("HmacSHA256 is not available", e);
		}
	}

	private static String base64(byte[] data) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}
}
