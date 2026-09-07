package api.anticorruption.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Yangi foydalanuvchini ro'yxatdan o'tkazish so'rovi.
 *
 * <p>Xabarlar {@code {kalit}} ko'rinishida beriladi va tarjima faylidan olinadi:
 * javob tili {@code ?lang=} parametriga qarab tanlanadi.
 */
public record RegisterRequest(

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
        String password
) {
}
