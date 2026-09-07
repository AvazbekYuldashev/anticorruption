package api.anticorruption.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Xotiradagi hisob: ilova bitta nusxada ishlaganda shu yetarli.
 *
 * <p>Bir nechta nusxaga chiqilganda har bir nusxa o'z hisobini yuritadi va
 * umumiy chegara nusxalar soniga ko'payib ketadi - o'shanda
 * {@link RedisRateLimitStore} kerak bo'ladi.
 */
public class InMemoryRateLimitStore implements RateLimitStore {

    /** IP -> joriy oynadagi urinishlar. */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final int maxTrackedClients;

    public InMemoryRateLimitStore(int maxTrackedClients) {
        this.maxTrackedClients = maxTrackedClients;
    }

    @Override
    public long increment(String client, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();

        Window current = windows.compute(client, (key, existing) ->
                existing == null || now - existing.startedAt >= windowMillis
                        ? new Window(now)
                        : existing);

        long hits = current.hits.incrementAndGet();

        // Eskirgan yozuvlar to'planib qolmasin: tozalash arzon va kamdan-kam.
        if (windows.size() > maxTrackedClients) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis);
        }

        return hits;
    }

    /** Bitta mijozning joriy oynasi. */
    private static final class Window {
        private final long startedAt;
        private final AtomicInteger hits = new AtomicInteger();

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
