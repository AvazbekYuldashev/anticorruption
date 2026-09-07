package api.anticorruption.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Yangilik yaratish yoki tahrirlash.
 *
 * <p>Mazmun bloklar ketma-ketligi sifatida yuboriladi: ro'yxat qanday
 * tartibda kelsa, saytda ham shunday chiqadi. Ro'yxat to'liq almashtiriladi -
 * unda yo'q blok o'chiriladi.
 *
 * @param published     null bo'lsa holat o'zgarmaydi (yangi yangilik qoralama bo'ladi)
 * @param language      "uz", "uz-cyrl", "ru" yoki "en"; ko'rsatilmasa asosiy til
 * @param translationOf mavjud yangilikning id si - yangi yozuv o'shaning
 *                      tarjimasi sifatida bog'lanadi. Faqat yaratishda
 *                      ishlatiladi; tahrirlashda e'tiborga olinmaydi.
 */
public record SaveNewsRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 5, max = 250, message = "{validation.size}")
        String title,

        @Size(max = 500, message = "{validation.size.max}")
        String summary,

        Boolean published,

        @Size(max = 10, message = "{validation.size.max}")
        String language,

        Long translationOf,

        @Size(max = 200, message = "{validation.size.max}")
        @Valid
        List<SaveNewsBlockRequest> blocks
) {
    /**
     * Bitta blok.
     *
     * @param type       "HEADING", "TEXT", "IMAGE" yoki "GALLERY"
     * @param text       matn bloklari uchun ("HEADING", "TEXT")
     * @param storedName yakka rasm uchun - avval yuklangan faylning nomi
     *                   ({@code POST /api/v1/admin/media} qaytaradi)
     * @param images     albom uchun rasmlar ro'yxati
     */
    public record SaveNewsBlockRequest(

            @NotBlank(message = "{validation.required}")
            String type,

            @Size(max = 20000, message = "{validation.size.max}")
            String text,

            @Size(max = 120, message = "{validation.size.max}")
            String storedName,

            @Size(max = 255, message = "{validation.size.max}")
            String originalName,

            @Size(max = 300, message = "{validation.size.max}")
            String caption,

            @Size(max = 60, message = "{validation.size.max}")
            @Valid
            List<SaveNewsBlockImageRequest> images
    ) {
    }

    /** Albomdagi bitta rasm. */
    public record SaveNewsBlockImageRequest(

            @NotBlank(message = "{validation.required}")
            @Size(max = 120, message = "{validation.size.max}")
            String storedName,

            @Size(max = 255, message = "{validation.size.max}")
            String originalName,

            @Size(max = 300, message = "{validation.size.max}")
            String caption
    ) {
    }
}
