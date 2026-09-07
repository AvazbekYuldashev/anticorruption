package api.anticorruption.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis dagi umumiy hisob: chegara butun klaster uchun bitta bo'ladi.
 *
 * <p>Kalit tarkibida oyna raqami bor ({@code rate-limit:IP:12345}), ya'ni
 * har bir oyna o'z kalitiga yoziladi. Shu tufayli muddat qo'yish urinishi
 * o'tmay qolsa ham mijoz abadiy bloklanmaydi - keyingi oyna baribir yangi
 * kalitdan boshlanadi.
 *
 * <p>Redis javob bermasa xotiradagi hisobga o'tiladi. Chegara - himoya
 * qatlami, ruxsat tekshiruvi emas: uning uzilishi tufayli butun sayt
 * ishdan chiqishi noto'g'ri bo'lardi.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private static final String PREFIX = "rate-limit:";

    private final StringRedisTemplate redis;
    private final RateLimitStore fallback;

    @Override
    public long increment(String client, Duration window) {
        long windowSeconds = Math.max(1, window.toSeconds());
        String key = PREFIX + client + ':' + (Instant.now().getEpochSecond() / windowSeconds);

        try {
            Long hits = redis.opsForValue().increment(key);
            if (hits == null) {
                return fallback.increment(client, window);
            }
            if (hits == 1L) {
                // Ikki barobar muddat: oyna chegarasidagi so'rovlar ham to'g'ri sanalsin.
                redis.expire(key, Duration.ofSeconds(windowSeconds * 2));
            }
            return hits;
        } catch (RuntimeException ex) {
            log.warn("Redis so'rov chegarasi ishlamadi, xotiradagi hisobga o'tildi: {}", ex.getMessage());
            return fallback.increment(client, window);
        }
    }
}
