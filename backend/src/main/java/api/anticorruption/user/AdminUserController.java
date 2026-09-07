package api.anticorruption.user;

import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.security.AppUserPrincipal;
import api.anticorruption.user.dto.ChangeRoleRequest;
import api.anticorruption.user.dto.CreateUserRequest;
import api.anticorruption.user.dto.SetEnabledRequest;
import api.anticorruption.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Foydalanuvchilarni boshqarish. Bu yo'llarga faqat ADMIN kira oladi. */
@Tag(name = "Admin - foydalanuvchilar", description = "Rollarni belgilash va hisoblarni bloklash")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "Foydalanuvchilar ro'yxati", description = "role parametri bilan filtrlash mumkin")
    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(userService.list(role, pageable));
    }

    @Operation(
            summary = "Yangi hisob yaratish",
            description = "Ochiq ro'yxatdan o'tish yo'q - hisoblarni faqat administrator ochadi")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Operation(summary = "Bitta foydalanuvchi ma'lumoti")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Rolni o'zgartirish", description = "Administrator o'z rolini o'zgartira olmaydi")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return ResponseEntity.ok(userService.changeRole(id, request.role(), principal.user()));
    }

    @Operation(summary = "Hisobni bloklash yoki blokdan chiqarish",
            description = "Administrator o'z hisobini bloklay olmaydi")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> setEnabled(
            @PathVariable Long id,
            @Valid @RequestBody SetEnabledRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return ResponseEntity.ok(userService.setEnabled(id, request.enabled(), principal.user()));
    }

    @Operation(
            summary = "Hisobni o'chirish",
            description = "Murojaatlarga bog'langan hisob o'chirilmaydi - uni bloklash kerak")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        userService.delete(id, principal.user());
        return ResponseEntity.noContent().build();
    }
}
