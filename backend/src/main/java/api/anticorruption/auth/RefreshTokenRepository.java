package api.anticorruption.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Foydalanuvchining barcha faol tokenlarini bekor qiladi.
     *
     * <p>Ikki holatda kerak: parol almashtirilganda va o'g'irlangan token
     * aniqlanganda - ikkalasida ham eski seanslar darrov uzilishi kerak.
     * Ikkalasi ham ataylab uzish, shuning uchun sabab har doim
     * {@link RefreshTokenRevocation#REVOKED}: bu tokenlarning qaytib kelishi
     * boshqa seanslarni uzishga asos bo'lmaydi.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now, t.revokedReason = :reason
             where t.user.id = :userId and t.revokedAt is null
            """)
    int revokeAllForUser(@Param("userId") Long userId,
                         @Param("now") Instant now,
                         @Param("reason") RefreshTokenRevocation reason);

    /** Eskirgan yozuvlar jadvalda to'planib qolmasin. */
    void deleteByUserIdAndExpiresAtBefore(Long userId, Instant cutoff);
}
