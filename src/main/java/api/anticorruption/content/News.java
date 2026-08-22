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
 * Yangilik yoki e'lon.
 *
 * <p>Qoralama sifatida yaratiladi ({@code published=false}) va tayyor bo'lgach
 * chop etiladi. Chop etilmagan yangilikni faqat xodimlar ko'radi.
 */
@Entity
@Table(
        name = "news",
        indexes = {
                @Index(name = "idx_news_slug", columnList = "slug", unique = true),
                @Index(name = "idx_news_published_at", columnList = "published_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL uchun qulay nom, sarlavhadan avtomatik yasaladi. */
    @Column(nullable = false, length = 140)
    private String slug;

    @Column(nullable = false, length = 250)
    private String title;

    /** Ro'yxatda ko'rsatiladigan qisqa mazmun. */
    @Column(length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    /** Ochiq zonadagi rasm nomi; {@code /api/v1/media/{nom}} orqali olinadi. */
    @Column(name = "cover_image", length = 120)
    private String coverImage;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    /** Birinchi marta chop etilgan vaqt. Ro'yxat shu bo'yicha saralanadi. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private long viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
