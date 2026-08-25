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

/**
 * Albom blokidagi bitta rasm.
 *
 * <p>Yakka rasm bloki ({@code Type.IMAGE}) rasmni o'zida saqlaydi - u blokning
 * xossasi. Albom ({@code Type.GALLERY}) esa to'plam, shuning uchun uning
 * rasmlari alohida jadvalda va o'z tartibiga ega.
 */
@Entity
@Table(
        name = "news_block_images",
        indexes = @Index(name = "idx_news_block_images_block", columnList = "block_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsBlockImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private NewsBlock block;

    /** Ochiq zonadagi rasm nomi; {@code /api/v1/media/{nom}} orqali olinadi. */
    @Column(name = "stored_name", nullable = false, length = 120)
    private String storedName;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(length = 300)
    private String caption;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
