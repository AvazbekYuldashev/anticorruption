package api.anticorruption.poll.dto;

import api.anticorruption.poll.PollType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * So'rovnoma yaratish yoki tahrirlash.
 *
 * <p>Savollar va variantlar to'liq ro'yxat sifatida yuboriladi: ro'yxatda
 * yo'q yozuv o'chiriladi. Tahrirlashda mavjud savol yoki variantning
 * {@code id} sini yuborish kerak - aks holda u yangi deb qaraladi va
 * ovozlari yo'qoladi.
 *
 * <p>Yuqori chegaralar mahsulot cheklovi emas, so'rov hajmini oqilona
 * ushlab turish uchun. Savollar chegarasi test savollari bazasiga qarab
 * qo'yilgan: bitta testga 200-300 ta savol kiritiladi va ishtirokchiga
 * ulardan bir qismi tasodifiy beriladi.
 *
 * @param groupId  qaysi guruhga tegishli - majburiy. Test guruhi har bir ishtirokchiga
 *                 nechta savol tasodifiy berilishini ham belgilaydi
 * @param startsAt null bo'lsa so'rovnoma faollashtirilishi bilan boshlanadi
 * @param endsAt   null bo'lsa qo'lda yopilgunicha davom etadi
 */
public record SavePollRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 5, max = 300, message = "{validation.size}")
        String title,

        @Size(max = 1000, message = "{validation.size.max}")
        String description,

        /* So'rovnomami yoki test; ko'rsatilmasa so'rovnoma. */
        PollType type,

        Boolean active,

        /* Qaysi guruhga tushsin. Majburiy: guruhsiz so'rovnoma bo'lmaydi. */
        @NotNull(message = "{validation.required}")
        Long groupId,

        Instant startsAt,

        Instant endsAt,

        @NotEmpty(message = "{validation.poll.questions.required}")
        @Size(max = 500, message = "{validation.size.max}")
        @Valid
        List<SavePollQuestionRequest> questions
) {
    /** Bitta savol. */
    public record SavePollQuestionRequest(

            /* Mavjud savolni ovozlari bilan saqlab qolish uchun; yangi savolda null. */
            Long id,

            @NotBlank(message = "{validation.required}")
            @Size(min = 3, max = 300, message = "{validation.size}")
            String text,

            Boolean multipleChoice,

            Boolean required,

            @NotEmpty(message = "{validation.required}")
            @Size(min = 1, max = 50, message = "{validation.poll.options.size}")
            @Valid
            List<SavePollOptionRequest> options
    ) {
    }

    /** Bitta variant. */
    public record SavePollOptionRequest(

            /* Mavjud variantni saqlab qolish uchun; yangi variantda null. */
            Long id,

            @NotBlank(message = "{validation.required}")
            @Size(min = 1, max = 250, message = "{validation.size}")
            String text,

            /* Faqat testda ma'noga ega: shu variant to'g'ri javobmi. */
            Boolean correct
    ) {
    }
}
