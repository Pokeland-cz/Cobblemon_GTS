package org.pokesplash.gts.web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Discord OAuth2 authorization code flow with the "identify" scope.
 */
class DiscordOAuth {
	private static final String AUTHORIZE_URL = "https://discord.com/oauth2/authorize";
	private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
	private static final String USER_URL = "https://discord.com/api/users/@me";
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	record DiscordUser(String id, String name) {}

	private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	private final WebConfig config;

	DiscordOAuth(WebConfig config) {
		this.config = config;
	}

	/**
	 * @param skipPrompt Don't ask again when the player already authorized the application.
	 * @return The Discord page that asks the player to authorize.
	 */
	String authorizeUrl(String state, boolean skipPrompt) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("response_type", "code");
		params.put("client_id", config.getDiscordClientId());
		params.put("scope", "identify");
		params.put("state", state);
		params.put("redirect_uri", config.getDiscordRedirectUri());
		if (skipPrompt) {
			params.put("prompt", "none");
		}
		return AUTHORIZE_URL + "?" + form(params);
	}

	/**
	 * Exchanges the code from the callback for the Discord user. The access token is not kept.
	 */
	DiscordUser fetchUser(String code) throws IOException, InterruptedException {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("grant_type", "authorization_code");
		params.put("code", code);
		params.put("redirect_uri", config.getDiscordRedirectUri());
		params.put("client_id", config.getDiscordClientId());
		params.put("client_secret", config.getDiscordClientSecret());

		JsonObject token = send(HttpRequest.newBuilder(URI.create(TOKEN_URL))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(form(params))));
		if (!token.has("access_token")) {
			throw new IOException("Discord returned no access token");
		}

		JsonObject user = send(HttpRequest.newBuilder(URI.create(USER_URL))
				.header("Authorization", "Bearer " + token.get("access_token").getAsString())
				.GET());

		String id = user.get("id").getAsString();
		String name = user.has("global_name") && !user.get("global_name").isJsonNull()
				? user.get("global_name").getAsString() : user.get("username").getAsString();
		return new DiscordUser(id, name);
	}

	private JsonObject send(HttpRequest.Builder request) throws IOException, InterruptedException {
		HttpResponse<String> response = client.send(request.timeout(TIMEOUT)
						.header("User-Agent", "GTS-Web (Cobblemon GTS mod)").build(),
				HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("Discord answered " + response.statusCode() + " for " + response.uri().getPath());
		}
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	private static String form(Map<String, String> params) {
		return params.entrySet().stream()
				.map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
						+ URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
				.collect(Collectors.joining("&"));
	}
}
