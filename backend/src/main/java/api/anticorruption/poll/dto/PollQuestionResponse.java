package api.anticorruption.poll.dto;

import api.anticorruption.poll.PollQuestion;

import java.util.List;

/**
 * So'rovnoma savoli va uning joriy natijalari.
 *
 * @param answeredCount shu savolga javob bergan ishtirokchilar soni. Testda
 *                      bu raqam faqat admin panelida to'ladi.
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
    public static PollQuestionResponse from(
            PollQuestion question, boolean revealCorrect, boolean revealStats) {

        return new PollQuestionResponse(
                question.getId(),
                question.getText(),
                question.isMultipleChoice(),
                question.isRequired(),
                revealStats ? question.getAnsweredCount() : 0,
                question.getDisplayOrder(),
                question.getOptions().stream()
                        .map(option -> PollOptionResponse.from(
                                option, question.getAnsweredCount(), revealCorrect, revealStats))
                        .toList());
    }
}
