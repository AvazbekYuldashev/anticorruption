package api.anticorruption.user.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.user.Role;
import api.anticorruption.user.User;

import java.time.Instant;

/**
 * Foydalanuvchi haqidagi ochiq ma'lumot (parolsiz).
 *
 * @param role      mashina uchun qiymat, masalan {@code MODERATOR}
 * @param roleLabel joriy tilga o'girilgan nomi
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Role role,
        String roleLabel,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user, Translator translator) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                translator.of(user.getRole()),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
