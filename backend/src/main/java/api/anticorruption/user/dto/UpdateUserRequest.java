package api.anticorruption.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Administrator hisob ma'lumotlarini tahrirlaydi.
 *
 * <p>Rol va bloklash alohida amallar: ular ro'yxatdan turib bir bosishda
 * o'zgartiriladi va o'z himoya qoidalariga ega.
 *
 * @param password yangi parol; null yoki bo'sh bo'lsa parol o'zgarmaydi.
 *                 Foydalanuvchi parolini unutganda administrator shu bilan tiklaydi
 */
public record UpdateUserRequest(

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

        @Size(min = 8, max = 72, message = "{validation.size}")
        String password
) {
}
