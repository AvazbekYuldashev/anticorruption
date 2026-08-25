package api.anticorruption.poll;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * So'rovnomalar.
 *
 * <p>Graf faqat savollarni oldindan yuklaydi; variantlar {@code @BatchSize}
 * tufayli bir necha so'rovda to'p-to'p keladi. Ikkala ro'yxatni bitta grafda
 * so'rash Hibernate da {@code MultipleBagFetchException} beradi.
 */
public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = "questions")
    List<Poll> findByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "questions")
    List<Poll> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "questions")
    Optional<Poll> findWithQuestionsById(Long id);

    /**
     * O'chirilayotgan so'rovnomaga havola qiladigan keyingi o'tkazishlarni uzadi.
     *
     * <p>Havola faqat "bu qaysi o'tkazishning takrori" degan ma'lumot uchun -
     * uni uzish yangi o'tkazishning o'z hisobotiga ta'sir qilmaydi.
     */
    @Modifying
    @Query("update Poll p set p.previousPoll = null where p.previousPoll.id = :pollId")
    int detachSuccessors(@Param("pollId") Long pollId);
}
