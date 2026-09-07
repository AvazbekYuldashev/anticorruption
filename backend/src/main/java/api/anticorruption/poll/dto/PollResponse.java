package api.anticorruption.poll.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.Poll;
import api.anticorruption.poll.PollStatus;
import api.anticorruption.poll.PollType;

import java.time.Instant;
import java.util.List;

/**
 * So'rovnoma yoki test, savollari va joriy natijalari.
 *
 * @param type           so'rovnomami yoki test
 * @param status         hisoblangan holat: qoralama, rejalashtirilgan, ochiq,
 *                       to'xtatilgan yoki muddati tugagan
 * @param openForVoting  hozir javob berish mumkinmi
 * @param alreadyVoted   shu tashrifchi allaqachon javob berganmi
 * @param runNumber      nechanchi marta o'tkazilayotgani
 * @param previousPollId oldingi o'tkazish; birinchisida null
 * @param quizResult     test yakunidagi natija - faqat javob yuborilgan
 *                       so'rovga javoban to'ladi, qolgan hollarda null
 */
public record PollResponse(
        Long id,
        String title,
        String description,
        PollType type,
        String typeLabel,
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
        QuizResultResponse quizResult,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Ochiq zona uchun: to'g'ri javoblar faqat ishtirokchi testni
     * ishlab bo'lgandan keyin ko'rinadi.
     *
     * <p>Faqat tranzaksiya ichida chaqirilishi kerak - savollar lazy yuklanadi.
     */
    public static PollResponse from(Poll poll, boolean alreadyVoted, Translator translator) {
        return from(poll, alreadyVoted, translator, alreadyVoted, null);
    }

    /** Admin paneli uchun: to'g'ri javoblar ham, umumiy raqamlar ham ko'rinadi. */
    public static PollResponse forAdmin(Poll poll, Translator translator) {
        return build(poll, false, translator, true, null, true);
    }

    public static PollResponse from(
            Poll poll,
            boolean alreadyVoted,
            Translator translator,
            boolean revealCorrect,
            QuizResultResponse quizResult) {

        return build(poll, alreadyVoted, translator, revealCorrect, quizResult, false);
    }

    /**
     * @param admin admin panelining so'rovimi. Test statistikasi (ishtirokchilar
     *              soni, variantlarning ovozlari) faqat shu holda to'ldiriladi:
     *              ochiq saytda ishtirokchi faqat o'z natijasini ko'radi.
     */
    private static PollResponse build(
            Poll poll,
            boolean alreadyVoted,
            Translator translator,
            boolean revealCorrect,
            QuizResultResponse quizResult,
            boolean admin) {

        PollStatus status = poll.status();
        PollType type = poll.typeOrSurvey();
        boolean reveal = poll.isQuiz() && revealCorrect;
        boolean showStats = admin || !poll.isQuiz();

        return new PollResponse(
                poll.getId(),
                poll.getTitle(),
                poll.getDescription(),
                type,
                translator.of(type),
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
                showStats ? poll.getVoterCount() : 0,
                poll.getQuestions().size(),
                poll.getQuestions().stream()
                        .map(question -> PollQuestionResponse.from(question, reveal, showStats))
                        .toList(),
                quizResult,
                poll.getCreatedAt(),
                poll.getUpdatedAt());
    }
}
