package api.anticorruption.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ochiq yozuv amallari uchun so'rov chegarasi.
 *
 * @param requests          bitta oynada ruxsat etilgan so'rovlar soni
 * @param window            oyna uzunligi
 * @param maxTrackedClients xotirada saqlanadigan mijozlar soni chegarasi;
 *                          undan oshsa eskirgan yozuvlar tozalanadi
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(

        @DefaultValue("true") boolean enabled,

        @DefaultValue("30") int requests,

        @DefaultValue("1m") Duration window,

        @DefaultValue("10000") int maxTrackedClients
) {
}
