package api.anticorruption.poll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * So'rovnoma yoki testlar guruhi.
 *
 * <p>Administrator o'zi nomlaydi ("2026 kuzgi anketalar"), shuning uchun enum
 * emas, jadval. Guruh faqat tartibga solish vositasi: unga biriktirilmagan
 * so'rovnoma ham to'liq ishlaydi, guruh o'chirilsa esa so'rovnomalar
 * "Guruhsiz" bo'limiga qaytadi.
 *
 * <p>Guruh turi bilan bog'langan: so'rovnomalar va testlar admin panelida
 * alohida sahifada, umumiy guruh ikkalasida ham yarmi bo'sh ko'rinardi.
 */
@Entity
@Table(
        name = "poll_groups",
        indexes = @Index(name = "idx_poll_groups_name", columnList = "type, name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    /** Guruh so'rovnomalarniki yoki testlarniki. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollType type;

    /** Ro'yxatdagi tartib; teng bo'lsa nom bo'yicha saralanadi. */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /**
     * Test guruhida: har bir ishtirokchiga nechta savol beriladi.
     *
     * <p>Guruhdagi testlarga katta savollar bazasi kiritiladi (200-300 ta),
     * ishtirokchi esa testni boshlaganda undan shuncha savolni tasodifiy oladi
     * ({@link PollAttempt}) - bitta savol ikki marta tushmaydi. Masalan,
     * 200 ta savoldan 50 tasi.
     *
     * <p>null bo'lsa barcha savollar kiritilgan tartibda beriladi. So'rovnomalar
     * guruhida ishlatilmaydi: so'rovnoma barcha savollarining javobini yig'adi.
     */
    @Column(name = "questions_per_attempt")
    private Integer questionsPerAttempt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
