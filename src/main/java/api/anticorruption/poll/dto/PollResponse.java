package api.anticorruption.poll.dto;

import api.anticorruption.poll.Poll;

import java.time.Instant;
import java.util.List;

/**
 * So'rovnoma va uning joriy natijalari.
 *
 * @param openForVoting hozir ovoz berish mumkinmi (faol va vaqt oynasi ichida)
 * @param alreadyVoted  shu tashrifchi allaqachon ovoz berganmi
 */
public record PollResponse(
        Long id,
        String question,
        String description,
        boolean active,
        boolean multipleChoice,
        boolean openForVoting,
        boolean alreadyVoted,
        Instant startsAt,
        Instant endsAt,
        long voterCount,
        List<PollOptionResponse> options
) {
    public static PollResponse from(Poll poll, boolean alreadyVoted) {
        return new PollResponse(
                poll.getId(),
                poll.getQuestion(),
                poll.getDescription(),
                poll.isActive(),
                poll.isMultipleChoice(),
                poll.isOpenForVoting(),
                alreadyVoted,
                poll.getStartsAt(),
                poll.getEndsAt(),
                poll.getVoterCount(),
                poll.getOptions().stream()
                        .map(option -> PollOptionResponse.from(option, poll.getVoterCount()))
                        .toList());
    }
}
