package api.anticorruption.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Parolni almashtirish.
 *
 * <p>Joriy parol ham so'raladi: token o'g'irlangan bo'lsa ham, uni
 * bilmasdan parolni almashtirib, hisobni butunlay egallab bo'lmasin.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "{validation.required}")
        String currentPassword,

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 72, message = "{validation.size}")
        String newPassword
) {
}
