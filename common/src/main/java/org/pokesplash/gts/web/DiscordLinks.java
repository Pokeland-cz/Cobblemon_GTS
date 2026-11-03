package org.pokesplash.gts.web;

import com.google.gson.reflect.TypeToken;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Discord accounts linked to Minecraft accounts, one to one.
 * Stored in config/gts/web/data/discord_links.json.
 */
public class DiscordLinks {
	private static final String FILE = "discord_links.json";

	public static class Link {
		public UUID uuid; // Minecraft account.
		public String name; // Minecraft name, refreshed on every Discord login.
		public String discordName;
		public long linkedAt;
	}

	private final Map<String, Link> links; // Discord user id -> link

	public DiscordLinks() {
		links = JsonFiles.read(FILE, new TypeToken<LinkedHashMap<String, Link>>() {}, new LinkedHashMap<>());
		links.values().removeIf(link -> link == null || link.uuid == null);
	}

	/**
	 * Links a Discord account, replacing earlier links of either account.
	 */
	public synchronized void link(String discordId, String discordName, UUID uuid, String name) {
		links.values().removeIf(link -> link.uuid.equals(uuid));
		Link link = new Link();
		link.uuid = uuid;
		link.name = name;
		link.discordName = discordName;
		link.linkedAt = System.currentTimeMillis();
		links.put(discordId, link);
		JsonFiles.write(FILE, links);
	}

	/**
	 * @return The link of a Discord account, or null.
	 */
	public synchronized Link find(String discordId) {
		return links.get(discordId);
	}

	/**
	 * @return The link of a Minecraft account, or null.
	 */
	public synchronized Link findByUuid(UUID uuid) {
		for (Link link : links.values()) {
			if (link.uuid.equals(uuid)) {
				return link;
			}
		}
		return null;
	}

	/**
	 * @return true if the Minecraft account had a Discord account linked.
	 */
	public synchronized boolean unlink(UUID uuid) {
		boolean removed = links.values().removeIf(link -> link.uuid.equals(uuid));
		if (removed) {
			JsonFiles.write(FILE, links);
		}
		return removed;
	}

	/**
	 * Remembers the player's current Minecraft and Discord names.
	 */
	public synchronized void updateNames(String discordId, String name, String discordName) {
		Link link = links.get(discordId);
		if (link != null && (!Objects.equals(link.name, name) || !Objects.equals(link.discordName, discordName))) {
			link.name = name;
			link.discordName = discordName;
			JsonFiles.write(FILE, links);
		}
	}
}
