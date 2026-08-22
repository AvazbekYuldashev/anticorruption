package api.anticorruption.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yangilik yaratish yoki tahrirlash.
 *
 * @param published null bo'lsa holat o'zgarmaydi (yangi yangilik qoralama bo'ladi)
 */
public record SaveNewsRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 5, max = 250, message = "{validation.size}")
        String title,

        @Size(max = 500, message = "{validation.size.max}")
        String summary,

        @NotBlank(message = "{validation.required}")
        @Size(max = 50000, message = "{validation.size.max}")
        String body,

        Boolean published
) {
}
