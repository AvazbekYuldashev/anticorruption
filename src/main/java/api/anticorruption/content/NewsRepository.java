package api.anticorruption.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, Long> {

    Optional<News> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<News> findBySlugAndPublishedTrue(String slug);

    Page<News> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    @Query("select n from News n where n.published = true and ("
            + "lower(n.title) like :pattern or lower(n.summary) like :pattern or lower(n.body) like :pattern) "
            + "order by n.publishedAt desc")
    Page<News> searchPublished(@Param("pattern") String pattern, Pageable pageable);

    /**
     * Ko'rishlar sonini bittaga oshiradi.
     *
     * <p>Entityni yuklab, o'zgartirib, saqlash o'rniga to'g'ridan-to'g'ri UPDATE:
     * bir vaqtda ko'p odam o'qiganda hisob yo'qolmasligi uchun.
     */
    @Modifying
    @Query("update News n set n.viewCount = n.viewCount + 1 where n.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
