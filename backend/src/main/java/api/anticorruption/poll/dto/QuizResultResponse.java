package api.anticorruption.poll.dto;

import java.util.List;

/**
 * Test yakunlangandan keyingi natija.
 *
 * <p>Faqat javob yuborgan ishtirokchiga qaytariladi: shu tufayli to'g'ri
 * javoblar testni ishlashdan oldin ochilib qolmaydi.
 *
 * @param questionCount savollar soni
 * @param correctCount  to'liq to'g'ri javob berilgan savollar soni
 * @param percentage    to'g'ri javoblar ulushi, bir kasrli aniqlikda
 */
public record QuizResultResponse(
        int questionCount,
        int correctCount,
        double percentage,
        List<QuestionResult> questions
) {
    /**
     * Bitta savol bo'yicha natija.
     *
     * <p>Savol to'liq to'g'ri hisoblanadi: barcha to'g'ri variantlar
     * belgilangan va ortiqchasi tanlanmagan bo'lsa.
     */
    public record QuestionResult(
            Long questionId,
            boolean correct,
            List<Long> chosenOptionIds,
            List<Long> correctOptionIds
    ) {
    }

    public static QuizResultResponse of(int questionCount, List<QuestionResult> questions) {
        int correct = (int) questions.stream().filter(QuestionResult::correct).count();
        double percentage = questionCount <= 0
                ? 0.0
                : Math.round(correct * 1000.0 / questionCount) / 10.0;

        return new QuizResultResponse(questionCount, correct, percentage, questions);
    }
}
