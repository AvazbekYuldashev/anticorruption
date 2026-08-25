package api.anticorruption.poll.dto;

import api.anticorruption.poll.PollOption;

/**
 * Javob varianti va uning natijasi.
 *
 * @param percentage shu savolga javob berganlarga nisbatan foiz, bir kasrli
 *                   aniqlikda. So'rovnoma ishtirokchilariga emas, aynan
 *                   savolga javob berganlarga nisbatan: majburiy bo'lmagan
 *                   savolni hamma ham javoblamaydi.
 */
public record PollOptionResponse(
        Long id,
        String text,
        long voteCount,
        double percentage
) {
    public static PollOptionResponse from(PollOption option, long answeredCount) {
        return new PollOptionResponse(
                option.getId(),
                option.getText(),
                option.getVoteCount(),
                percentage(option.getVoteCount(), answeredCount));
    }

    private static double percentage(long votes, long answeredCount) {
        if (answeredCount <= 0) {
            return 0.0;
        }
        return Math.round(votes * 1000.0 / answeredCount) / 10.0;
    }
}
