package api.anticorruption.poll;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ommaviy so'rovnoma - masalan "Institutda korrupsiya darajasini qanday baholaysiz?".
 *
 * <p>Ovoz berish autentifikatsiyasiz ochiq, shuning uchun bir odam ko'p marta
 * ovoz bermasligi {@link PollVote#getVoterKey()} orqali cheklanadi.
 */
@Entity
@Table(name = "polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String question;

    @Column(length = 1000)
    private String description;

    /** false bo'lsa so'rovnoma ochiq ro'yxatda ko'rinmaydi va ovoz qabul qilinmaydi. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** true bo'lsa bir nechta variant tanlash mumkin. */
    @Column(name = "multiple_choice", nullable = false)
    @Builder.Default
    private boolean multipleChoice = false;

    /** Ovoz berish oynasi. null bo'lsa cheklov yo'q. */
    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    /**
     * Ovoz bergan odamlar soni (ovozlar soni emas - ko'p tanlovli so'rovnomada farq qiladi).
     * Foizni hisoblashda maxraj sifatida ishlatiladi.
     */
    @Column(name = "voter_count", nullable = false)
    @Builder.Default
    private long voterCount = 0;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<PollOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addOption(PollOption option) {
        options.add(option);
        option.setPoll(this);
    }

    /** Hozir ovoz berish mumkinmi: faol va vaqt oynasi ichida. */
    public boolean isOpenForVoting() {
        if (!active) {
            return false;
        }
        Instant now = Instant.now();
        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }
        return endsAt == null || !now.isAfter(endsAt);
    }
}
