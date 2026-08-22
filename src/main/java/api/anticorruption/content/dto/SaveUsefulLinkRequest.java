package api.anticorruption.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Foydali havolani qo'shish yoki tahrirlash. */
public record SaveUsefulLinkRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 200, message = "{validation.size}")
        String title,

        @NotBlank(message = "{validation.required}")
        @Size(max = 500, message = "{validation.size.max}")
        @Pattern(regexp = "^https?://.+", message = "{validation.url}")
        String url,

        @Size(max = 300, message = "{validation.size.max}")
        String description,

        @Size(max = 100, message = "{validation.size.max}")
        String groupName,

        Integer displayOrder,

        Boolean active
) {
}
