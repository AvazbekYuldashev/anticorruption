package api.anticorruption.poll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * So'rovnoma yaratish yoki tahrirlash.
 *
 * <p>Variantlar to'liq ro'yxat sifatida yuboriladi: ro'yxatda yo'q variant
 * o'chiriladi. Tahrirlashda mavjud variantning {@code id} sini yuborish kerak -
 * aks holda u yangi variant deb qaraladi va ovozlari yo'qoladi.
 */
public record SavePollRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 5, max = 300, message = "{validation.size}")
        String question,

        @Size(max = 1000, message = "{validation.size.max}")
        String description,

        Boolean multipleChoice,

        Boolean active,

        Instant startsAt,

        Instant endsAt,

        @NotEmpty(message = "{validation.required}")
        @Size(min = 2, max = 20, message = "{validation.poll.options.size}")
        @Valid
        List<SavePollOptionRequest> options
) {
    /** Bitta variant. */
    public record SavePollOptionRequest(

            /* Mavjud variantni saqlab qolish uchun; yangi variantda null. */
            Long id,

            @NotBlank(message = "{validation.required}")
            @Size(min = 1, max = 250, message = "{validation.size}")
            String text
    ) {
    }
}
