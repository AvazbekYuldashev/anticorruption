package api.anticorruption.content.dto;

import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.content.AboutSection;
import api.anticorruption.content.AboutSectionTranslation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * "Bo'lim haqida" sahifasining mazmuni.
 *
 * <p>Saytga so'ralgan tildagi matn, admin paneliga esa asosiy matn va
 * barcha tarjimalar birga qaytadi - muharrir hammasini bitta sahifada
 * ko'radi.
 *
 * @param languageCode  javobdagi matn qaysi tilda
 * @param filled        sahifa to'ldirilganmi. Bo'sh bo'lsa sayt menyusida
 *                      ko'rsatilmaydi va admin panelida "hali to'ldirilmagan"
 *                      deb chiqadi.
 * @param translations  faqat admin uchun to'ldiriladi; saytga bo'sh keladi
 */
public record AboutSectionResponse(
        String languageCode,
        String title,
        String body,
        String tasksTitle,
        List<String> tasks,
        String goal,
        boolean filled,
        List<AboutTranslationPayload> translations,
        Instant updatedAt
) {
    /**
     * Sayt uchun: so'ralgan tildagi matn.
     *
     * <p>Har bir maydon alohida qaytadi: tarjimada bo'sh qolgan sarlavha
     * o'zbekchasini ko'rsatadi, tarjima qilingan qismi esa o'z tilida
     * chiqadi. Butun sahifani "tarjima bor/yo'q" deb ikkiga bo'lish
     * yarim tayyor tarjimani foydasiz qilib qo'yardi.
     */
    public static AboutSectionResponse localized(AboutSection about, AppLanguage language) {
        Optional<AboutSectionTranslation> translation = find(about, language);

        return new AboutSectionResponse(
                language.getCode(),
                pick(translation.map(AboutSectionTranslation::getTitle), about.getTitle()),
                pick(translation.map(AboutSectionTranslation::getBody), about.getBody()),
                pick(translation.map(AboutSectionTranslation::getTasksTitle), about.getTasksTitle()),
                translation.map(AboutSectionTranslation::getTasks)
                        .filter(tasks -> !tasks.isEmpty())
                        .map(List::copyOf)
                        .orElseGet(() -> List.copyOf(about.getTasks())),
                pick(translation.map(AboutSectionTranslation::getGoal), about.getGoal()),
                isFilled(about),
                List.of(),
                about.getUpdatedAt());
    }

    /** Admin uchun: asosiy matn va barcha tarjimalar. */
    public static AboutSectionResponse forAdmin(AboutSection about) {
        return new AboutSectionResponse(
                AppLanguage.DEFAULT.getCode(),
                about.getTitle(),
                about.getBody(),
                about.getTasksTitle(),
                List.copyOf(about.getTasks()),
                about.getGoal(),
                isFilled(about),
                about.getTranslations().stream().map(AboutTranslationPayload::from).toList(),
                about.getUpdatedAt());
    }

    /** Hali hech narsa kiritilmagan sahifa. */
    public static AboutSectionResponse empty() {
        return new AboutSectionResponse(
                AppLanguage.DEFAULT.getCode(), null, null, null, List.of(), null, false, List.of(), null);
    }

    private static Optional<AboutSectionTranslation> find(AboutSection about, AppLanguage language) {
        if (language == AppLanguage.DEFAULT) {
            return Optional.empty();
        }
        return about.getTranslations().stream()
                .filter(item -> item.getLanguage() == language)
                .findFirst();
    }

    private static String pick(Optional<String> translated, String base) {
        return translated.filter(value -> !value.isBlank()).orElse(base);
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
