package api.anticorruption.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Kamdan-kam o'zgaradigan matnli sahifa: "Bo'lim haqida", "Normativ hujjatlar",
 * "Bog'lanish" va shunga o'xshashlar.
 *
 * <p>Har bir sahifa {@code slug} orqali chaqiriladi, masalan
 * {@code /api/v1/pages/about}. Shu tufayli yangi sahifa qo'shish uchun
 * kod yozish shart emas - admin uni panelda yaratadi.
 */
@Entity
@Table(
        name = "static_pages",
        indexes = @Index(name = "idx_static_pages_slug", columnList = "slug", unique = true)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaticPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    /** Menyuda ko'rsatish tartibi. */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 100;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
