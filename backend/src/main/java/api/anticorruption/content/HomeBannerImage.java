package api.anticorruption.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Bosh banner fonidagi bitta rasm.
 *
 * <p>Rasm bitta bo'lsa u doim turadi, bir nechta bo'lsa albom sifatida navbat
 * bilan almashadi. Fayl {@code StorageArea.PUBLIC} zonasida.
 */
@Entity
@Table(
        name = "home_banner_images",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_home_banner_images_stored_name",
                columnNames = "stored_name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeBannerImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ochiq zonadagi noyob nom; {@code /api/v1/media/{nom}} orqali olinadi. */
    @Column(name = "stored_name", nullable = false, length = 120)
    private String storedName;

    /** Yuklangan asl nom - faqat admin panelida ko'rsatish uchun. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** Albomdagi tartib: kichik raqam oldinda. */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
