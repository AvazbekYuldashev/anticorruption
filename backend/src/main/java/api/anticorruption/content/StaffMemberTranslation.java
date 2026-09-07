package api.anticorruption.content;

import api.anticorruption.common.i18n.AppLanguage;
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

/**
 * Xodim ma'lumotlarining bir tildagi varianti.
 *
 * <p>Faqat matn tarjima qilinadi. Telefon, email, surat, tartib va
 * faollik holati tilga bog'liq emas - ular asosiy yozuvda qoladi va
 * bir joyda o'zgartiriladi.
 */
@Entity
@Table(
        name = "staff_member_translations",
        indexes = @Index(name = "idx_staff_translation", columnList = "member_id, language", unique = true)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMemberTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private StaffMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AppLanguage language;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(length = 200)
    private String position;

    @Column(name = "academic_degree", length = 150)
    private String academicDegree;

    @Column(columnDefinition = "text")
    private String biography;

    @Column(name = "reception_hours", length = 150)
    private String receptionHours;
}
