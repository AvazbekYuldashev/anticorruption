package api.anticorruption.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Matnli sahifani yaratish yoki tahrirlash.
 *
 * @param slug bo'sh qoldirilsa sarlavhadan avtomatik yasaladi
 */
public record SaveStaticPageRequest(

        @Pattern(regexp = "^$|^[a-z0-9-]{2,80}$", message = "{validation.slug}")
        String slug,

        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 200, message = "{validation.size}")
        String title,

        @NotBlank(message = "{validation.required}")
        @Size(max = 50000, message = "{validation.size.max}")
        String body,

        Integer displayOrder,

        Boolean published
) {
}
