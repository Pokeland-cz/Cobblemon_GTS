package org.pokesplash.gts.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.pokesplash.gts.Gts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and atomically writes the website's JSON data files in config/gts/web/data.
 */
final class JsonFiles {
	static final Path DATA_DIR = Path.of("config", "gts", "web", "data");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private JsonFiles() {}

	/**
	 * @return The parsed file, or the fallback if it is missing or unreadable.
	 */
	static <T> T read(String file, TypeToken<T> type, T fallback) {
		Path path = DATA_DIR.resolve(file);
		if (!Files.exists(path)) {
			return fallback;
		}
		try {
			T value = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), type.getType());
			return value == null ? fallback : value;
		} catch (Exception e) {
			Gts.LOGGER.error("Could not read " + path + ": " + e.getMessage());
			return fallback;
		}
	}

	/**
	 * Writes through a temporary file, so a crash never leaves a half-written file.
	 */
	static void write(String file, Object data) {
		try {
			Files.createDirectories(DATA_DIR);
			Path target = DATA_DIR.resolve(file);
			Path temp = DATA_DIR.resolve(file + ".tmp");
			Files.writeString(temp, GSON.toJson(data), StandardCharsets.UTF_8);
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			Gts.LOGGER.error("Could not write " + file + " of the GTS website: " + e.getMessage());
		}
	}
}
