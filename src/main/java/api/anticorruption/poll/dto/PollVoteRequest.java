package api.anticorruption.poll.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Ovoz berish.
 *
 * @param optionIds tanlangan variant id lari; bir tanlovli so'rovnomada bitta bo'lishi kerak
 */
public record PollVoteRequest(

        @NotEmpty(message = "{validation.poll.optionIds.required}")
        List<Long> optionIds
) {
}
