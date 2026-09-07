package api.anticorruption.attachment;

import api.anticorruption.complaint.Complaint;
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
 * Murojaatga biriktirilgan dalil fayli.
 *
 * <p>Faylning o'zi diskda saqlanadi, bazada esa faqat metama'lumot turadi.
 * {@code storedName} - diskdagi nom (foydalanuvchi kiritgan nomdan ajratilgan,
 * chunki asl nom xavfli belgilar saqlashi mumkin).
 */
@Entity
@Table(
        name = "attachments",
        indexes = @Index(name = "idx_attachments_complaint", columnList = "complaint_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /** Foydalanuvchi yuklagan asl fayl nomi - faqat ko'rsatish uchun. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** Diskdagi noyob nom, masalan "8f3a...c1.pdf". */
    @Column(name = "stored_name", nullable = false, unique = true, length = 120)
    private String storedName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
