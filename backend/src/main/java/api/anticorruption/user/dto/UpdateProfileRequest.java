package api.anticorruption.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Foydalanuvchining o'z ma'lumotlarini yangilashi. */
public record UpdateProfileRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 150, message = "{validation.size}")
        String fullName,

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.email}")
        @Size(max = 180, message = "{validation.size.max}")
        String email,

        @Pattern(regexp = "^$|^[+]?[0-9]{9,15}$", message = "{validation.phone}")
        @Size(max = 30, message = "{validation.size.max}")
        String phone
) {
}
