package api.anticorruption.auth;

import api.anticorruption.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Seans cookie'larini yasaydi va o'qiydi.
 *
 * <p>Ikkala cookie ham {@code HttpOnly}: brauzerdagi JavaScript ularni
 * o'qiy olmaydi, ya'ni XSS orqali token o'g'irlanmaydi. Buning evaziga
 * cookie har bir so'rovga avtomatik qo'shiladi - shuning uchun CSRF
 * himoyasi majburiy bo'lib qoladi (u {@code SecurityConfig} da yoqilgan).
 *
 * <p>Yangilash cookie'si {@code /api/v1/auth} yo'li bilan cheklangan:
 * qolgan so'rovlarda u umuman yuborilmaydi, demak oshkor bo'lish
 * ehtimoli ham kamayadi.
 */
@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private final AuthCookieProperties properties;
    private final JwtProperties jwtProperties;

    public ResponseCookie accessCookie(String token) {
        return build(properties.accessCookie(), token, "/", jwtProperties.expiration());
    }

    public ResponseCookie refreshCookie(String token) {
        return build(properties.refreshCookie(), token,
                properties.refreshCookiePath(), jwtProperties.refreshExpiration());
    }

    public ResponseCookie clearedAccessCookie() {
        return build(properties.accessCookie(), "", "/", Duration.ZERO);
    }

    public ResponseCookie clearedRefreshCookie() {
        return build(properties.refreshCookie(), "", properties.refreshCookiePath(), Duration.ZERO);
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        return read(request, properties.accessCookie());
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return read(request, properties.refreshCookie());
    }

    private Optional<String> read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(path)
                .maxAge(maxAge);

        if (properties.domain() != null && !properties.domain().isBlank()) {
            builder.domain(properties.domain());
        }
        return builder.build();
    }
}
