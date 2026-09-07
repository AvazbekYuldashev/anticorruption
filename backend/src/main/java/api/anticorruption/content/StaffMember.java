package api.anticorruption.content;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
 * Korrupsiyaga qarshi kurash bo'limi xodimi.
 *
 * <p>Bu tizim foydalanuvchisi ({@code User}) emas - bu saytda ko'rsatiladigan
 * ma'lumot. Bo'lim xodimi tizimga kirmasligi ham mumkin, va aksincha:
 * moderator hisobi bor odam ro'yxatda ko'rsatilmasligi mumkin.
 */
@Entity
@Table(name = "staff_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 200)
    private String position;

    /** Ilmiy daraja yoki unvon, masalan "PhD, dotsent". */
    @Column(name = "academic_degree", length = 150)
    private String academicDegree;

    /**
     * Qisqacha tarjimai hol: mehnat yo'li, ilmiy faoliyati, yutuqlari.
     *
     * <p>Uzunligi oldindan ma'lum emas, shuning uchun {@code text}: bir necha
     * jumla ham, to'liq biografiya ham sig'adi.
     */
    @Column(columnDefinition = "text")
    private String biography;

    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    /** Qabul vaqti, masalan "Dushanba-juma, 14:00-17:00". */
    @Column(name = "reception_hours", length = 150)
    private String receptionHours;

    /** Ochiq zonadagi surat nomi. */
    @Column(length = 120)
    private String photo;

    /** Ro'yxatdagi tartib: kichik raqam yuqorida turadi. */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 100;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    /** Boshqa tillardagi matn: ism, lavozim, unvon, biografiya, qabul vaqti. */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StaffMemberTranslation> translations = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
