package api.anticorruption.poll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Bitta ovoz: bir ishtirokchining bir savoldagi bir tanlovi.
 *
 * <p>{@code voterKey} - ovoz beruvchining anonimlashtirilgan belgisi. Bu IP
 * manzil emas: IP va brauzer ma'lumoti maxfiy tuz bilan birga xeshlanadi,
 * shuning uchun bazadan hech kimning IP sini tiklab bo'lmaydi, lekin bir
 * odamning ikkinchi marta ovoz berishini aniqlash mumkin.
 *
 * <p>Savol alohida saqlanadi: statistikani savol kesimida hisoblash uchun
 * har safar variantga o'tib borish shart bo'lmaydi.
 */
@Entity
@Table(
        name = "poll_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_poll_votes_option_voter",
                columnNames = {"option_id", "voter_key"}),
        indexes = @Index(name = "idx_poll_votes_question", columnList = "question_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    /**
     * Ovoz berilgan savol.
     *
     * <p>{@link PollOption#getQuestion()} kabi, ustun mavjud jadvalga keyin
     * qo'shilgani uchun bazada null bo'lishi mumkin; eski yozuvlarni
     * ko'chirish to'ldiradi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private PollQuestion question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @Column(name = "voter_key", nullable = false, length = 64)
    private String voterKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
