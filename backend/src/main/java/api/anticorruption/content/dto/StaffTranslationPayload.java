package api.anticorruption.content.dto;

import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.content.StaffMemberTranslation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Xodim ma'lumotlarining bir tildagi varianti.
 *
 * <p>Telefon, email va surat bu yerda yo'q: ular tilga bog'liq emas va
 * asosiy yozuvda bir marta kiritiladi.
 *
 * @param languageName tilning o'z tilidagi nomi - faqat javobda to'ldiriladi
 */
public record StaffTranslationPayload(

        @NotBlank(message = "{validation.required}")
        @Size(max = 10, message = "{validation.size.max}")
        String languageCode,

        String languageName,

        @Size(max = 150, message = "{validation.size.max}")
        String fullName,

        @Size(max = 200, message = "{validation.size.max}")
        String position,

        @Size(max = 150, message = "{validation.size.max}")
        String academicDegree,

        @Size(max = 20000, message = "{validation.size.max}")
        String biography,

        @Size(max = 150, message = "{validation.size.max}")
        String receptionHours
) {
    public static StaffTranslationPayload from(StaffMemberTranslation translation) {
        AppLanguage language = translation.getLanguage();
        return new StaffTranslationPayload(
                language.getCode(),
                language.getDisplayName(),
                translation.getFullName(),
                translation.getPosition(),
                translation.getAcademicDegree(),
                translation.getBiography(),
                translation.getReceptionHours());
    }
}
