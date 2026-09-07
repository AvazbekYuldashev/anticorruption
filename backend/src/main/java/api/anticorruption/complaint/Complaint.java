package api.anticorruption.complaint;

import api.anticorruption.attachment.Attachment;
import api.anticorruption.university.Department;
import api.anticorruption.university.Faculty;
import api.anticorruption.user.User;
import jakarta.persistence.CascadeType;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Universitetdagi korrupsiya holati haqidagi murojaat.
 *
 * <p>Murojaat ikki xil yo'l bilan kelishi mumkin:
 * <ul>
 *   <li>ro'yxatdan o'tgan foydalanuvchi tomonidan - {@code author} to'ldiriladi;</li>
 *   <li>anonim - {@code author} bo'sh qoladi, faqat {@code trackingCode} orqali kuzatiladi.</li>
 * </ul>
 *
 * <p>Fakultet va kafedra ixtiyoriy: murojaat butun universitetga taalluqli
 * bo'lishi mumkin (masalan rektorat yoki qabul komissiyasi haqida).
 */
@Entity
@Table(
        name = "complaints",
        indexes = {
                @Index(name = "idx_complaints_tracking_code", columnList = "tracking_code", unique = true),
                @Index(name = "idx_complaints_status", columnList = "status"),
                @Index(name = "idx_complaints_category", columnList = "category"),
                @Index(name = "idx_complaints_faculty", columnList = "faculty_id"),
                @Index(name = "idx_complaints_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Murojaatchi holatni tekshirish uchun ishlatadigan noyob kod, masalan "AC-2026-K7M2Q4". */
    @Column(name = "tracking_code", nullable = false, length = 20)
    private String trackingCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ComplaintCategory category;

    // ------------------------------------------------------------- universitet konteksti

    /** Holat qaysi fakultetga tegishli. Universitet miqyosidagi murojaatda null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    /** Holat qaysi kafedraga tegishli. Har doim {@code faculty} tarkibida bo'ladi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Qaysi fan bo'yicha, masalan "Oliy matematika". */
    @Column(name = "subject_name", length = 200)
    private String subjectName;

    /** Kimning harakati haqida - lavozim darajasida, ism emas. */
    @Enumerated(EnumType.STRING)
    @Column(name = "accused_position", length = 40)
    private AccusedPosition accusedPosition;

    // ------------------------------------------------------------- voqea

    /** Voqea sodir bo'lgan sana (taxminiy bo'lishi mumkin). */
    @Column(name = "incident_date")
    private LocalDate incidentDate;

    /** Voqea joyi va vaqti erkin matnda, masalan "2-bino, 305-xona, imtihon paytida". */
    @Column(name = "incident_place", length = 250)
    private String incidentPlace;

    // ------------------------------------------------------------- murojaatchi

    /** true bo'lsa murojaatchi shaxsi saqlanmaydi. */
    @Column(nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    /** Murojaatchining universitetga aloqadorligi - anonim murojaatda ham so'raladi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false, length = 30)
    private ReporterType reporterType;

    /** Nechanchi kurs - faqat talabalar uchun. */
    @Column(name = "course_year")
    private Integer courseYear;

    @Column(name = "group_name", length = 50)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_form", length = 20)
    private StudyForm studyForm;

    /** Anonim bo'lmagan, lekin ro'yxatdan o'tmagan murojaatchining aloqa ma'lumotlari. */
    @Column(name = "reporter_name", length = 150)
    private String reporterName;

    /** Holat o'zgarganda xabarnoma yuborish uchun. Anonim murojaatda ham bo'lishi mumkin. */
    @Column(name = "reporter_email", length = 180)
    private String reporterEmail;

    @Column(name = "reporter_phone", length = 30)
    private String reporterPhone;

    /** Ro'yxatdan o'tgan muallif (anonim murojaatda null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    // ------------------------------------------------------------- ko'rib chiqish

    /** Murojaatni ko'rib chiqayotgan moderator. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * Murojaat yuborilgan til kodi, masalan "uz" yoki "ru".
     *
     * <p>Email xabarnomalar shu tilda yuboriladi. Til saqlanmasa, xabarnoma
     * serverning standart tilida ketardi - murojaatchi esa boshqa tilda
     * yozgan bo'lishi mumkin.
     */
    @Column(name = "locale", length = 10)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.NEW;

    /** Xodimning rasmiy javobi - murojaatchiga ko'rinadi. */
    @Column(name = "official_response", columnDefinition = "text")
    private String officialResponse;

    /**
     * true bo'lsa murojaat ochiq reyestrda ko'rsatilmaydi.
     * Xodim tafsilotlar murojaatchini oshkor qilishi mumkin deb hisoblasa qo'yadi.
     */
    @Column(name = "hidden_from_register", nullable = false)
    @Builder.Default
    private boolean hiddenFromRegister = false;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<ComplaintStatusHistory> history = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Yakuniy holatga o'tgan vaqt. */
    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Qabul qilinganidan yopilgunicha o'tgan vaqt (daqiqalarda).
     * Yopilganda bir marta hisoblanadi - o'rtacha muddat statistikasini
     * bazaga xos sana funksiyalarisiz hisoblash uchun.
     */
    @Column(name = "resolution_minutes")
    private Long resolutionMinutes;

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
        attachment.setComplaint(this);
    }

    public void addHistoryEntry(ComplaintStatusHistory entry) {
        history.add(entry);
        entry.setComplaint(this);
    }

    /** Xabarnoma yuborish uchun email: muallifniki yoki murojaatda ko'rsatilgani. */
    public String notificationEmail() {
        if (author != null) {
            return author.getEmail();
        }
        return reporterEmail;
    }
}
