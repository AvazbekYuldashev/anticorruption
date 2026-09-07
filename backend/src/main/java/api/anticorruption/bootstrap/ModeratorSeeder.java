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
 * Birinchi ishga tushirishda bitta moderator yaratadi, shunda tizim boshidanoq
 * ishga tayyor bo'ladi: murojaatlarni ko'rib chiqadigan odam allaqachon bor va
 * administrator uni qo'lda qo'shib o'tirmaydi.
 *
 * <p>{@link AdminSeeder} dan farqi ikkitada: bu hisob majburiy emas (sozlanmasa
 * shunchaki yaratilmaydi) va roli {@link Role#MODERATOR} - ya'ni u murojaatlar
 * va kontent bilan ishlaydi, foydalanuvchilar va fakultetlar bo'limiga kira
 * olmaydi.
 *
 * <p>Bazada shu emailli hisob bo'lsa hech narsa qilmaydi: mavjud hisobning
 * paroli ham, roli ham hech qachon qayta yozilmaydi. Shu sabab ilovani qayta
 * ishga tushirish xavfsiz - administrator moderatorni bloklagan yoki rolini
 * o'zgartirgan bo'lsa, keyingi ishga tushishda u tiklanib qolmaydi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModeratorSeeder implements ApplicationRunner {

    /** application.properties dagi dev-parol. Ishlab chiqarishda o'zgartirilishi kerak. */
    private static final String DEFAULT_DEV_PASSWORD = "Moderator12345!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModeratorProperties moderatorProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!moderatorProperties.isConfigured()) {
            log.info("app.moderator.* sozlanmagan - boshlang'ich moderator yaratilmadi");
            return;
        }

        String email = moderatorProperties.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Moderator allaqachon mavjud: {}", email);
            warnAboutDefaultPassword();
            return;
        }

        User moderator = User.builder()
                .fullName(moderatorProperties.fullName())
                .email(email)
                .passwordHash(passwordEncoder.encode(moderatorProperties.password()))
                .role(Role.MODERATOR)
                .enabled(true)
                .build();

        userRepository.save(moderator);

        log.info("Boshlang'ich moderator yaratildi: {}", email);
        warnAboutDefaultPassword();
    }

    private void warnAboutDefaultPassword() {
        if (DEFAULT_DEV_PASSWORD.equals(moderatorProperties.password())) {
            log.warn("DIQQAT: moderator standart parolda ishlamoqda. "
                    + "Ishlab chiqarishga chiqarishdan oldin APP_MODERATOR_PASSWORD ni o'zgartiring.");
        }
    }
}
