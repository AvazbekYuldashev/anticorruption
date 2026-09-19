package api.anticorruption.poll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Tasodifiy testda ishtirokchilarga tushgan savollar to'plamlari. */
public interface PollAttemptRepository extends JpaRepository<PollAttempt, Long> {

    Optional<PollAttempt> findByPollIdAndVoterKey(Long pollId, String voterKey);

    Optional<PollAttempt> findByPollIdAndToken(Long pollId, String token);

    /**
     * Hisobot uchun: javob yuborgan har bir ishtirokchiga nechta savol berilgan.
     *
     * <p>Faqat ikki ustun olinadi - to'plamlarning savollar ro'yxatini
     * yuklash hisobot uchun ortiqcha.
     */
    @Query("select new api.anticorruption.poll.AttemptSizeRow(a.voterKey, a.questionCount) "
            + "from PollAttempt a where a.poll.id = :pollId and a.submittedAt is not null")
    List<AttemptSizeRow> findSubmittedSizes(@Param("pollId") Long pollId);

    /** So'rovnoma o'chirilishidan oldin - to'plamlar unga tashqi kalit bilan bog'langan. */
    @Modifying
    @Query("delete from PollAttempt a where a.poll.id = :pollId")
    int deleteByPollId(@Param("pollId") Long pollId);
}
