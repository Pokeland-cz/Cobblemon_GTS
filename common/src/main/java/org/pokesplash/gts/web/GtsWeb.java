package org.pokesplash.gts.web;

import com.sun.net.httpserver.HttpServer;
import org.pokesplash.gts.Gts;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Built-in web server serving the GTS website and its API.
 */
public final class GtsWeb {
	private static final WebAuth AUTH = new WebAuth(); // Kept across restarts so issued codes stay valid.
	private static WebConfig config = new WebConfig();
	private static HttpServer server;
	private static ExecutorService executor;

	private GtsWeb() {}

	/**
	 * Loads config/gts/web.json and starts the web server if it is enabled.
	 * Failures are logged and never stop the Minecraft server.
	 */
	public static synchronized void start() {
		stop();
		config = WebConfig.load();
		if (!config.isEnabled()) {
			return;
		}

		try {
			AtomicInteger threadId = new AtomicInteger();
			executor = Executors.newFixedThreadPool(config.getThreads(), runnable -> {
				Thread thread = new Thread(runnable, "GTS-Web-" + threadId.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			});

			server = HttpServer.create(new InetSocketAddress(config.getBindAddress(), config.getPort()), 64);
			server.createContext("/api/", new ApiHandler(config, AUTH, new TradeBoard(), new DiscordLinks()));
			server.createContext("/", new StaticHandler());
			server.setExecutor(executor);
			server.start();
			Gts.LOGGER.info("GTS website running on " + config.getBindAddress() + ":" + config.getPort()
					+ " (" + config.getPublicUrl() + ")");
		} catch (Exception | LinkageError e) {
			Gts.LOGGER.error("Could not start the GTS website on port " + config.getPort() + ": " + e);
			stop();
		}
	}

	public static synchronized void stop() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
	}

	public static synchronized boolean isRunning() {
		return server != null;
	}

	public static synchronized WebConfig getConfig() {
		return config;
	}

	public static WebAuth getAuth() {
		return AUTH;
	}
}
