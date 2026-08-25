package api.anticorruption.poll.dto;

import api.anticorruption.poll.PollQuestion;

import java.util.List;

/**
 * So'rovnoma savoli va uning joriy natijalari.
 *
 * @param answeredCount shu savolga javob bergan ishtirokchilar soni
 */
public record PollQuestionResponse(
        Long id,
        String text,
        boolean multipleChoice,
        boolean required,
        long answeredCount,
        int displayOrder,
        List<PollOptionResponse> options
) {
    public static PollQuestionResponse from(PollQuestion question) {
        return new PollQuestionResponse(
                question.getId(),
                question.getText(),
                question.isMultipleChoice(),
                question.isRequired(),
                question.getAnsweredCount(),
                question.getDisplayOrder(),
                question.getOptions().stream()
                        .map(option -> PollOptionResponse.from(option, question.getAnsweredCount()))
                        .toList());
    }
}
