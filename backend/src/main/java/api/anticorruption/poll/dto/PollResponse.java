package api.anticorruption.poll.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.Poll;
import api.anticorruption.poll.PollQuestion;
import api.anticorruption.poll.PollStatus;
import api.anticorruption.poll.PollType;

import java.time.Instant;
import java.util.List;

/**
 * So'rovnoma yoki test, savollari va joriy natijalari.
 *
 * @param type                so'rovnomami yoki test
 * @param status              hisoblangan holat: qoralama, rejalashtirilgan, ochiq,
 *                            to'xtatilgan yoki muddati tugagan
 * @param openForVoting       hozir javob berish mumkinmi
 * @param alreadyVoted        shu tashrifchi allaqachon javob berganmi
 * @param runNumber           nechanchi marta o'tkazilayotgani
 * @param previousPollId      oldingi o'tkazish; birinchisida null
 * @param groupId             qaysi guruhga tegishli; guruhsiz bo'lsa null
 * @param groupName           guruh nomi - ro'yxatni guruhlarga ajratish uchun,
 *                            alohida so'rov yubormasdan
 * @param questionCount       testdagi barcha savollar soni (savollar bazasi)
 * @param questionsPerAttempt har bir ishtirokchiga savollardan nechtasi tasodifiy
 *                            beriladi; barcha savollar berilsa null. Son guruhda
 *                            belgilanadi, testda undan kam savol bo'lsa - bori
 * @param questions           ko'rsatiladigan savollar. Savollari tasodifiy tanlanadigan
 *                            testda ochiq saytga butun baza hech qachon chiqmaydi:
 *                            ishtirokchi faqat o'ziga tushgan savollarni ko'radi,
 *                            ro'yxat va boshlanmagan test sahifasida esa bu bo'sh
 * @param quizResult          test yakunidagi natija - faqat javob yuborilgan
 *                            so'rovga javoban to'ladi, qolgan hollarda null
 * @param attemptToken        testni boshlaganda beriladigan belgi - javoblar bilan
 *                            qaytarib yuboriladi. Qolgan hollarda null
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
        Long groupId,
        String groupName,
        long voterCount,
        int questionCount,
        Integer questionsPerAttempt,
        List<PollQuestionResponse> questions,
        QuizResultResponse quizResult,
        String attemptToken,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Ochiq zona uchun: to'g'ri javoblar faqat ishtirokchi testni
     * ishlab bo'lgandan keyin ko'rinadi.
     *
     * <p>Faqat tranzaksiya ichida chaqirilishi kerak - savollar lazy yuklanadi.
     *
     * @param questions qaysi savollar ko'rsatilsin - buni xizmat hal qiladi
     */
    public static PollResponse from(
            Poll poll, List<PollQuestion> questions, boolean alreadyVoted, Translator translator) {

        return build(poll, questions, alreadyVoted, alreadyVoted, null, null, false, translator);
    }

    /** Test boshlanganda: ishtirokchiga tushgan savollar, to'g'ri javoblarsiz. */
    public static PollResponse forAttempt(
            Poll poll, List<PollQuestion> questions, String attemptToken, Translator translator) {

        return build(poll, questions, false, false, null, attemptToken, false, translator);
    }

    /** Javob yuborilgandan keyin: to'g'ri variantlar va test natijasi bilan. */
    public static PollResponse afterVote(
            Poll poll, List<PollQuestion> questions, QuizResultResponse quizResult, Translator translator) {

        return build(poll, questions, true, true, quizResult, null, false, translator);
    }

    /** Admin paneli uchun: barcha savollar, to'g'ri javoblar va umumiy raqamlar. */
    public static PollResponse forAdmin(Poll poll, Translator translator) {
        return build(poll, poll.getQuestions(), false, true, null, null, true, translator);
    }

    /**
     * @param admin admin panelining so'rovimi. Test statistikasi (ishtirokchilar
     *              soni, variantlarning ovozlari) faqat shu holda to'ldiriladi:
     *              ochiq saytda ishtirokchi faqat o'z natijasini ko'radi.
     */
    private static PollResponse build(
            Poll poll,
            List<PollQuestion> questions,
            boolean alreadyVoted,
            boolean revealCorrect,
            QuizResultResponse quizResult,
            String attemptToken,
            boolean admin,
            Translator translator) {

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
                poll.getGroup() == null ? null : poll.getGroup().getId(),
                poll.getGroup() == null ? null : poll.getGroup().getName(),
                showStats ? poll.getVoterCount() : 0,
                poll.getQuestions().size(),
                poll.drawSize(),
                questions.stream()
                        .map(question -> PollQuestionResponse.from(question, reveal, showStats))
                        .toList(),
                quizResult,
                attemptToken,
                poll.getCreatedAt(),
                poll.getUpdatedAt());
    }
}
