package api.anticorruption.security;

import api.anticorruption.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** JWT token yaratadi va tekshiradi. */
@Slf4j
@Service
public class JwtService {

    /** HS256 uchun kalit kamida shuncha bayt bo'lishi kerak. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        byte[] secretBytes = properties.secret() == null
                ? new byte[0]
                : properties.secret().getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret kamida " + MIN_SECRET_BYTES + " bayt bo'lishi kerak, hozir: "
                            + secretBytes.length + " bayt. APP_JWT_SECRET muhit o'zgaruvchisini sozlang.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.properties = properties;
    }

    /** Foydalanuvchi uchun imzolangan token qaytaradi. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.expiration());

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /** Token amal qilish muddati (sekundlarda) - mijozga qaytarish uchun. */
    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }

    /**
     * Tokenni tekshiradi. Imzo yoki muddat noto'g'ri bo'lsa bo'sh Optional qaytaradi -
     * bunday holatda so'rov shunchaki autentifikatsiyasiz davom etadi.
     */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Yaroqsiz JWT token: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
