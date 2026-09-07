package api.anticorruption.poll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Ovoz berish.
 *
 * <p>Butun so'rovnoma bitta so'rovda yuboriladi: ishtirokchi savollarni
 * to'ldiradi va bir marta yuboradi. Majburiy bo'lmagan savol ro'yxatda
 * umuman bo'lmasligi mumkin - javobsiz qoldirilgani shundan bilinadi.
 *
 * @param answers har bir savol uchun tanlangan variantlar
 */
public record PollVoteRequest(

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
