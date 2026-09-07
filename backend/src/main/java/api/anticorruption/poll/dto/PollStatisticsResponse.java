package api.anticorruption.poll.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.Poll;
import api.anticorruption.poll.PollStatus;

import java.time.Instant;
import java.util.List;

/**
 * So'rovnoma statistikasi.
 *
 * <p>Natijalarning o'zi {@link PollResponse} da ham bor - bu yerda ular
 * so'rovnomaning borishini baholashga yordam beradigan qo'shimchalar bilan
 * keladi: qachon boshlangani, oxirgi ovoz qachon tushgani va savollarning
 * javoblanish darajasi.
 *
 * @param firstVoteAt    birinchi ovoz vaqti; hech kim ovoz bermagan bo'lsa null
 * @param previousPollId oldingi o'tkazish hisoboti; birinchisida null
 * @param completionRate savollarning o'rtacha javoblanish darajasi, foizda
 * @param quiz           test bo'yicha ball hisoboti; so'rovnomada null
 */
public record PollStatisticsResponse(
        Long pollId,
        String title,
        PollStatus status,
        String statusLabel,
        Instant startsAt,
        Instant endsAt,
        Instant stoppedAt,
        int runNumber,
        Long previousPollId,
        long voterCount,
        int questionCount,
        double completionRate,
        Instant firstVoteAt,
        Instant lastVoteAt,
        List<PollQuestionResponse> questions,
        QuizStatisticsResponse quiz
) {
    public static PollStatisticsResponse from(
            Poll poll,
            Instant firstVoteAt,
            Instant lastVoteAt,
            QuizStatisticsResponse quiz,
            Translator translator) {

        PollStatus status = poll.status();

        return new PollStatisticsResponse(
                poll.getId(),
                poll.getTitle(),
                status,
                translator.of(status),
                poll.getStartsAt(),
                poll.getEndsAt(),
                poll.getStoppedAt(),
                poll.runNumberOrFirst(),
                poll.getPreviousPoll() == null ? null : poll.getPreviousPoll().getId(),
                poll.getVoterCount(),
                poll.getQuestions().size(),
                completionRate(poll),
                firstVoteAt,
                lastVoteAt,
                // Hisobot admin uchun: to'g'ri javoblar ham ko'rinadi.
                poll.getQuestions().stream()
                        .map(question -> PollQuestionResponse.from(question, poll.isQuiz(), true))
                        .toList(),
                quiz);
    }

    /**
     * Savollarning qanchasi javoblanganini ko'rsatadi.
     *
     * <p>Har bir savolning javob berganlari ishtirokchilar soniga nisbatan
     * olinadi va o'rtachasi hisoblanadi. Majburiy bo'lmagan savollar tashlab
     * ketilsa bu ko'rsatkich pasayadi - anketaning qaysi joyida odamlar
     * to'xtab qolayotgani shundan bilinadi.
     */
    private static double completionRate(Poll poll) {
        if (poll.getVoterCount() <= 0 || poll.getQuestions().isEmpty()) {
            return 0.0;
        }
        double sum = poll.getQuestions().stream()
                .mapToDouble(question -> (double) question.getAnsweredCount() / poll.getVoterCount())
                .sum();

        return Math.round(sum * 1000.0 / poll.getQuestions().size()) / 10.0;
    }
}
