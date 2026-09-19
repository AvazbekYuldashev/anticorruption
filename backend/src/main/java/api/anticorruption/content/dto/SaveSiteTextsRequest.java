package api.anticorruption.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Bosh sahifa matnlarini saqlash.
 *
 * <p>Ro'yxat to'liq holat sifatida yuboriladi: unda yo'q yoki bo'sh qiymatli
 * matn asl holiga qaytadi - sayt yana tarjima faylidagi matnni ko'rsatadi.
 *
 * @param texts o'zgartirilgan matnlar; chegara barcha kalitlar to'rt tilda
 *              bo'lganidan ancha katta, so'rov hajmini oqilona ushlab turish uchun
 */
public record SaveSiteTextsRequest(

        @NotNull(message = "{validation.required}")
        @Size(max = 200, message = "{validation.size.max}")
        @Valid
        List<Item> texts
) {
    /**
     * Bitta matn.
     *
     * @param key      tarjima kaliti, masalan {@code home.heroTitle}
     * @param language til kodi: uz, uz-cyrl, ru, en
     */
    public record Item(

            @NotBlank(message = "{validation.required}")
            String key,

            @NotBlank(message = "{validation.required}")
            String language,

            @NotNull(message = "{validation.required}")
            @Size(max = 1000, message = "{validation.size.max}")
            String value
    ) {
    }
}
