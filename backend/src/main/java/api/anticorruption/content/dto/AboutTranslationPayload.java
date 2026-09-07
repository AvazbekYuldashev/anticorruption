package api.anticorruption.content.dto;

import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.content.AboutSectionTranslation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * "Bo'lim haqida" sahifasining bir tildagi matni.
 *
 * <p>Ham saqlashda, ham admin panelidagi javobda ishlatiladi.
 * Bo'sh qoldirilgan maydon asosiy tildagi matnga qaytadi.
 *
 * @param languageCode "uz-cyrl", "ru" yoki "en"
 * @param languageName tilning o'z tilidagi nomi - faqat javobda to'ldiriladi
 */
public record AboutTranslationPayload(

        @NotBlank(message = "{validation.required}")
        @Size(max = 10, message = "{validation.size.max}")
        String languageCode,

        String languageName,

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
    public static AboutTranslationPayload from(AboutSectionTranslation translation) {
        AppLanguage language = translation.getLanguage();
        return new AboutTranslationPayload(
                language.getCode(),
                language.getDisplayName(),
                translation.getTitle(),
                translation.getBody(),
                translation.getTasksTitle(),
                List.copyOf(translation.getTasks()),
                translation.getGoal());
    }
}
