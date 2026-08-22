package api.anticorruption.poll;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    boolean existsByPollIdAndVoterKey(Long pollId, String voterKey);
}
