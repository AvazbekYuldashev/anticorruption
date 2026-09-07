package api.anticorruption.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Ishlab chiqarish rejimida xavfli sozlamalar bilan ishga tushishga yo'l qo'ymaydi.
 *
 * <p>Eng ko'p uchraydigan xavfsizlik xatosi - ilovani ishlab chiqish
 * kalitlari bilan haqiqiy serverga chiqarish. Bunday holatda JWT kaliti
 * omma uchun ma'lum bo'ladi va istalgan odam administrator tokenini
 * o'zi yasab olishi mumkin.
 *
 * <p>Shuning uchun {@code prod} profilida ishga tushish paytida standart
 * qiymatlar tekshiriladi va topilsa ilova umuman ko'tarilmaydi: xato
 * bilan ishlagandan ko'ra ishlamagani xavfsizroq.
 */
@Slf4j
@Component
@Profile("prod")
public class ProductionSafetyCheck {

    private static final String DEV_JWT_SECRET =
            "dev-only-secret-key-change-me-in-production-32bytes-min";
    private static final String DEV_POLL_SALT = "dev-only-poll-salt-change-me-in-production";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin12345!";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.poll.vote-salt}")
    private String pollSalt;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    @Value("${app.auth.secure:false}")
    private boolean cookieSecure;

    @Value("${app.auth.same-site:Lax}")
    private String cookieSameSite;

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        List<String> problems = new ArrayList<>();

        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            problems.add("APP_JWT_SECRET hali ishlab chiqish kaliti - uni almashtiring");
        }
        if (jwtSecret == null || jwtSecret.getBytes().length < 32) {
            problems.add("APP_JWT_SECRET kamida 32 bayt bo'lishi kerak");
        }
        if (DEV_POLL_SALT.equals(pollSalt)) {
            problems.add("APP_POLL_SALT hali ishlab chiqish qiymati - uni almashtiring");
        }
        if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword)) {
            problems.add("APP_ADMIN_PASSWORD standart parol - uni almashtiring");
        }
        if (allowedOrigins != null && allowedOrigins.contains("localhost")) {
            problems.add("APP_CORS_ORIGINS da localhost qolgan - haqiqiy domenni ko'rsating");
        }
        if (!cookieSecure) {
            problems.add("app.auth.secure=false - seans cookie'lari HTTPS'siz ham yuboriladi");
        }
        if ("None".equalsIgnoreCase(cookieSameSite) && !cookieSecure) {
            problems.add("SameSite=None faqat secure=true bilan ishlaydi");
        }
        // Ishlab chiqarishda sxemani Hibernate emas, migratsiya boshqaradi.
        if ("update".equals(ddlAuto) || "create".equals(ddlAuto) || "create-drop".equals(ddlAuto)) {
            problems.add("spring.jpa.hibernate.ddl-auto=" + ddlAuto
                    + " - ishlab chiqarishda 'validate' bo'lishi kerak");
        }

        if (!problems.isEmpty()) {
            problems.forEach(problem -> log.error("Ishlab chiqarish sozlamasi xato: {}", problem));
            throw new IllegalStateException(
                    "Ilova ishlab chiqarish rejimida xavfsiz emas: " + String.join("; ", problems));
        }

        log.info("Ishlab chiqarish sozlamalari tekshirildi - muammo topilmadi");
    }
}
