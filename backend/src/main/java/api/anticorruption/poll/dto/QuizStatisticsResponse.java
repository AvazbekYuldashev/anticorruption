package api.anticorruption.poll.dto;

import java.util.List;

/**
 * Test bo'yicha hisobot - faqat admin panelida ko'rsatiladi.
 *
 * <p>Ishtirokchi o'z natijasini javob yuborganda ko'radi ({@link QuizResultResponse}),
 * umumiy ko'rsatkichlar esa ochiq saytga chiqmaydi.
 *
 * @param participants      testni ishlagan ishtirokchilar soni
 * @param averageCorrect    o'rtacha nechta savolga to'g'ri javob berilgan
 * @param averagePercentage o'rtacha natija, foizda
 */
public record QuizStatisticsResponse(
        long participants,
        double averageCorrect,
        double averagePercentage,
        List<QuestionStat> questions
) {
    /**
     * Bitta savol kesimi.
     *
     * @param answeredCount shu savolga javob berganlar soni
     * @param correctCount  to'liq to'g'ri javob berganlar soni
     * @param correctRate   javob berganlarga nisbatan to'g'ri javob ulushi, foizda
     */
    public record QuestionStat(
            Long questionId,
            String text,
            long answeredCount,
            long correctCount,
            double correctRate
    ) {
    }

    /**
     * @param expectedAnswers ishtirokchilarga jami nechta savol berilgan. O'rtacha
     *                        natija shunga nisbatan olinadi: savollari tasodifiy
     *                        tanlanadigan testda ishtirokchi 200 ta savoldan
     *                        50 tasini oladi va 50 tadan baholanadi
     */
    public static QuizStatisticsResponse of(
            long participants, long expectedAnswers, List<QuestionStat> questions) {

        long totalCorrect = questions.stream().mapToLong(QuestionStat::correctCount).sum();

        double averageCorrect = participants <= 0
                ? 0.0
                : Math.round(totalCorrect * 100.0 / participants) / 100.0;

        double averagePercentage = expectedAnswers <= 0
                ? 0.0
                : Math.round(totalCorrect * 1000.0 / expectedAnswers) / 10.0;

        return new QuizStatisticsResponse(participants, averageCorrect, averagePercentage, questions);
    }
}
