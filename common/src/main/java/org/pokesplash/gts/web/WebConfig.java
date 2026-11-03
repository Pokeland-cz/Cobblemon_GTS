package org.pokesplash.gts.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.pokesplash.gts.Gts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Settings of the built-in web server, stored in config/gts/web.json.
 */
public class WebConfig {
	public static final Path FILE = Path.of("config", "gts", "web.json");
	// No HTML escaping, so messages with & color codes stay readable in the file.
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private boolean enabled = false; // Starts the web server.
	private String bindAddress = "0.0.0.0"; // Address the web server listens on.
	private int port = 8080; // Port the web server listens on.
	private String publicUrl = ""; // URL players open, used in the /gts web link. Empty = http://localhost:port
	private boolean secureCookies = false; // Set to true when the site is served over HTTPS (reverse proxy).
	private boolean trustProxyHeaders = false; // Use X-Forwarded-For for rate limiting (only behind a reverse proxy).
	private int threads = 4; // Worker threads handling web requests.
	private int sessionHours = 168; // A web login expires after this many hours without visiting the website.
	private int loginCodeMinutes = 5; // How long a /gts web code stays valid.
	private String sessionSecret = ""; // Signs login sessions. Generated on first start, keep it private.
	private String discordClientId = ""; // Discord application for "Log in with Discord". Empty = disabled.
	private String discordClientSecret = ""; // Keep it private.
	private String loginLinkMessage = "&3GTS web: &b&n{url}"; // Clickable chat message, {url} = login link.
	private String loginCodeMessage = "&7Or enter code &f{code} &7on the website (valid {minutes} min)."; // {code}, {minutes}
	private String disabledMessage = "&cThe GTS website is disabled.";

	/**
	 * Loads the config, writing defaults and a fresh session secret when needed.
	 * @return The loaded config.
	 */
	public static WebConfig load() {
		WebConfig config = new WebConfig();
		boolean write = !Files.exists(FILE);

		if (!write) {
			try {
				String data = Files.readString(FILE, StandardCharsets.UTF_8);
				JsonObject json = JsonParser.parseString(data).getAsJsonObject();
				config = GSON.fromJson(json, WebConfig.class);
				// Write back so options added in newer versions show up in the file.
				write = !json.keySet().containsAll(GSON.toJsonTree(new WebConfig()).getAsJsonObject().keySet());
			} catch (Exception e) {
				Gts.LOGGER.error("Could not read " + FILE + ", the GTS website stays disabled: " + e.getMessage());
				return new WebConfig();
			}
		}

		if (config.sessionSecret == null || config.sessionSecret.length() < 32) {
			byte[] secret = new byte[32];
			new SecureRandom().nextBytes(secret);
			config.sessionSecret = Base64.getEncoder().encodeToString(secret);
			write = true;
		}

		if (write) {
			try {
				Files.createDirectories(FILE.getParent());
				Files.writeString(FILE, GSON.toJson(config), StandardCharsets.UTF_8);
			} catch (Exception e) {
				Gts.LOGGER.error("Could not write " + FILE + ": " + e.getMessage());
			}
		}

		return config;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public int getPort() {
		return port;
	}

	public String getPublicUrl() {
		if (publicUrl == null || publicUrl.isBlank()) {
			String host = bindAddress == null || bindAddress.isBlank() || bindAddress.equals("0.0.0.0")
					? "localhost" : bindAddress;
			return "http://" + host + ":" + port;
		}
		return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
	}

	public boolean isSecureCookies() {
		return secureCookies;
	}

	public boolean isTrustProxyHeaders() {
		return trustProxyHeaders;
	}

	public int getThreads() {
		return Math.max(1, Math.min(threads, 32));
	}

	public int getSessionHours() {
		return Math.max(1, sessionHours);
	}

	public int getLoginCodeMinutes() {
		return Math.max(1, loginCodeMinutes);
	}

	public boolean isDiscordEnabled() {
		return discordClientId != null && !discordClientId.isBlank()
				&& discordClientSecret != null && !discordClientSecret.isBlank();
	}

	public String getDiscordClientId() {
		return discordClientId.trim();
	}

	public String getDiscordClientSecret() {
		return discordClientSecret.trim();
	}

	/**
	 * @return The OAuth2 redirect URL to register in the Discord Developer Portal.
	 */
	public String getDiscordRedirectUri() {
		return getPublicUrl() + "/api/auth/discord/callback";
	}

	public byte[] getSessionSecret() {
		return sessionSecret.getBytes(StandardCharsets.UTF_8);
	}

	public String getLoginLinkMessage() {
		return loginLinkMessage;
	}

	public String getLoginCodeMessage() {
		return loginCodeMessage;
	}

	public String getDisabledMessage() {
		return disabledMessage;
	}
}
