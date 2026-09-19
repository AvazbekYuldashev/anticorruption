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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Administrator o'zgartirgan sayt matni - bitta kalit, bitta til.
 *
 * <p>Faqat o'zgartirilgan matnlar saqlanadi: yozuv bo'lmasa sayt tarjima
 * faylidagi asl matnni ko'rsatadi. Qaysi matnlarni o'zgartirish mumkinligini
 * {@link SiteTextKey} belgilaydi.
 */
@Entity
@Table(
        name = "site_texts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_site_texts_key_language",
                columnNames = {"text_key", "language_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Interfeysdagi tarjima kaliti, masalan {@code home.heroTitle}. */
    @Column(name = "text_key", nullable = false, length = 64)
    private String textKey;

    /** {@link api.anticorruption.common.i18n.AppLanguage} kodi: uz, uz-cyrl, ru, en. */
    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "text_value", nullable = false, length = 1000)
    private String textValue;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
