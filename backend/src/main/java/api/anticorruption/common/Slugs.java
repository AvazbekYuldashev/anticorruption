package api.anticorruption.common;

import java.util.Locale;

/**
 * Sarlavhadan URL uchun qulay "slug" yasaydi.
 *
 * <p>O'zbek lotin alifbosidagi apostrofli harflar (o', g') va rus harflari
 * lotin ekvivalentiga o'giriladi, qolgan belgilar chiziqchaga aylanadi.
 * Masalan: "Kafedrada pora so'ralgani aniqlandi" -> "kafedrada-pora-soralgani-aniqlandi".
 */
public final class Slugs {

    private static final int MAX_LENGTH = 120;

    private Slugs() {
    }

    public static String from(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String lower = text.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());
        boolean lastWasSeparator = false;

        for (int i = 0; i < lower.length(); i++) {
            String replacement = translate(lower.charAt(i));

            if (replacement == null) {
                // Harf ham, raqam ham emas - ajratuvchi sifatida qaraymiz.
                if (!lastWasSeparator && !builder.isEmpty()) {
                    builder.append('-');
                    lastWasSeparator = true;
                }
                continue;
            }
            builder.append(replacement);
            lastWasSeparator = false;
        }

        String slug = builder.toString();
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug.length() > MAX_LENGTH ? slug.substring(0, MAX_LENGTH) : slug;
    }

    /** Belgi slug ichida qanday ko'rinishini qaytaradi; ajratuvchi bo'lsa null. */
    private static String translate(char ch) {
        if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
            return String.valueOf(ch);
        }
        return switch (ch) {
            case 'а' -> "a";
            case 'б' -> "b";
            case 'в' -> "v";
            case 'г' -> "g";
            case 'д' -> "d";
            case 'е', 'э' -> "e";
            case 'ё' -> "yo";
            case 'ж' -> "j";
            case 'з' -> "z";
            case 'и' -> "i";
            case 'й' -> "y";
            case 'к' -> "k";
            case 'л' -> "l";
            case 'м' -> "m";
            case 'н' -> "n";
            case 'о' -> "o";
            case 'п' -> "p";
            case 'р' -> "r";
            case 'с' -> "s";
            case 'т' -> "t";
            case 'у' -> "u";
            case 'ф' -> "f";
            case 'х' -> "x";
            case 'ц' -> "ts";
            case 'ч' -> "ch";
            case 'ш' -> "sh";
            case 'щ' -> "sh";
            case 'ъ', 'ь' -> "";
            case 'ы' -> "i";
            case 'ю' -> "yu";
            case 'я' -> "ya";
            case 'ў' -> "o";
            case 'қ' -> "q";
            case 'ғ' -> "g";
            case 'ҳ' -> "h";
            // Apostrof harf ichida keladi (o', g') - shuning uchun tashlab yuboriladi,
            // ajratuvchi sifatida qaralmaydi.
            case '\'', '‘', '’', 'ʻ', 'ʼ' -> "";
            default -> null;
        };
    }
}
