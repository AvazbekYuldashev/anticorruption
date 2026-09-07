package api.anticorruption.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT sozlamalari (application.properties dagi "app.jwt.*").
 *
 * @param secret            HMAC-SHA kaliti; kamida 32 bayt (256 bit) bo'lishi shart
 * @param expiration        kirish tokeni muddati - qisqa bo'lishi kerak, chunki uni
 *                          bekor qilib bo'lmaydi
 * @param refreshExpiration yangilash tokeni muddati; u bazada saqlanadi va
 *                          istalgan payt bekor qilinishi mumkin
 * @param issuer            token kim tomonidan berilgani
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        Duration refreshExpiration,
        String issuer
) {
    public JwtProperties {
        if (expiration == null) {
            expiration = Duration.ofMinutes(15);
        }
        if (refreshExpiration == null) {
            refreshExpiration = Duration.ofDays(14);
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "anticorruption-api";
        }
    }
}
