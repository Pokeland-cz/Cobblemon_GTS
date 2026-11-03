package org.pokesplash.gts.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Small helpers around {@link HttpExchange}.
 */
final class Http {
	static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	private static final int MAX_BODY = 16 * 1024;

	private Http() {}

	static void addCommonHeaders(HttpExchange exchange) {
		exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
		exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
		exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
	}

	static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
		addCommonHeaders(exchange);
		if (contentType != null) {
			exchange.getResponseHeaders().set("Content-Type", contentType);
		}
		boolean head = "HEAD".equals(exchange.getRequestMethod());
		exchange.sendResponseHeaders(status, head || body.length == 0 ? -1 : body.length);
		if (!head && body.length > 0) {
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		}
		exchange.close();
	}

	static void json(HttpExchange exchange, int status, Object body) throws IOException {
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		send(exchange, status, "application/json; charset=utf-8", GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
	}

	static void error(HttpExchange exchange, int status, String code) throws IOException {
		json(exchange, status, Map.of("error", code));
	}

	/**
	 * Reads a JSON object body of at most 16 KiB.
	 * @return The object, or null if the body is too large or not a JSON object.
	 */
	static JsonObject readJson(HttpExchange exchange) throws IOException {
		try (InputStream in = exchange.getRequestBody()) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int read;
			while ((read = in.read(chunk)) != -1) {
				buffer.write(chunk, 0, read);
				if (buffer.size() > MAX_BODY) {
					return null;
				}
			}
			var element = JsonParser.parseString(buffer.toString(StandardCharsets.UTF_8));
			return element.isJsonObject() ? element.getAsJsonObject() : null;
		} catch (RuntimeException e) {
			return null;
		}
	}

	static String getString(JsonObject json, String key) {
		if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
			return null;
		}
		return json.get(key).getAsString();
	}

	static String getCookie(HttpExchange exchange, String name) {
		for (String header : exchange.getRequestHeaders().getOrDefault("Cookie", java.util.List.of())) {
			for (String part : header.split(";")) {
				int eq = part.indexOf('=');
				if (eq > 0 && part.substring(0, eq).trim().equals(name)) {
					return part.substring(eq + 1).trim();
				}
			}
		}
		return null;
	}

	static void setCookie(HttpExchange exchange, String name, String value, long maxAgeSeconds, boolean secure) {
		exchange.getResponseHeaders().add("Set-Cookie", name + "=" + value + "; Path=/; Max-Age=" + maxAgeSeconds
				+ "; HttpOnly; SameSite=Lax" + (secure ? "; Secure" : ""));
	}

	/**
	 * @return The client's IP, taken from X-Forwarded-For only when trusted.
	 */
	static String clientIp(HttpExchange exchange, boolean trustProxy) {
		if (trustProxy) {
			String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				return forwarded.split(",")[0].trim();
			}
		}
		return exchange.getRemoteAddress().getAddress().getHostAddress();
	}

	static boolean isJsonRequest(HttpExchange exchange) {
		String type = exchange.getRequestHeaders().getFirst("Content-Type");
		return type != null && type.toLowerCase().startsWith("application/json");
	}
}
