package api.anticorruption.poll.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.Poll;
import api.anticorruption.poll.PollStatus;

import java.time.Instant;
import java.util.List;

/**
 * So'rovnoma, savollari va joriy natijalari.
 *
 * @param status         hisoblangan holat: qoralama, rejalashtirilgan, ochiq,
 *                       to'xtatilgan yoki muddati tugagan
 * @param openForVoting  hozir ovoz berish mumkinmi
 * @param alreadyVoted   shu tashrifchi allaqachon javob berganmi
 * @param runNumber      nechanchi marta o'tkazilayotgani
 * @param previousPollId oldingi o'tkazish; birinchisida null
 */
public record PollResponse(
        Long id,
        String title,
        String description,
        boolean active,
        PollStatus status,
        String statusLabel,
        boolean openForVoting,
        boolean alreadyVoted,
        Instant startsAt,
        Instant endsAt,
        Instant stoppedAt,
        int runNumber,
        Long previousPollId,
        long voterCount,
        int questionCount,
        List<PollQuestionResponse> questions,
        Instant createdAt,
        Instant updatedAt
) {
    /** Faqat tranzaksiya ichida chaqirilishi kerak - savollar lazy yuklanadi. */
    public static PollResponse from(Poll poll, boolean alreadyVoted, Translator translator) {
        PollStatus status = poll.status();

        return new PollResponse(
                poll.getId(),
                poll.getTitle(),
                poll.getDescription(),
                poll.isActive(),
                status,
                translator.of(status),
                status == PollStatus.OPEN,
                alreadyVoted,
                poll.getStartsAt(),
                poll.getEndsAt(),
                poll.getStoppedAt(),
                poll.runNumberOrFirst(),
                poll.getPreviousPoll() == null ? null : poll.getPreviousPoll().getId(),
                poll.getVoterCount(),
                poll.getQuestions().size(),
                poll.getQuestions().stream().map(PollQuestionResponse::from).toList(),
                poll.getCreatedAt(),
                poll.getUpdatedAt());
    }
}
