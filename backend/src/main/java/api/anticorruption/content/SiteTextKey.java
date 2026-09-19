package api.anticorruption.content;

import java.util.Arrays;
import java.util.Optional;

/**
 * Admin panelidan o'zgartirish mumkin bo'lgan sayt matnlari.
 *
 * <p>Ro'yxat yopiq: administrator istalgan tarjima kalitini emas, faqat bosh
 * sahifa mazmunini o'zgartiradi. Aks holda xato xabarlari yoki tugma nomlari
 * kabi interfeys matnlari ham bazadan kelib, tarjima fayllari bilan
 * chalkashib ketardi.
 *
 * <p>Kalitlar frontenddagi tarjima kalitlari bilan aynan bir xil
 * ({@code frontend/src/lib/siteTexts.ts}).
 */
public enum SiteTextKey {

    /** Institut nomi - bosh bannerda, sayt tepasida va pastida. */
    INSTITUTE("site.institute"),

    /** Markaz nomi - bosh bannerda, sayt tepasida va pastida. */
    SITE_NAME("site.name"),

    HERO_TITLE("home.heroTitle"),
    HERO_TEXT("home.heroText"),
    CTA_SUBMIT("home.ctaSubmit"),
    CTA_TRACK("home.ctaTrack"),
    STATS_TITLE("home.statsTitle"),
    STAT_TOTAL("home.statTotal"),
    STAT_RESOLVED("home.statResolved"),
    STAT_OPEN("home.statOpen"),
    STAT_LAST30("home.statLast30"),
    HOW_TITLE("home.howTitle"),
    STEP1_TITLE("home.step1Title"),
    STEP1_TEXT("home.step1Text"),
    STEP2_TITLE("home.step2Title"),
    STEP2_TEXT("home.step2Text"),
    STEP3_TITLE("home.step3Title"),
    STEP3_TEXT("home.step3Text"),
    NEWS_TITLE("home.newsTitle"),
    NEWS_MORE("home.newsMore"),
    POLL_TITLE("home.pollTitle");

    private final String key;

    SiteTextKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Optional<SiteTextKey> fromKey(String key) {
        return Arrays.stream(values())
                .filter(value -> value.key.equals(key))
                .findFirst();
    }
}
