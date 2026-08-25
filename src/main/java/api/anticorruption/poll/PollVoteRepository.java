package api.anticorruption.poll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    boolean existsByPollIdAndVoterKey(Long pollId, String voterKey);

    /**
     * So'rovnoma o'chirilishidan oldin uning ovozlarini tozalaydi.
     *
     * <p>Ovozlar so'rovnomaga kaskad bilan bog'lanmagan: kaskad ular hammasini
     * xotiraga yuklashni talab qilardi, holbuki ommaviy so'rovnomada ovozlar
     * soni katta bo'lishi mumkin.
     */
    @Modifying
    @Query("delete from PollVote v where v.poll.id = :pollId")
    int deleteByPollId(@Param("pollId") Long pollId);

    /** Ro'yxatdan chiqarilgan variantlarning ovozlari. */
    @Modifying
    @Query("delete from PollVote v where v.option.id in :optionIds")
    int deleteByOptionIds(@Param("optionIds") Collection<Long> optionIds);

    /**
     * Ishtirokchilar soni - ovozlar soni emas.
     * Variant o'chirilgandan keyin hisoblagichlarni tiklash uchun kerak.
     */
    @Query("select count(distinct v.voterKey) from PollVote v where v.poll.id = :pollId")
    long countVotersByPollId(@Param("pollId") Long pollId);

    @Query("select count(distinct v.voterKey) from PollVote v where v.question.id = :questionId")
    long countVotersByQuestionId(@Param("questionId") Long questionId);

    /** Statistika uchun: birinchi va oxirgi ovoz vaqti. */
    @Query("select min(v.createdAt) from PollVote v where v.poll.id = :pollId")
    Optional<Instant> findFirstVoteAt(@Param("pollId") Long pollId);

    @Query("select max(v.createdAt) from PollVote v where v.poll.id = :pollId")
    Optional<Instant> findLastVoteAt(@Param("pollId") Long pollId);
}
