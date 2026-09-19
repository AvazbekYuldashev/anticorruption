package api.anticorruption.poll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ishtirokchiga tushgan savollar to'plami - savollari tasodifiy tanlanadigan testda.
 *
 * <p>To'plam ishtirokchi testni boshlaganda bir marta tanlanadi va saqlanadi.
 * Qayta boshlasa yoki sahifani yangilasa o'sha to'plam qaytadi: aks holda
 * "osonroq" savollar chiqquncha qayta-qayta boshlash mumkin bo'lardi.
 * Javoblar ham aynan shu savollarga qarab tekshiriladi va baholanadi.
 *
 * <p>Savollar tashqi kalit bilan emas, id lar ro'yxati sifatida saqlanadi:
 * administrator testni tahrirlab savolni o'chirsa, boshlangan to'plamlar uni
 * shunchaki tashlab ketadi ({@code PollAttemptService#questionsOf}).
 */
@Entity
@Table(
        name = "poll_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_poll_attempts_poll_voter",
                        columnNames = {"poll_id", "voter_key"}),
                @UniqueConstraint(name = "uq_poll_attempts_token", columnNames = "token")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    /** To'plam egasi - {@link PollVote#getVoterKey()} bilan bir xil belgi. */
    @Column(name = "voter_key", nullable = false, length = 64)
    private String voterKey;

    /**
     * Testni boshlaganda ishtirokchiga beriladigan tasodifiy belgi.
     *
     * <p>Anonim ishtirokchi IP manzili bo'yicha taniladi, u esa uzun test
     * davomida o'zgarishi mumkin (mobil internet). Javob shu belgi bilan
     * to'plamiga bog'lanadi va ishtirokchi "boshqa odam" bo'lib qolmaydi.
     */
    @Column(nullable = false, length = 36)
    private String token;

    /** Tushgan savollarning id lari, ishtirokchiga ko'rsatiladigan tartibda: "12,5,40". */
    @Column(name = "question_ids", nullable = false, columnDefinition = "text")
    private String questionIds;

    /**
     * Ishtirokchiga berilgan savollar soni.
     *
     * <p>Hisobotda o'rtacha natija shunga nisbatan olinadi: test davomida
     * savollar soni o'zgartirilsa ham har bir ishtirokchi o'zi olgan savollar
     * bo'yicha baholanadi.
     */
    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Javoblar yuborilgan vaqt; test hali ishlanayotgan bo'lsa null. */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    public List<Long> questionIdList() {
        List<Long> ids = new ArrayList<>();
        if (questionIds == null) {
            return ids;
        }
        for (String part : questionIds.split(",")) {
            if (!part.isBlank()) {
                ids.add(Long.valueOf(part.trim()));
            }
        }
        return ids;
    }

    /** Tushgan savollarni yozib qo'yadi - berilgan tartibida. */
    public void assignQuestions(List<PollQuestion> questions) {
        this.questionIds = questions.stream()
                .map(question -> String.valueOf(question.getId()))
                .collect(Collectors.joining(","));
        this.questionCount = questions.size();
    }
}
