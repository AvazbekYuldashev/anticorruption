package api.anticorruption.bootstrap;

import api.anticorruption.user.Role;
import api.anticorruption.user.User;
import api.anticorruption.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Birinchi ishga tushirishda bitta administrator yaratadi - aks holda
 * tizimga hech kim kira olmaydi va rol bera olmaydi ("tovuq va tuxum" muammosi).
 *
 * <p>Agar bazada allaqachon administrator bo'lsa, hech narsa qilmaydi.
 * Mavjud hisobning paroli hech qachon qayta yozilmaydi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    /** application.properties dagi dev-parol. Ishlab chiqarishda albatta o'zgartirilishi kerak. */
    private static final String DEFAULT_DEV_PASSWORD = "Admin12345!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminProperties.isConfigured()) {
            log.warn("app.admin.email yoki app.admin.password sozlanmagan - administrator yaratilmadi");
            return;
        }

        String email = adminProperties.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Administrator allaqachon mavjud: {}", email);
            warnAboutDefaultPassword();
            return;
        }

        User admin = User.builder()
                .fullName(adminProperties.fullName())
                .email(email)
                .passwordHash(passwordEncoder.encode(adminProperties.password()))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        log.info("Boshlang'ich administrator yaratildi: {}", email);
        warnAboutDefaultPassword();
    }

    private void warnAboutDefaultPassword() {
        if (DEFAULT_DEV_PASSWORD.equals(adminProperties.password())) {
            log.warn("DIQQAT: administrator standart parolda ishlamoqda. "
                    + "Ishlab chiqarishga chiqarishdan oldin APP_ADMIN_PASSWORD ni o'zgartiring.");
        }
    }
}
