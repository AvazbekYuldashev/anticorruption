package api.anticorruption.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT sozlamalari (application.properties dagi "app.jwt.*").
 *
 * @param secret     HMAC-SHA kaliti; kamida 32 bayt (256 bit) bo'lishi shart
 * @param expiration token amal qilish muddati
 * @param issuer     token kim tomonidan berilgani
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        String issuer
) {
    public JwtProperties {
        if (expiration == null) {
            expiration = Duration.ofHours(12);
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "anticorruption-api";
        }
    }
}
