package api.anticorruption.content.dto;

/**
 * Diskdagi rasm nomini API manziliga o'giradi.
 *
 * <p>Bazada faqat fayl nomi saqlanadi, to'liq manzil emas: shunda domen
 * yoki yo'l o'zgarsa eski yozuvlarni tuzatish shart bo'lmaydi.
 */
final class MediaUrls {

    private static final String MEDIA_PATH = "/api/v1/media/";

    private MediaUrls() {
    }

    /** Rasm biriktirilmagan bo'lsa null qaytaradi. */
    static String of(String storedName) {
        return storedName == null || storedName.isBlank() ? null : MEDIA_PATH + storedName;
    }
}
