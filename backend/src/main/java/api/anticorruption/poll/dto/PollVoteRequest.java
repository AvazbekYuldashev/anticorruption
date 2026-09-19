package api.anticorruption.poll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Ovoz berish.
 *
 * <p>Butun so'rovnoma bitta so'rovda yuboriladi: ishtirokchi savollarni
 * to'ldiradi va bir marta yuboradi. Majburiy bo'lmagan savol ro'yxatda
 * umuman bo'lmasligi mumkin - javobsiz qoldirilgani shundan bilinadi.
 *
 * @param attemptToken savollari tasodifiy tanlanadigan testda - testni boshlaganda
 *                     berilgan belgi ({@link PollResponse#attemptToken()}). Javoblar
 *                     aynan shu to'plamning savollariga qarab tekshiriladi
 * @param answers      har bir savol uchun tanlangan variantlar
 */
public record PollVoteRequest(

        @Size(max = 64, message = "{validation.size.max}")
        String attemptToken,

        @NotEmpty(message = "{validation.poll.answers.required}")
        @Valid
        List<QuestionAnswer> answers
) {
    /**
     * Bitta savolga javob.
     *
     * @param optionIds bir tanlovli savolda bitta bo'lishi kerak
     */
    public record QuestionAnswer(

            @NotNull(message = "{validation.required}")
            Long questionId,

            @NotEmpty(message = "{validation.poll.optionIds.required}")
            List<Long> optionIds
    ) {
    }
}
