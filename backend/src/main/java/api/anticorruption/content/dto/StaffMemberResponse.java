package api.anticorruption.content.dto;

import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.content.StaffMember;
import api.anticorruption.content.StaffMemberTranslation;

import java.util.List;
import java.util.Optional;

/**
 * Bo'lim xodimi haqidagi ma'lumot.
 *
 * <p>Telefon, email va surat tilga bog'liq emas - ular har doim asosiy
 * yozuvdan keladi.
 *
 * @param translations faqat admin uchun to'ldiriladi; saytga bo'sh keladi
 */
public record StaffMemberResponse(
        Long id,
        String languageCode,
        String fullName,
        String position,
        String academicDegree,
        String biography,
        String phone,
        String email,
        String receptionHours,
        String photoUrl,
        int displayOrder,
        boolean active,
        List<StaffTranslationPayload> translations
) {
    /** Sayt uchun: so'ralgan tildagi matn, bo'sh maydonlar asosiy tildan. */
    public static StaffMemberResponse localized(StaffMember member, AppLanguage language) {
        Optional<StaffMemberTranslation> translation = find(member, language);

        return new StaffMemberResponse(
                member.getId(),
                language.getCode(),
                pick(translation.map(StaffMemberTranslation::getFullName), member.getFullName()),
                pick(translation.map(StaffMemberTranslation::getPosition), member.getPosition()),
                pick(translation.map(StaffMemberTranslation::getAcademicDegree), member.getAcademicDegree()),
                pick(translation.map(StaffMemberTranslation::getBiography), member.getBiography()),
                member.getPhone(),
                member.getEmail(),
                pick(translation.map(StaffMemberTranslation::getReceptionHours), member.getReceptionHours()),
                MediaUrls.of(member.getPhoto()),
                member.getDisplayOrder(),
                member.isActive(),
                List.of());
    }

    /** Admin uchun: asosiy matn va barcha tarjimalar. */
    public static StaffMemberResponse forAdmin(StaffMember member) {
        return new StaffMemberResponse(
                member.getId(),
                AppLanguage.DEFAULT.getCode(),
                member.getFullName(),
                member.getPosition(),
                member.getAcademicDegree(),
                member.getBiography(),
                member.getPhone(),
                member.getEmail(),
                member.getReceptionHours(),
                MediaUrls.of(member.getPhoto()),
                member.getDisplayOrder(),
                member.isActive(),
                member.getTranslations().stream().map(StaffTranslationPayload::from).toList());
    }

    private static Optional<StaffMemberTranslation> find(StaffMember member, AppLanguage language) {
        if (language == AppLanguage.DEFAULT) {
            return Optional.empty();
        }
        return member.getTranslations().stream()
                .filter(item -> item.getLanguage() == language)
                .findFirst();
    }

    private static String pick(Optional<String> translated, String base) {
        return translated.filter(value -> !value.isBlank()).orElse(base);
    }
}
