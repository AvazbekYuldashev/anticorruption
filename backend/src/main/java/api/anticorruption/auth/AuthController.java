package api.anticorruption.auth;

import api.anticorruption.auth.dto.AuthResponse;
import api.anticorruption.auth.dto.LoginRequest;
import api.anticorruption.common.exception.UnauthorizedException;
import api.anticorruption.common.i18n.MessageKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tizimga kirish, seansni yangilash va chiqish.
 *
 * <p>Tokenlar javob tanasida emas, {@code HttpOnly} cookie'da beriladi -
 * shuning uchun brauzerdagi JavaScript ularga kira olmaydi. Buning evaziga
 * yozuv so'rovlari CSRF tokenini talab qiladi: uni {@code /auth/csrf}
 * chaqiruvidan keyin {@code XSRF-TOKEN} cookie'sidan olib,
 * {@code X-XSRF-TOKEN} sarlavhasida qaytarish kerak.
 *
 * <p>Ochiq ro'yxatdan o'tish yo'q: hisoblarni faqat administrator yaratadi
 * ({@code /api/v1/admin/users}). Murojaat yuborish uchun esa hisob umuman
 * kerak emas - u anonim ham yuboriladi.
 */
@Tag(name = "Autentifikatsiya", description = "Tizimga kirish, seansni yangilash va chiqish")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookieService;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "Tizimga kirish",
            description = "Kirish va yangilash tokenlarini HttpOnly cookie sifatida o'rnatadi")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withSession(authService.login(request));
    }

    @Operation(
            summary = "Seansni yangilash",
            description = "Yangilash cookie'si bo'yicha yangi kirish tokeni beradi va cookie'ni almashtiradi")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String token = cookieService.readRefreshToken(request)
                .orElseThrow(() -> new UnauthorizedException(MessageKeys.ERROR_AUTH_REFRESH_INVALID));

        return withSession(authService.refresh(token));
    }

    @Operation(summary = "Chiqish", description = "Yangilash tokenini bekor qiladi va cookie'larni o'chiradi")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        cookieService.readRefreshToken(request).ifPresent(refreshTokenService::revoke);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clearedAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.clearedRefreshCookie().toString())
                .build();
    }

    /**
     * CSRF tokenini o'rnatadi.
     *
     * <p>Interfeys kirish sahifasida hech qanday API chaqiruvi qilmasligi
     * mumkin - shunda XSRF-TOKEN cookie'si ham bo'lmaydi va login so'rovi
     * 403 bilan qaytardi. Shu bo'sh chaqiruv aynan shuning uchun bor.
     */
    @Operation(summary = "CSRF tokeni", description = "XSRF-TOKEN cookie'sini o'rnatadi")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> withSession(AuthService.Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.accessCookie(session.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.refreshCookie(session.refreshToken()).toString())
                .body(session.body());
    }
}
