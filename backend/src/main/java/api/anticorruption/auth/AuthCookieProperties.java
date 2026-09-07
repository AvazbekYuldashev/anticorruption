package api.anticorruption.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Seans cookie'lari sozlamalari ("app.auth.*").
 *
 * @param accessCookie      qisqa muddatli JWT saqlanadigan cookie nomi
 * @param refreshCookie     yangilash tokeni cookie nomi
 * @param secure            HTTPS talab qilinsinmi; ishlab chiqarishda doim true
 * @param sameSite          "Lax" odatda yetarli: cross-site POST cookie'siz keladi
 * @param domain            bo'sh bo'lsa cookie faqat joriy hostga tegishli bo'ladi
 * @param refreshCookiePath yangilash cookie'si faqat shu yo'lga yuboriladi
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthCookieProperties(

        @DefaultValue("ac_access") String accessCookie,

        @DefaultValue("ac_refresh") String refreshCookie,

        @DefaultValue("false") boolean secure,

        @DefaultValue("Lax") String sameSite,

        String domain,

        @DefaultValue("/api/v1/auth") String refreshCookiePath
) {
}
