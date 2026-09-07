package api.anticorruption.auth;

import api.anticorruption.common.exception.UnauthorizedException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.security.JwtProperties;
import api.anticorruption.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Yangilash tokenlarini beradi, aylantiradi va bekor qiladi.
 *
 * <p>Token - tasodifiy 32 bayt. U JWT emas: ichida hech qanday ma'lumot
 * yo'q va u faqat bazadagi yozuvga ishora qiladi. Shu tufayli tokenni
 * bekor qilish mumkin - JWT bilan buni qilib bo'lmaydi, chunki imzo
 * to'g'ri bo'lsa server uni qabul qilaverardi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;
    private final SecureRandom random = new SecureRandom();

    /** Yangi token beradi va uning ochiq qiymatini qaytaradi (u faqat cookie ga tushadi). */
    @Transactional
    public String issue(User user) {
        repository.deleteByUserIdAndExpiresAtBefore(user.getId(), Instant.now());
        return persist(user);
    }

    /**
     * Tokenni tekshiradi, bekor qiladi va o'rniga yangisini beradi.
     *
     * <p>{@code noRollbackFor}: qayta ishlatish aniqlanganda barcha tokenlarni
     * bekor qilamiz va shundan keyin xatolik tashlaymiz. Odatiy holatda
     * xatolik tranzaksiyani orqaga qaytarardi - ya'ni himoya amalga
     * oshmasdan qolardi.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken stored = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(RefreshTokenService::invalid);

        if (stored.getRevokedAt() != null) {
            /*
             * Aylantirilgan token qaytib keldi: mijozda uning o'rnida yangisi
             * turishi kerak edi, demak zanjir ikki qo'lda - nusxasi birovda.
             * Ataylab bekor qilingani (chiqish, parol almashtirish) esa
             * shunchaki eskirgan cookie: uni rad etamiz, lekin qolgan
             * seanslarni uzishga asos yo'q. Aks holda parolini almashtirgan
             * odam boshqa qurilmasining navbatdagi yangilashi tufayli o'zi
             * ham tizimdan chiqib qolardi.
             */
            if (stored.getRevokedReason() == RefreshTokenRevocation.ROTATED) {
                log.warn("Aylantirilgan yangilash tokeni qayta ishlatildi: foydalanuvchi={}",
                        stored.getUser().getId());
                revokeAllForUser(stored.getUser().getId());
            }
            throw invalid();
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw invalid();
        }

        User user = stored.getUser();
        if (!user.isEnabled()) {
            revokeAllForUser(user.getId());
            throw invalid();
        }

        stored.setRevokedAt(Instant.now());
        stored.setRevokedReason(RefreshTokenRevocation.ROTATED);
        return new Rotation(user, persist(user));
    }

    /** Chiqishda: shu bitta tokenni bekor qiladi. Noma'lum token e'tiborsiz qoldiriladi. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    token.setRevokedReason(RefreshTokenRevocation.REVOKED);
                });
    }

    /** Foydalanuvchining barcha seanslarini uzadi (parol almashtirilganda). */
    @Transactional
    public void revokeAll(Long userId) {
        revokeAllForUser(userId);
    }

    /** Ataylab uzish - shuning uchun sabab har doim {@code REVOKED}. */
    private void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, Instant.now(), RefreshTokenRevocation.REVOKED);
    }

    private String persist(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(raw))
                .expiresAt(Instant.now().plus(jwtProperties.refreshExpiration()))
                .build());

        return raw;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 mavjud emas", ex);
        }
    }

    /** Sabab ataylab aniqlashtirilmaydi: mijozga token yo'qmi yoki eskirganmi - farqi yo'q. */
    private static UnauthorizedException invalid() {
        return new UnauthorizedException(MessageKeys.ERROR_AUTH_REFRESH_INVALID);
    }

    /** Aylantirish natijasi: token egasi va yangi token. */
    public record Rotation(User user, String refreshToken) {
    }
}
