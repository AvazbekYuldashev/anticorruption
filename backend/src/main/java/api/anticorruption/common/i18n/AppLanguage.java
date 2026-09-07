package api.anticorruption.common.i18n;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tizim qo'llab-quvvatlaydigan tillar.
 *
 * <p>Har biriga bitta tarjima fayli mos keladi:
 * {@code messages_uz.properties}, {@code messages_uz_Cyrl.properties},
 * {@code messages_ru.properties}, {@code messages_en.properties}.
 *
 * <p>Bir tilni bir necha kod bilan yozish mumkin ({@code uz-cyrl}, {@code uzc},
 * {@code cyr}) - foydalanuvchi qaysi shaklda yozishini oldindan bilib bo'lmaydi,
 * shuning uchun keng qabul qilamiz.
 */
public enum AppLanguage {

    /** O'zbekcha, lotin alifbosi. Standart til. */
    UZ("uz", "O'zbekcha", Locale.forLanguageTag("uz"), "uz-latn", "uzl", "latn"),

    /** O'zbekcha, kirill alifbosi. */
    UZ_CYRL("uz-cyrl", "Ўзбекча", Locale.forLanguageTag("uz-Cyrl"), "uzc", "cyr", "cyrl", "uz_cyrl"),

    RU("ru", "Русский", Locale.forLanguageTag("ru"), "rus"),

    EN("en", "English", Locale.ENGLISH, "eng");

    /** Til aniqlanmasa ishlatiladigan til. */
    public static final AppLanguage DEFAULT = UZ;

    private static final Map<String, AppLanguage> BY_CODE = buildLookup();

    private final String code;
    private final String displayName;
    private final Locale locale;
    private final String[] aliases;

    AppLanguage(String code, String displayName, Locale locale, String... aliases) {
        this.code = code;
        this.displayName = displayName;
        this.locale = locale;
        this.aliases = aliases;
    }

    public String getCode() {
        return code;
    }

    /** Tilning o'z tilidagi nomi - til tanlash menyusi uchun. */
    public String getDisplayName() {
        return displayName;
    }

    public Locale getLocale() {
        return locale;
    }

    /** Noma'lum yoki bo'sh qiymat uchun standart tilni qaytaradi. */
    public static AppLanguage from(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        return BY_CODE.getOrDefault(value.trim().toLowerCase(Locale.ROOT), DEFAULT);
    }

    public static AppLanguage fromLocale(Locale locale) {
        if (locale == null) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(language -> language.locale.equals(locale))
                .findFirst()
                .orElseGet(() -> from(locale.getLanguage()));
    }

    private static Map<String, AppLanguage> buildLookup() {
        Map<String, AppLanguage> lookup = new LinkedHashMap<>();
        for (AppLanguage language : values()) {
            lookup.put(language.code, language);
            for (String alias : language.aliases) {
                lookup.put(alias, language);
            }
        }
        return Map.copyOf(lookup);
    }
}
