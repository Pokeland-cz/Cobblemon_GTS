package org.pokesplash.gts.web;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.util.Utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The website's WTB/WTS trade board, per-player post history and visit counter.
 * Stored as JSON in config/gts/web/data, compatible with the files of the old standalone website.
 */
public class TradeBoard {
	public static final Path DATA_DIR = Path.of("config", "gts", "web", "data");
	private static final long TRADE_LIFETIME = 72L * 60 * 60 * 1000;
	private static final int MAX_TRADES = 200;
	private static final int MAX_HISTORY = 20;
	public static final int MAX_POKEMON_LENGTH = 64;
	public static final int MAX_NOTE_LENGTH = 200;

	public static class Trade {
		public String id;
		public String author;
		public String type; // "wtb" or "wts"
		public String pokemon;
		public String note;
		public long timestamp;
	}

	public static class HistoryEntry {
		public String type;
		public String pokemon;
		public String note;

		HistoryEntry(String type, String pokemon, String note) {
			this.type = type;
			this.pokemon = pokemon;
			this.note = note;
		}
	}

	private static class Counter {
		long count;
	}

	private final Gson gson = Utils.newGson();
	private List<Trade> trades;
	private Map<String, List<HistoryEntry>> history;
	private long visits;

	public TradeBoard() {
		trades = read("trades.json", new TypeToken<List<Trade>>() {}, new ArrayList<>());
		history = read("trade_history.json", new TypeToken<LinkedHashMap<String, List<HistoryEntry>>>() {},
				new LinkedHashMap<>());
		visits = read("counter.json", new TypeToken<Counter>() {}, new Counter()).count;
	}

	public static boolean isValidType(String type) {
		return "wtb".equals(type) || "wts".equals(type);
	}

	/**
	 * @return Trades younger than 72 hours, newest first.
	 */
	public synchronized List<Trade> getTrades() {
		long now = System.currentTimeMillis();
		if (trades.removeIf(t -> now - t.timestamp >= TRADE_LIFETIME)) {
			write("trades.json", trades);
		}
		return new ArrayList<>(trades);
	}

	public synchronized Trade addTrade(String author, String type, String pokemon, String note) {
		Trade trade = new Trade();
		trade.id = UUID.randomUUID().toString();
		trade.author = author;
		trade.type = type;
		trade.pokemon = pokemon;
		trade.note = note;
		trade.timestamp = System.currentTimeMillis();

		trades.add(0, trade);
		while (trades.size() > MAX_TRADES) {
			trades.remove(trades.size() - 1);
		}
		write("trades.json", trades);
		return trade;
	}

	/**
	 * @return true if the author's trade existed and was removed.
	 */
	public synchronized boolean deleteTrade(String id, String author) {
		boolean removed = trades.removeIf(t -> id.equals(t.id) && author.equals(t.author));
		if (removed) {
			write("trades.json", trades);
		}
		return removed;
	}

	/**
	 * Restarts the 72 hour lifetime of the author's trade.
	 * @return The new timestamp, or null if the trade doesn't exist.
	 */
	public synchronized Long extendTrade(String id, String author) {
		for (Trade trade : trades) {
			if (id.equals(trade.id) && author.equals(trade.author)) {
				trade.timestamp = System.currentTimeMillis();
				write("trades.json", trades);
				return trade.timestamp;
			}
		}
		return null;
	}

	public synchronized List<HistoryEntry> getHistory(String author) {
		return new ArrayList<>(history.getOrDefault(author, List.of()));
	}

	/**
	 * Remembers a post so the player can repeat it quickly. Keeps the last 20 unique posts.
	 */
	public synchronized List<HistoryEntry> addHistory(String author, String type, String pokemon, String note) {
		List<HistoryEntry> entries = new ArrayList<>();
		entries.add(new HistoryEntry(type, pokemon, note));
		for (HistoryEntry entry : history.getOrDefault(author, List.of())) {
			boolean duplicate = type.equals(entry.type) && entry.pokemon != null
					&& entry.pokemon.toLowerCase(Locale.ROOT).equals(pokemon.toLowerCase(Locale.ROOT));
			if (!duplicate && entries.size() < MAX_HISTORY) {
				entries.add(entry);
			}
		}
		history.put(author, entries);
		write("trade_history.json", history);
		return new ArrayList<>(entries);
	}

	public synchronized long getVisits() {
		return visits;
	}

	public synchronized long addVisit() {
		visits++;
		Counter counter = new Counter();
		counter.count = visits;
		write("counter.json", counter);
		return visits;
	}

	private <T> T read(String file, TypeToken<T> type, T fallback) {
		Path path = DATA_DIR.resolve(file);
		if (!Files.exists(path)) {
			return fallback;
		}
		try {
			T value = gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), type.getType());
			return value == null ? fallback : value;
		} catch (Exception e) {
			Gts.LOGGER.error("Could not read " + path + ": " + e.getMessage());
			return fallback;
		}
	}

	private void write(String file, Object data) {
		try {
			Files.createDirectories(DATA_DIR);
			Path target = DATA_DIR.resolve(file);
			Path temp = DATA_DIR.resolve(file + ".tmp");
			Files.writeString(temp, gson.toJson(data), StandardCharsets.UTF_8);
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			Gts.LOGGER.error("Could not write " + file + " of the GTS website: " + e.getMessage());
		}
	}
}
