package api.anticorruption.content;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * "Bo'lim haqida" sahifasi - saytda bitta nusxada bo'ladi.
 *
 * <p>Erkin matnli sahifa o'rniga tayyor tuzilma: sarlavha, kirish matni,
 * vazifalar ro'yxati va maqsad. Shu tufayli sahifa admin nima kiritishidan
 * qat'i nazar bir xil va tartibli ko'rinadi - HTML yozish talab qilinmaydi.
 */
@Entity
@Table(name = "about_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AboutSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sahifa sarlavhasi, masalan institut nomi. */
    @Column(length = 250)
    private String title;

    /** Kirish matni - bo'limning tashkil etilishi va tarixi. */
    @Column(columnDefinition = "text")
    private String body;

    /** Ro'yxat sarlavhasi, masalan "Bo'limning asosiy vazifalari:". */
    @Column(name = "tasks_title", length = 250)
    private String tasksTitle;

    /**
     * Vazifalar ro'yxati - saytda belgilangan ro'yxat bo'lib chiqadi.
     *
     * <p>Alohida entity emas: bandlarning matnidan boshqa xossasi yo'q,
     * tartib esa {@code @OrderColumn} bilan saqlanadi.
     */
    @ElementCollection
    @CollectionTable(name = "about_tasks", joinColumns = @JoinColumn(name = "about_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "text", length = 500, nullable = false)
    @Builder.Default
    private List<String> tasks = new ArrayList<>();

    /** Yakuniy xatboshi - bo'limning maqsadi. */
    @Column(columnDefinition = "text")
    private String goal;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
