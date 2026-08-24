package api.anticorruption.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NewsImageRepository extends JpaRepository<NewsImage, Long> {

    long countByNewsId(Long newsId);

    Optional<NewsImage> findByIdAndNewsId(Long id, Long newsId);

    /** Albomga yangi rasm qo'shishda keyingi tartib raqamini aniqlash uchun. */
    @Query("select coalesce(max(i.displayOrder), -1) from NewsImage i where i.news.id = :newsId")
    int maxDisplayOrder(@Param("newsId") Long newsId);

    /**
     * Bir sahifadagi barcha yangiliklar uchun rasmlar sonini bitta so'rovda
     * hisoblaydi. Natija: [yangilik_id, soni] juftliklari.
     */
    @Query("select i.news.id, count(i) from NewsImage i where i.news.id in :ids group by i.news.id")
    List<Object[]> countGroupedByNewsIds(@Param("ids") Collection<Long> ids);
}
