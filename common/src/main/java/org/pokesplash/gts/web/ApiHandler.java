package org.pokesplash.gts.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.Listing.Listing;
import org.pokesplash.gts.Listing.PokemonListing;
import org.pokesplash.gts.api.GtsAPI;
import org.pokesplash.gts.util.ColorUtil;
import org.pokesplash.gts.util.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * JSON API of the website. Errors are returned as {"error": "CODE"}, the website translates the codes.
 */
class ApiHandler implements HttpHandler {
	private static final long LISTINGS_CACHE_MILLIS = 2000;

	private final WebConfig config;
	private final WebAuth auth;
	private final TradeBoard board;
	private final RateLimiter loginLimiter = new RateLimiter(10, 60_000);
	private final RateLimiter buyLimiter = new RateLimiter(20, 60_000);
	private final RateLimiter postLimiter = new RateLimiter(30, 60_000);
	private final RateLimiter visitLimiter = new RateLimiter(3, 60 * 60_000);

	private volatile String listingsJson;
	private volatile long listingsTime;

	ApiHandler(WebConfig config, WebAuth auth, TradeBoard board) {
		this.config = config;
		this.auth = auth;
		this.board = board;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			route(exchange);
		} catch (Exception e) {
			Gts.LOGGER.error("GTS web API error on " + exchange.getRequestURI().getPath() + ": " + e);
			try {
				Http.error(exchange, 500, "INTERNAL");
			} catch (IOException ignored) {
				exchange.close();
			}
		}
	}

	private void route(HttpExchange exchange) throws Exception {
		String path = exchange.getRequestURI().getPath();
		String method = exchange.getRequestMethod();

		// Body-carrying requests must be JSON, which browsers can't send cross-site without CORS.
		boolean mutating = !method.equals("GET") && !method.equals("HEAD");
		boolean exempt = path.equals("/api/auth/logout") || path.equals("/api/counter");
		if (mutating && !exempt && !Http.isJsonRequest(exchange)) {
			Http.error(exchange, 415, "BAD_REQUEST");
			return;
		}

		switch (method + " " + path) {
			case "GET /api/listings" -> listings(exchange);
			case "GET /api/auth/me" -> me(exchange);
			case "POST /api/auth/login" -> login(exchange);
			case "POST /api/auth/logout" -> logout(exchange);
			case "POST /api/buy" -> buy(exchange);
			case "POST /api/listings/cancel" -> cancelListing(exchange);
			case "GET /api/listings/expired" -> expiredListings(exchange);
			case "POST /api/listings/claim" -> claimListing(exchange);
			case "GET /api/trades" -> Http.json(exchange, 200, board.getTrades());
			case "POST /api/trades" -> addTrade(exchange);
			case "DELETE /api/trades" -> deleteTrade(exchange);
			case "PATCH /api/trades" -> extendTrade(exchange);
			case "GET /api/trades/history" -> history(exchange);
			case "POST /api/trades/history" -> addHistory(exchange);
			case "GET /api/counter" -> Http.json(exchange, 200, Map.of("count", board.getVisits()));
			case "POST /api/counter" -> visit(exchange);
			default -> Http.error(exchange, 404, "NOT_FOUND");
		}
	}

	private String ip(HttpExchange exchange) {
		return Http.clientIp(exchange, config.isTrustProxyHeaders());
	}

	private WebAuth.Session session(HttpExchange exchange) {
		return auth.decode(Http.getCookie(exchange, WebAuth.COOKIE), config.getSessionSecret());
	}

	private void listings(HttpExchange exchange) throws Exception {
		String json = listingsJson;
		if (json == null || System.currentTimeMillis() - listingsTime > LISTINGS_CACHE_MILLIS) {
			// Listings are changed on the server thread, so read them there.
			json = onServerThread(() -> {
				JsonArray array = new JsonArray();
				for (PokemonListing listing : Gts.listings.getPokemonListings()) {
					array.add(Http.GSON.toJsonTree(listing));
				}
				return Http.GSON.toJson(array);
			});
			listingsJson = json;
			listingsTime = System.currentTimeMillis();
		}
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		Http.send(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
	}

	private void me(HttpExchange exchange) throws IOException {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.json(exchange, 401, Map.of("authenticated", false));
			return;
		}
		Http.json(exchange, 200, Map.of("authenticated", true, "user", Map.of(
				"id", session.uuid().toString(),
				"username", session.name(),
				"realname", session.name())));
	}

	private void login(HttpExchange exchange) throws IOException {
		if (!loginLimiter.tryAcquire(ip(exchange))) {
			Http.error(exchange, 429, "RATE_LIMITED");
			return;
		}
		WebAuth.Session session = auth.redeem(Http.getString(Http.readJson(exchange), "code"), config.getSessionHours());
		if (session == null) {
			Http.error(exchange, 401, "INVALID_CODE");
			return;
		}
		Http.setCookie(exchange, WebAuth.COOKIE, auth.encode(session, config.getSessionSecret()),
				config.getSessionHours() * 3600L, config.isSecureCookies());
		Http.json(exchange, 200, Map.of("success", true, "username", session.name()));
	}

	private void logout(HttpExchange exchange) throws IOException {
		Http.setCookie(exchange, WebAuth.COOKIE, "", 0, config.isSecureCookies());
		Http.json(exchange, 200, Map.of("success", true));
	}

	private void buy(HttpExchange exchange) throws Exception {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		if (!buyLimiter.tryAcquire(ip(exchange))) {
			Http.error(exchange, 429, "RATE_LIMITED");
			return;
		}

		UUID listingId = readListingId(exchange);
		if (listingId == null) {
			Http.error(exchange, 400, "BAD_REQUEST");
			return;
		}

		String result = onServerThread(() -> {
			ServerPlayer buyer = Gts.server.getPlayerList().getPlayer(session.uuid());
			if (buyer == null) {
				return "NOT_ONLINE";
			}
			Listing listing = Gts.listings.getActiveListingById(listingId);
			if (listing == null) {
				return "NOT_AVAILABLE";
			}
			return GtsAPI.purchase(buyer, listing).name();
		});

		listingsJson = null; // Show the change right away.
		switch (result) {
			case "SUCCESS" -> Http.json(exchange, 200, Map.of("success", true));
			case "NOT_ONLINE", "OWN_LISTING", "NO_INVENTORY_SPACE", "INSUFFICIENT_FUNDS" ->
					Http.error(exchange, 409, result);
			case "NOT_AVAILABLE" -> Http.error(exchange, 404, result);
			default -> Http.error(exchange, 500, "PURCHASE_FAILED");
		}
	}

	/**
	 * Cancels the player's own listing. It moves to their expired listings, to be collected in game
	 * with /gts expired, so it works while the player is offline.
	 */
	private void cancelListing(HttpExchange exchange) throws Exception {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		if (!postLimiter.tryAcquire(ip(exchange))) {
			Http.error(exchange, 429, "RATE_LIMITED");
			return;
		}

		UUID listingId = readListingId(exchange);
		if (listingId == null) {
			Http.error(exchange, 400, "BAD_REQUEST");
			return;
		}

		String result = onServerThread(() -> {
			Listing listing = Gts.listings.getActiveListingById(listingId);
			if (listing == null) {
				return "NOT_AVAILABLE";
			}
			if (!listing.getSellerUuid().equals(session.uuid())) {
				return "NOT_OWNER";
			}
			if (!GtsAPI.cancelListing(listing)) {
				return "FAILED";
			}
			ServerPlayer seller = Gts.server.getPlayerList().getPlayer(session.uuid());
			if (seller != null) {
				seller.sendSystemMessage(ColorUtil.parse(Utils.formatPlaceholders(Gts.language.getCancelListing(),
						0, listing.getListingName(), listing.getSellerName(), session.name())));
			}
			return "SUCCESS";
		});

		listingsJson = null; // Show the change right away.
		switch (result) {
			case "SUCCESS" -> Http.json(exchange, 200, Map.of("success", true));
			case "NOT_AVAILABLE" -> Http.error(exchange, 404, result);
			case "NOT_OWNER" -> Http.error(exchange, 403, result);
			default -> Http.error(exchange, 500, "INTERNAL");
		}
	}

	/**
	 * The player's expired and cancelled listings, waiting to be collected.
	 */
	private void expiredListings(HttpExchange exchange) throws Exception {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		String json = onServerThread(() -> {
			JsonArray array = new JsonArray();
			for (Listing listing : Gts.listings.getExpiredListingsOfPlayer(session.uuid())) {
				JsonObject object = Http.GSON.toJsonTree(listing).getAsJsonObject();
				object.addProperty("listingName", listing.getListingName());
				array.add(object);
			}
			return Http.GSON.toJson(array);
		});
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		Http.send(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Gives an expired listing back to its seller, who must be online in game.
	 */
	private void claimListing(HttpExchange exchange) throws Exception {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		if (!postLimiter.tryAcquire(ip(exchange))) {
			Http.error(exchange, 429, "RATE_LIMITED");
			return;
		}
		UUID listingId = readListingId(exchange);
		if (listingId == null) {
			Http.error(exchange, 400, "BAD_REQUEST");
			return;
		}

		String result = onServerThread(() -> {
			ServerPlayer player = Gts.server.getPlayerList().getPlayer(session.uuid());
			if (player == null) {
				return "NOT_ONLINE";
			}
			Listing listing = Gts.listings.getExpiredListingById(listingId);
			if (listing == null) {
				return "NOT_AVAILABLE";
			}
			if (!listing.getSellerUuid().equals(session.uuid())) {
				return "NOT_OWNER";
			}

			boolean success = GtsAPI.returnListing(player, listing);
			// Same messages as collecting in the in-game UI.
			player.sendSystemMessage(ColorUtil.parse(Utils.formatPlaceholders(success
							? Gts.language.getReturnListingSuccess() : Gts.language.getReturnListingFail(),
					0, listing.getListingName(), listing.getSellerName(), session.name())));

			if (success) {
				return "SUCCESS";
			}
			return !listing.isPokemon() && player.getInventory().getFreeSlot() == -1 ? "NO_INVENTORY_SPACE" : "FAILED";
		});

		switch (result) {
			case "SUCCESS" -> Http.json(exchange, 200, Map.of("success", true));
			case "NOT_ONLINE", "NO_INVENTORY_SPACE" -> Http.error(exchange, 409, result);
			case "NOT_AVAILABLE" -> Http.error(exchange, 404, result);
			case "NOT_OWNER" -> Http.error(exchange, 403, result);
			default -> Http.error(exchange, 500, "CLAIM_FAILED");
		}
	}

	/**
	 * @return The "listing_id" of a JSON body, or null if missing or not a UUID.
	 */
	private static UUID readListingId(HttpExchange exchange) throws IOException {
		try {
			return UUID.fromString(Http.getString(Http.readJson(exchange), "listing_id"));
		} catch (IllegalArgumentException | NullPointerException e) {
			return null;
		}
	}

	private void addTrade(HttpExchange exchange) throws IOException {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		if (!postLimiter.tryAcquire(ip(exchange))) {
			Http.error(exchange, 429, "RATE_LIMITED");
			return;
		}
		Post post = readPost(exchange);
		if (post == null) {
			Http.error(exchange, 400, "BAD_REQUEST");
			return;
		}
		Http.json(exchange, 200, board.addTrade(session.name(), post.type, post.pokemon, post.note));
	}

	private void deleteTrade(HttpExchange exchange) throws IOException {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		String id = Http.getString(Http.readJson(exchange), "id");
		if (id == null || !board.deleteTrade(id, session.name())) {
			Http.error(exchange, 404, "TRADE_NOT_FOUND");
			return;
		}
		Http.json(exchange, 200, Map.of("success", true));
	}

	private void extendTrade(HttpExchange exchange) throws IOException {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		String id = Http.getString(Http.readJson(exchange), "id");
		Long timestamp = id == null ? null : board.extendTrade(id, session.name());
		if (timestamp == null) {
			Http.error(exchange, 404, "TRADE_NOT_FOUND");
			return;
		}
		Http.json(exchange, 200, Map.of("success", true, "timestamp", timestamp));
	}

	private void history(HttpExchange exchange) throws IOException {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		Http.json(exchange, 200, board.getHistory(session.name()));
	}

	private void addHistory(HttpExchange exchange) throws IOException {
		WebAuth.Session session = session(exchange);
		if (session == null) {
			Http.error(exchange, 401, "UNAUTHORIZED");
			return;
		}
		Post post = readPost(exchange);
		if (post == null) {
			Http.error(exchange, 400, "BAD_REQUEST");
			return;
		}
		Http.json(exchange, 200, board.addHistory(session.name(), post.type, post.pokemon, post.note));
	}

	private void visit(HttpExchange exchange) throws IOException {
		long count = visitLimiter.tryAcquire(ip(exchange)) ? board.addVisit() : board.getVisits();
		Http.json(exchange, 200, Map.of("count", count));
	}

	private record Post(String type, String pokemon, String note) {}

	private static Post readPost(HttpExchange exchange) throws IOException {
		JsonObject json = Http.readJson(exchange);
		String type = Http.getString(json, "type");
		String pokemon = Http.getString(json, "pokemon");
		String note = Http.getString(json, "note");
		if (!TradeBoard.isValidType(type) || pokemon == null || pokemon.isBlank()
				|| pokemon.length() > TradeBoard.MAX_POKEMON_LENGTH
				|| (note != null && note.length() > TradeBoard.MAX_NOTE_LENGTH)) {
			return null;
		}
		return new Post(type, pokemon.trim(), note == null ? "" : note.trim());
	}

	/**
	 * Runs a task on the Minecraft server thread and waits up to 5 seconds for it.
	 */
	private static <T> T onServerThread(Supplier<T> task) throws Exception {
		MinecraftServer server = Gts.server;
		if (server == null) {
			throw new IllegalStateException("The server is not running");
		}
		try {
			return server.submit(task).get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("The server thread did not respond in time", e);
		}
	}
}
