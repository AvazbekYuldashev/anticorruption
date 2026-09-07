package api.anticorruption.user.dto;

import api.anticorruption.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Administrator yaratadigan yangi hisob.
 *
 * <p>Ochiq ro'yxatdan o'tish yo'q, shuning uchun rol ham shu yerda
 * belgilanadi: xodim ham, oddiy foydalanuvchi ham shu yo'l bilan qo'shiladi.
 */
public record CreateUserRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 150, message = "{validation.size}")
        String fullName,

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.email}")
        @Size(max = 180, message = "{validation.size.max}")
        String email,

        @Pattern(regexp = "^$|^[+]?[0-9]{9,15}$", message = "{validation.phone}")
        @Size(max = 30, message = "{validation.size.max}")
        String phone,

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 72, message = "{validation.size}")
        String password,

        @NotNull(message = "{validation.required}")
        Role role
) {
}
