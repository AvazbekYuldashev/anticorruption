package api.anticorruption.poll.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.Poll;
import api.anticorruption.poll.PollQuestion;
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
 * @param firstVoteAt         birinchi ovoz vaqti; hech kim ovoz bermagan bo'lsa null
 * @param previousPollId      oldingi o'tkazish hisoboti; birinchisida null
 * @param questionsPerAttempt har bir ishtirokchiga nechta savol tasodifiy beriladi;
 *                            barcha savollar berilsa null
 * @param completionRate      ishtirokchilarga berilgan savollarning qanchasi
 *                            javoblangani, foizda
 * @param quiz                test bo'yicha ball hisoboti; so'rovnomada null
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
        Integer questionsPerAttempt,
        double completionRate,
        Instant firstVoteAt,
        Instant lastVoteAt,
        List<PollQuestionResponse> questions,
        QuizStatisticsResponse quiz
) {
    /**
     * @param expectedAnswers ishtirokchilarga jami nechta savol berilgan: har biriga
     *                        barcha savollar yoki o'ziga tushgan to'plam
     */
    public static PollStatisticsResponse from(
            Poll poll,
            Instant firstVoteAt,
            Instant lastVoteAt,
            long expectedAnswers,
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
                poll.drawSize(),
                completionRate(poll, expectedAnswers),
                firstVoteAt,
                lastVoteAt,
                // Hisobot admin uchun: to'g'ri javoblar ham ko'rinadi.
                poll.getQuestions().stream()
                        .map(question -> PollQuestionResponse.from(question, poll.isQuiz(), true))
                        .toList(),
                quiz);
    }

    /**
     * Berilgan savollarning qanchasi javoblanganini ko'rsatadi.
     *
     * <p>Savollarga berilgan javoblar jami ishtirokchilarga berilgan savollar
     * soniga nisbatan olinadi. Majburiy bo'lmagan savollar tashlab ketilsa bu
     * ko'rsatkich pasayadi - anketaning qaysi joyida odamlar to'xtab qolayotgani
     * shundan bilinadi.
     *
     * <p>Maxraj savollar soni emas, ishtirokchilarga berilgan savollar: tasodifiy
     * testda har bir savol ishtirokchilarning faqat bir qismiga tushadi va
     * savollar soniga bo'linsa hamma javob bergan testda ham ko'rsatkich past
     * chiqardi.
     */
    private static double completionRate(Poll poll, long expectedAnswers) {
        if (expectedAnswers <= 0) {
            return 0.0;
        }
        long answered = poll.getQuestions().stream()
                .mapToLong(PollQuestion::getAnsweredCount)
                .sum();

        return Math.min(100.0, Math.round(answered * 1000.0 / expectedAnswers) / 10.0);
    }
}
