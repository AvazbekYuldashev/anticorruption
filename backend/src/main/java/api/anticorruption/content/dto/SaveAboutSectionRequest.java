package api.anticorruption.content.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * "Bo'lim haqida" sahifasini saqlash.
 *
 * <p>Hamma maydon ixtiyoriy: sahifa bosqichma-bosqich to'ldirilishi mumkin.
 * Bo'sh bandlar saqlashda tashlab yuboriladi.
 */
public record SaveAboutSectionRequest(

        @Size(max = 250, message = "{validation.size.max}")
        String title,

        @Size(max = 20000, message = "{validation.size.max}")
        String body,

        @Size(max = 250, message = "{validation.size.max}")
        String tasksTitle,

        @Size(max = 50, message = "{validation.size.max}")
        List<@Size(max = 500, message = "{validation.size.max}") String> tasks,

        @Size(max = 5000, message = "{validation.size.max}")
        String goal
) {
}
