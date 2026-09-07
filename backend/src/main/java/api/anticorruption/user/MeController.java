package api.anticorruption.user;

import api.anticorruption.auth.AuthCookieService;
import api.anticorruption.auth.AuthService;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.security.AppUserPrincipal;
import api.anticorruption.user.dto.ChangePasswordRequest;
import api.anticorruption.user.dto.UpdateProfileRequest;
import api.anticorruption.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tizimga kirgan foydalanuvchining o'z profili.
 *
 * <p>Hisoblarni administrator yaratadi, lekin login (email) va parolni
 * egasining o'zi almashtira oladi - shu tufayli administrator bergan
 * dastlabki parol uzoq qolib ketmaydi.
 */
@Tag(name = "Profil", description = "Joriy foydalanuvchi ma'lumotlari va parol")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final AuthService authService;
    private final AuthCookieService cookieService;
    private final Translator translator;

    @Operation(summary = "Men kimman", description = "Token egasining ma'lumotlarini qaytaradi")
    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(UserResponse.from(principal.user(), translator));
    }

    @Operation(
            summary = "Profilni yangilash",
            description = "Ism, email (login) va telefon. Email band bo'lsa 409 qaytadi")
    @PutMapping
    public ResponseEntity<UserResponse> update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(userService.updateProfile(principal.user(), request));
    }

    /**
     * Parolni almashtiradi.
     *
     * <p>Parol almashtirilishining odatiy sababi - eski parol birovga
     * ma'lum bo'lib qolgani. Shuning uchun barcha yangilash tokenlari
     * bekor qilinadi va boshqa qurilmalardagi seanslar uziladi. Joriy
     * qurilma esa yangi cookie'lar oladi - o'z parolini almashtirgan odam
     * shu zahoti tizimdan chiqib qolmasin.
     */
    @Operation(
            summary = "Parolni almashtirish",
            description = "Joriy parol ham talab qilinadi. Boshqa qurilmalardagi seanslar uziladi")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(principal.user(), request);

        AuthService.Session session = authService.renew(principal.user());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.accessCookie(session.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.refreshCookie(session.refreshToken()).toString())
                .build();
    }
}
