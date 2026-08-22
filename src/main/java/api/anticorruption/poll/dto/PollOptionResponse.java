package api.anticorruption.poll.dto;

import api.anticorruption.poll.PollOption;

/**
 * So'rovnoma varianti va uning natijasi.
 *
 * @param percentage ovoz bergan odamlarga nisbatan foiz, bir kasrli aniqlikda
 */
public record PollOptionResponse(
        Long id,
        String text,
        long voteCount,
        double percentage
) {
    public static PollOptionResponse from(PollOption option, long voterCount) {
        return new PollOptionResponse(
                option.getId(),
                option.getText(),
                option.getVoteCount(),
                percentage(option.getVoteCount(), voterCount));
    }

    private static double percentage(long votes, long voterCount) {
        if (voterCount <= 0) {
            return 0.0;
        }
        return Math.round(votes * 1000.0 / voterCount) / 10.0;
    }
}
