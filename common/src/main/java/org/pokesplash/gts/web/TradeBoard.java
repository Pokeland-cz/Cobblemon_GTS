package org.pokesplash.gts.web;

import com.google.gson.reflect.TypeToken;

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

	private List<Trade> trades;
	private Map<String, List<HistoryEntry>> history;
	private long visits;

	public TradeBoard() {
		trades = JsonFiles.read("trades.json", new TypeToken<List<Trade>>() {}, new ArrayList<>());
		history = JsonFiles.read("trade_history.json", new TypeToken<LinkedHashMap<String, List<HistoryEntry>>>() {},
				new LinkedHashMap<>());
		visits = JsonFiles.read("counter.json", new TypeToken<Counter>() {}, new Counter()).count;
	}

	public static boolean isValidType(String type) {
		return "wtb".equals(type) || "wts".equals(type);
	}

	/**
	 * @return All trades, newest first. They stay until their author deletes them
	 * or they are pushed out by the 200 newest.
	 */
	public synchronized List<Trade> getTrades() {
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
		JsonFiles.write("trades.json", trades);
		return trade;
	}

	/**
	 * @return true if the author's trade existed and was removed.
	 */
	public synchronized boolean deleteTrade(String id, String author) {
		boolean removed = trades.removeIf(t -> id.equals(t.id) && author.equals(t.author));
		if (removed) {
			JsonFiles.write("trades.json", trades);
		}
		return removed;
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
		JsonFiles.write("trade_history.json", history);
		return new ArrayList<>(entries);
	}

	public synchronized long getVisits() {
		return visits;
	}

	public synchronized long addVisit() {
		visits++;
		Counter counter = new Counter();
		counter.count = visits;
		JsonFiles.write("counter.json", counter);
		return visits;
	}
}
