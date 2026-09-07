package api.anticorruption.auth;

import api.anticorruption.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Uzoq muddatli yangilash tokeni.
 *
 * <p>Tokenning o'zi bazada saqlanmaydi - faqat uning SHA-256 xeshi. Baza
 * nusxasi qo'lga tushsa ham undan ishlaydigan token tiklab bo'lmaydi
 * (parol xeshlari bilan bir xil mantiq).
 *
 * <p>Har bir yangilashda joriy token bekor qilinadi va yangisi beriladi
 * (rotation). Aynan shunday aylantirilgan token qaytadan kelsa - bu
 * o'g'irlanish alomati: o'sha foydalanuvchining barcha tokenlari bekor
 * qilinadi. Qarang {@link RefreshTokenRevocation}.
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_refresh_tokens_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tokenning SHA-256 xeshi, o'n oltilik ko'rinishda (64 belgi). */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Bekor qilingan vaqt; null bo'lsa token hali amal qiladi. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Bekor qilinish sababi; token amal qilayotgan bo'lsa null.
     *
     * <p>Faqat aylantirilgan tokenning qaytib kelishi o'g'irlanish alomati -
     * qarang {@link RefreshTokenRevocation}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 20)
    private RefreshTokenRevocation revokedReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
