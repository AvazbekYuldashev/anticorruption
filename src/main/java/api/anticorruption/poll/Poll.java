package api.anticorruption.poll;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ommaviy so'rovnoma - bir yoki bir nechta savoldan iborat.
 *
 * <p>So'rovnoma mavsumiy ham, doimiy ham bo'lishi mumkin: {@code startsAt} va
 * {@code endsAt} qo'yilsa u faqat shu oraliqda ovoz qabul qiladi, qo'yilmasa
 * yopilgunicha ochiq turadi.
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

    /**
     * So'rovnoma sarlavhasi.
     *
     * <p>Bazada null bo'lishi mumkin, chunki ustun mavjud jadvalga keyin
     * qo'shilgan ({@code ddl-auto=update} to'ldirilgan jadvalga NOT NULL
     * ustun qo'sha olmaydi). Kod uni har doim to'ldiradi - bo'shligini
     * {@code PollService} tekshiradi.
     */
    @Column(length = 300)
    private String title;

    @Column(length = 1000)
    private String description;

    /** false bo'lsa so'rovnoma ochiq ro'yxatda ko'rinmaydi va ovoz qabul qilinmaydi. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Ovoz berish oynasi. null bo'lsa cheklov yo'q. */
    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    /**
     * Administrator qo'lda to'xtatgan vaqt. null bo'lsa to'xtatilmagan.
     *
     * <p>Muddat tugashidan farq qiladi: to'xtatilgan so'rovnoma saytda
     * umuman ko'rinmaydi, muddati tugagani esa natijalari bilan qoladi.
     */
    @Column(name = "stopped_at")
    private Instant stoppedAt;

    /**
     * Nechanchi marta o'tkazilayotgani.
     *
     * <p>Bazada null bo'lishi mumkin - ustun mavjud jadvalga keyin
     * qo'shilgan; eski yozuvlar birinchi o'tkazish deb qaraladi.
     */
    @Column(name = "run_number")
    private Integer runNumber;

    /**
     * Shu so'rovnoma qaysi o'tkazishning takrori ekani.
     *
     * <p>Qayta o'tkazish eski yozuvni o'zgartirmaydi, yangi so'rovnoma
     * ochadi: shunda eski hisobot butunligicha qoladi va yangi ovozlar
     * unga qo'shilib ketmaydi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_poll_id")
    private Poll previousPoll;

    /**
     * Ishtirokchilar soni - ovozlar soni emas.
     *
     * <p>Bir ishtirokchi bir necha savolga javob beradi, ko'p tanlovli
     * savolda esa bir necha variant belgilaydi. Foiz har bir savolning
     * o'z javob berganlari soniga nisbatan hisoblanadi.
     */
    @Column(name = "voter_count", nullable = false)
    @Builder.Default
    private long voterCount = 0;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<PollQuestion> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addQuestion(PollQuestion question) {
        questions.add(question);
        question.setPoll(this);
    }

    /** Hozir ovoz berish mumkinmi: faol va vaqt oynasi ichida. */
    public boolean isOpenForVoting() {
        return status() == PollStatus.OPEN;
    }

    public PollStatus status() {
        if (!active) {
            return PollStatus.DRAFT;
        }
        // Qo'lda to'xtatish muddatdan ustun: administrator qarori sanadan kuchliroq.
        if (stoppedAt != null) {
            return PollStatus.STOPPED;
        }
        Instant now = Instant.now();
        if (startsAt != null && now.isBefore(startsAt)) {
            return PollStatus.SCHEDULED;
        }
        if (endsAt != null && now.isAfter(endsAt)) {
            return PollStatus.CLOSED;
        }
        return PollStatus.OPEN;
    }

    /** Eski yozuvlarda ustun bo'lmagani uchun null birinchi o'tkazish deb o'qiladi. */
    public int runNumberOrFirst() {
        return runNumber == null ? 1 : runNumber;
    }
}
