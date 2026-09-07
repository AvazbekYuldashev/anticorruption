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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** So'rovnoma savolining javob varianti. */
@Entity
@Table(
        name = "poll_options",
        indexes = @Index(name = "idx_poll_options_question", columnList = "question_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Variant tegishli savol.
     *
     * <p>Ustun bazada null bo'lishi mumkin, chunki u mavjud jadvalga keyin
     * qo'shilgan - ilgari variant to'g'ridan-to'g'ri so'rovnomaga bog'langan
     * edi. Eski yozuvlarni {@code PollQuestionMigration} to'ldiradi, yangi
     * yozuvda esa savol har doim qo'yiladi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private PollQuestion question;

    @Column(nullable = false, length = 250)
    private String text;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /**
     * Test rejimida to'g'ri javob ekanini bildiradi; so'rovnomada ishlatilmaydi.
     *
     * <p>Bazada null bo'lishi mumkin - ustun to'ldirilgan jadvalga keyin
     * qo'shilgan. Eski variantlar to'g'ri emas deb o'qiladi.
     */
    @Column(name = "correct")
    private Boolean correct;

    /**
     * Shu variantga berilgan ovozlar soni.
     * Har safar {@code poll_votes} jadvalini sanamaslik uchun shu yerda saqlanadi.
     */
    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private long voteCount = 0;

    /** Ustun to'ldirilmagan eski variantlar to'g'ri emas hisoblanadi. */
    public boolean isCorrectAnswer() {
        return correct != null && correct;
    }
}
