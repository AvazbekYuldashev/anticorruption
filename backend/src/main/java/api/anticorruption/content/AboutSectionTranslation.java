package api.anticorruption.content;

import api.anticorruption.common.i18n.AppLanguage;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * "Bo'lim haqida" sahifasining bir tildagi matni.
 *
 * <p>Yangiliklardan farqli o'laroq bu sahifa struktura sifatida bitta:
 * sarlavha, kirish, vazifalar va maqsad har tilda ham shu tartibda
 * qoladi, faqat matn almashadi. Shuning uchun bu yerda alohida yozuv
 * emas, tarjima jadvali to'g'ri keladi.
 *
 * <p>Bo'sh qoldirilgan maydon asosiy tildagi matnga qaytadi - yarim
 * tarjima qilingan sahifa bo'sh joylar bilan chiqmasin.
 */
@Entity
@Table(
        name = "about_section_translations",
        indexes = @Index(name = "idx_about_translation", columnList = "about_id, language", unique = true)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AboutSectionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "about_id", nullable = false)
    private AboutSection about;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AppLanguage language;

    @Column(length = 250)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "tasks_title", length = 250)
    private String tasksTitle;

    @ElementCollection
    @CollectionTable(
            name = "about_translation_tasks",
            joinColumns = @JoinColumn(name = "translation_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "text", length = 500, nullable = false)
    @Builder.Default
    private List<String> tasks = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String goal;
}
