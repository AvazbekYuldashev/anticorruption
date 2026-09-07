package api.anticorruption.content.dto;

import api.anticorruption.content.News;

/**
 * Bir maqolaning boshqa tildagi nusxasiga havola.
 *
 * <p>Til tanlash tugmasi shu ro'yxatga qaraydi: foydalanuvchi rus tiliga
 * o'tsa, sayt uni bosh sahifaga emas, xuddi shu maqolaning ruschasiga
 * olib boradi.
 *
 * @param languageCode "uz", "uz-cyrl", "ru" yoki "en"
 * @param languageName tilning o'z tilidagi nomi - menyuda ko'rsatish uchun
 * @param published    qoralama tarjima ham ro'yxatda ko'rinadi (faqat admin panelida)
 */
public record NewsTranslationResponse(
        Long id,
        String languageCode,
        String languageName,
        String slug,
        String title,
        boolean published
) {
    public static NewsTranslationResponse from(News news) {
        return new NewsTranslationResponse(
                news.getId(),
                news.getLanguage().getCode(),
                news.getLanguage().getDisplayName(),
                news.getSlug(),
                news.getTitle(),
                news.isPublished());
    }
}
