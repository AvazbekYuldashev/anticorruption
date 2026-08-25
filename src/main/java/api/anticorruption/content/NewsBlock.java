package api.anticorruption.content;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Yangilik mazmunining bir bo'lagi: matn parchasi yoki rasm.
 *
 * <p>Yangilik bitta katta matn emas, bloklar ketma-ketligi: muharrirda
 * "+" bosib matn yoki rasm qo'shiladi va ular qanday tartibda kiritilgan
 * bo'lsa, saytda ham shunday chiqadi. Shu tufayli matn va rasm
 * aralashtirib joylanishi mumkin.
 *
 * <p>Bitta jadval ikkala turni ham saqlaydi: {@code text} faqat matn
 * blokida, {@code storedName} faqat rasm blokida to'ldiriladi. Ikkita
 * alohida jadval tartibni saqlashni murakkablashtirardi - blok raqami
 * ikkalasida ham yagona ketma-ketlikda bo'lishi kerak.
 */
@Entity
@Table(
        name = "news_blocks",
        indexes = @Index(name = "idx_news_blocks_news", columnList = "news_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsBlock {

    /** Blok turi. Foydalanuvchiga ko'rsatilmaydi, shuning uchun tarjima kerak emas. */
    public enum Type {
        /** Bo'lim sarlavhasi. */
        HEADING,

        /** Oddiy matn xatboshisi. */
        TEXT,

        /** Yakka rasm - matn oqimida to'liq kenglikda chiqadi. */
        IMAGE,

        /** Albom - bir nechta rasm to'r ko'rinishida. */
        GALLERY;

        public boolean isTextual() {
            return this == HEADING || this == TEXT;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    /** Matn bloki mazmuni. Rasm blokida null. */
    @Column(columnDefinition = "text")
    private String text;

    /** Ochiq zonadagi rasm nomi. Matn blokida null. */
    @Column(name = "stored_name", length = 120)
    private String storedName;

    /** Rasmning asl fayl nomi - admin panelida ko'rsatish uchun. */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /** Rasm ostidagi izoh. */
    @Column(length = 300)
    private String caption;

    /** Albom blokidagi rasmlar. Boshqa turlarda bo'sh. */
    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<NewsBlockImage> images = new ArrayList<>();

    /** Yangilik ichidagi tartib: kichik raqam yuqorida turadi. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public boolean isTextual() {
        return type != null && type.isTextual();
    }

    /** Blokdagi barcha rasm nomlari - yakka rasm ham, albom ham. */
    public List<String> imageNames() {
        if (type == Type.IMAGE) {
            return storedName == null ? List.of() : List.of(storedName);
        }
        return images.stream().map(NewsBlockImage::getStoredName).toList();
    }
}
