package api.anticorruption.poll;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * So'rovnomadagi bitta savol.
 *
 * <p>Savollar soni cheklanmagan: bitta savolli oddiy so'rovnoma ham,
 * uzun anketa ham shu model bilan ifodalanadi.
 */
@Entity
@Table(
        name = "poll_questions",
        indexes = @Index(name = "idx_poll_questions_poll", columnList = "poll_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = 300)
    private String text;

    /** true bo'lsa shu savolda bir nechta variant tanlash mumkin. */
    @Column(name = "multiple_choice", nullable = false)
    @Builder.Default
    private boolean multipleChoice = false;

    /** false bo'lsa ishtirokchi savolni tashlab ketishi mumkin. */
    @Column(nullable = false)
    @Builder.Default
    private boolean required = true;

    /**
     * Shu savolga javob bergan ishtirokchilar soni.
     *
     * <p>So'rovnomaning umumiy ishtirokchilar sonidan farq qilishi mumkin:
     * majburiy bo'lmagan savolni hamma ham javobsiz qoldirishi mumkin.
     * Foiz shu songa nisbatan hisoblanadi.
     */
    @Column(name = "answered_count", nullable = false)
    @Builder.Default
    private long answeredCount = 0;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    // Savollar bir so'rovda kelganda variantlar ham to'p-to'p yuklanadi:
    // aks holda har bir savol uchun alohida so'rov ketardi.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<PollOption> options = new ArrayList<>();

    public void addOption(PollOption option) {
        options.add(option);
        option.setQuestion(this);
    }
}
