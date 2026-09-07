package api.anticorruption.content.dto;

import api.anticorruption.content.AboutSection;

import java.time.Instant;
import java.util.List;

/**
 * "Bo'lim haqida" sahifasining mazmuni.
 *
 * @param filled sahifa to'ldirilganmi. Bo'sh bo'lsa sayt menyusida
 *               ko'rsatilmaydi va admin panelida "hali to'ldirilmagan"
 *               deb chiqadi.
 */
public record AboutSectionResponse(
        String title,
        String body,
        String tasksTitle,
        List<String> tasks,
        String goal,
        boolean filled,
        Instant updatedAt
) {
    public static AboutSectionResponse from(AboutSection about) {
        return new AboutSectionResponse(
                about.getTitle(),
                about.getBody(),
                about.getTasksTitle(),
                List.copyOf(about.getTasks()),
                about.getGoal(),
                isFilled(about),
                about.getUpdatedAt());
    }

    /** Hali hech narsa kiritilmagan sahifa. */
    public static AboutSectionResponse empty() {
        return new AboutSectionResponse(null, null, null, List.of(), null, false, null);
    }

    private static boolean isFilled(AboutSection about) {
        return notBlank(about.getTitle())
                || notBlank(about.getBody())
                || notBlank(about.getGoal())
                || !about.getTasks().isEmpty();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
