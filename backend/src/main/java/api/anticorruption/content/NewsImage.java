package api.anticorruption.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Yangilik albomidagi bitta rasm.
 *
 * <p>Muqova rasmidan ({@code News.coverImage}) farqi bor: muqova bitta va u
 * ro'yxatlarda ko'rsatiladi, albom esa yangilik ichida galereya sifatida
 * chiqadi va istalgancha rasmdan iborat bo'lishi mumkin.
 *
 * <p>Fayllar {@code StorageArea.PUBLIC} zonasida - ular hech qanday
 * tekshiruvsiz beriladi.
 */
@Entity
@Table(
        name = "news_images",
        indexes = @Index(name = "idx_news_images_news", columnList = "news_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    /** Ochiq zonadagi noyob nom; {@code /api/v1/media/{nom}} orqali olinadi. */
    @Column(name = "stored_name", nullable = false, unique = true, length = 120)
    private String storedName;

    /** Foydalanuvchi yuklagan asl nom - faqat admin panelida ko'rsatish uchun. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** Rasm ostidagi izoh. Hozircha ixtiyoriy, keyin admin paneldan tahrirlanadi. */
    @Column(length = 300)
    private String caption;

    /** Albomdagi tartib: kichik raqam oldinda turadi. */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
