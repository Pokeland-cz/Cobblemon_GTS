package org.pokesplash.gts.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

/**
 * Serves the website's static files. Files in config/gts/web/site override the ones bundled in the mod jar.
 */
class StaticHandler implements HttpHandler {
	static final Path OVERRIDE_DIR = Path.of("config", "gts", "web", "site");
	private static final String RESOURCE_ROOT = "/gts_web/";
	private static final Map<String, String> TYPES = Map.ofEntries(
			Map.entry("html", "text/html; charset=utf-8"),
			Map.entry("js", "text/javascript; charset=utf-8"),
			Map.entry("css", "text/css; charset=utf-8"),
			Map.entry("json", "application/json; charset=utf-8"),
			Map.entry("txt", "text/plain; charset=utf-8"),
			Map.entry("svg", "image/svg+xml"),
			Map.entry("png", "image/png"),
			Map.entry("jpg", "image/jpeg"),
			Map.entry("jpeg", "image/jpeg"),
			Map.entry("webp", "image/webp"),
			Map.entry("gif", "image/gif"),
			Map.entry("ico", "image/x-icon"),
			Map.entry("woff", "font/woff"),
			Map.entry("woff2", "font/woff2"),
			Map.entry("map", "application/json; charset=utf-8"));

	private record Asset(byte[] plain, byte[] gzip) {}

	// Bundled files never change while the server runs, so they are read and compressed once.
	// Only hits are cached, so probing random URLs can't grow the map.
	private final Map<String, Asset> bundled = new ConcurrentHashMap<>();

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String method = exchange.getRequestMethod();
			if (!method.equals("GET") && !method.equals("HEAD")) {
				exchange.getResponseHeaders().set("Allow", "GET, HEAD");
				Http.send(exchange, 405, null, new byte[0]);
				return;
			}

			String path = sanitize(exchange.getRequestURI().getPath());
			if (path == null) {
				notFound(exchange);
				return;
			}

			// Next.js export: "/" -> index.html, "/about" -> about.html
			String[] candidates = path.isEmpty() ? new String[]{"index.html"}
					: path.endsWith("/") ? new String[]{path + "index.html"}
					: path.contains(".") ? new String[]{path}
					: new String[]{path + ".html", path + "/index.html"};

			for (String candidate : candidates) {
				Asset asset = find(candidate);
				if (asset != null) {
					serve(exchange, 200, candidate, asset);
					return;
				}
			}
			notFound(exchange);
		} catch (IOException e) {
			exchange.close();
			throw e;
		}
	}

	private void notFound(HttpExchange exchange) throws IOException {
		Asset page = find("404.html");
		if (page != null) {
			serve(exchange, 404, "404.html", page);
		} else {
			Http.send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
		}
	}

	private void serve(HttpExchange exchange, int status, String path, Asset asset) throws IOException {
		String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		exchange.getResponseHeaders().set("Cache-Control", path.startsWith("_next/static/")
				? "public, max-age=31536000, immutable" : "no-cache");

		String encoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
		if (asset.gzip() != null && encoding != null && encoding.contains("gzip")) {
			exchange.getResponseHeaders().set("Content-Encoding", "gzip");
			exchange.getResponseHeaders().set("Vary", "Accept-Encoding");
			Http.send(exchange, status, TYPES.getOrDefault(extension, "application/octet-stream"), asset.gzip());
		} else {
			Http.send(exchange, status, TYPES.getOrDefault(extension, "application/octet-stream"), asset.plain());
		}
	}

	/**
	 * @return A relative path without leading slash, or null if it tries to leave the site root.
	 */
	static String sanitize(String rawPath) {
		if (rawPath == null || rawPath.indexOf('\0') >= 0 || rawPath.contains("\\")) {
			return null;
		}
		String path = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
		for (String segment : path.split("/")) {
			if (segment.equals("..") || segment.equals(".")) {
				return null;
			}
		}
		return path;
	}

	private Asset find(String path) throws IOException {
		try {
			Path override = OVERRIDE_DIR.resolve(path).normalize();
			if (override.startsWith(OVERRIDE_DIR) && Files.isRegularFile(override)) {
				return toAsset(path, Files.readAllBytes(override));
			}
		} catch (InvalidPathException e) {
			return null;
		}

		Asset cached = bundled.get(path);
		if (cached != null) {
			return cached;
		}
		try (InputStream in = StaticHandler.class.getResourceAsStream(RESOURCE_ROOT + path)) {
			if (in == null) {
				return null;
			}
			Asset asset = toAsset(path, in.readAllBytes());
			bundled.put(path, asset);
			return asset;
		}
	}

	private static Asset toAsset(String path, byte[] data) throws IOException {
		boolean compressible = path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".css")
				|| path.endsWith(".json") || path.endsWith(".svg") || path.endsWith(".txt");
		if (!compressible || data.length < 1024) {
			return new Asset(data, null);
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(data.length / 3);
		try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
			gzip.write(data);
		}
		return new Asset(data, buffer.toByteArray());
	}
}
