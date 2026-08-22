package api.anticorruption.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Foydali havola: vazirlik, Bosh prokuratura, ishonch telefonlari va boshqalar.
 */
@Entity
@Table(name = "useful_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsefulLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 300)
    private String description;

    /** Havolalarni guruhlash uchun, masalan "Davlat organlari". */
    @Column(name = "group_name", length = 100)
    private String groupName;

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
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
