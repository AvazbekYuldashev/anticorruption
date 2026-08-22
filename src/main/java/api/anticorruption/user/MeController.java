package api.anticorruption.user;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.security.AppUserPrincipal;
import api.anticorruption.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tizimga kirgan foydalanuvchining o'z profili. */
@Tag(name = "Profil", description = "Joriy foydalanuvchi ma'lumotlari")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final Translator translator;

    @Operation(summary = "Men kimman", description = "Token egasining ma'lumotlarini qaytaradi")
    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(UserResponse.from(principal.user(), translator));
    }
}
