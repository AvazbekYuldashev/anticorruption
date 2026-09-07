package api.anticorruption.poll.dto;

import api.anticorruption.poll.PollOption;

/**
 * Javob varianti va uning natijasi.
 *
 * @param percentage shu savolga javob berganlarga nisbatan foiz, bir kasrli
 *                   aniqlikda. So'rovnoma ishtirokchilariga emas, aynan
 *                   savolga javob berganlarga nisbatan: majburiy bo'lmagan
 *                   savolni hamma ham javoblamaydi.
 * @param correct    testda shu variant to'g'rimi. So'rovnomada va javob
 *                   berilmasidan oldingi testda {@code null}: aks holda
 *                   javoblar shakl bilan birga ochilib qolardi.
 * @param voteCount  nechta ishtirokchi tanlagani. Test statistikasi faqat
 *                   admin panelida ko'rinadi, shuning uchun ochiq javobda
 *                   testning raqamlari nol bo'lib keladi.
 */
public record PollOptionResponse(
        Long id,
        String text,
        long voteCount,
        double percentage,
        Boolean correct
) {
    public static PollOptionResponse from(
            PollOption option, long answeredCount, boolean revealCorrect, boolean revealStats) {

        long votes = revealStats ? option.getVoteCount() : 0;

        return new PollOptionResponse(
                option.getId(),
                option.getText(),
                votes,
                revealStats ? percentage(votes, answeredCount) : 0.0,
                revealCorrect ? option.isCorrectAnswer() : null);
    }

    private static double percentage(long votes, long answeredCount) {
        if (answeredCount <= 0) {
            return 0.0;
        }
        return Math.round(votes * 1000.0 / answeredCount) / 10.0;
    }
}
