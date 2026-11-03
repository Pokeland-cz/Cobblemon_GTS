package org.pokesplash.gts.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window request limiter per client key (IP address).
 */
public class RateLimiter {
	private static final int MAX_TRACKED = 10_000;

	private final int limit;
	private final long windowMillis;
	private final Map<String, long[]> windows = new ConcurrentHashMap<>(); // key -> {windowStart, count}

	public RateLimiter(int limit, long windowMillis) {
		this.limit = limit;
		this.windowMillis = windowMillis;
	}

	/**
	 * Counts a request.
	 * @return true if the request is within the limit.
	 */
	public boolean tryAcquire(String key) {
		long now = System.currentTimeMillis();
		if (windows.size() > MAX_TRACKED) {
			windows.values().removeIf(w -> now - w[0] >= windowMillis);
		}
		long[] window = windows.compute(key, (k, w) -> {
			if (w == null || now - w[0] >= windowMillis) {
				return new long[]{now, 1};
			}
			w[1]++;
			return w;
		});
		return window[1] <= limit;
	}
}
