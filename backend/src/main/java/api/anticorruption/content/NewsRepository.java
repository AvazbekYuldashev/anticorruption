package api.anticorruption.content;

import api.anticorruption.common.i18n.AppLanguage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, Long> {

    Optional<News> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<News> findBySlugAndPublishedTrue(String slug);

    /**
     * Tanlangan tildagi yangiliklar.
     *
     * <p>Tarjimasi yo'q maqola ro'yxatdan tushib qolmasligi kerak: shuning
     * uchun so'ralgan tilda nusxasi bo'lmagan guruhlar uchun asosiy
     * tildagi nusxa ko'rsatiladi. Aks holda rus tiliga o'tgan odam yarim
     * bo'sh sayt ko'rardi.
     */
    @Query("""
            select n from News n
            where n.published = true
              and (n.language = :language
                   or (n.language = :fallback
                       and not exists (select 1 from News t
                                       where t.translationGroup = n.translationGroup
                                         and t.language = :language
                                         and t.published = true)))
            order by n.publishedAt desc
            """)
    Page<News> findPublishedInLanguage(@Param("language") AppLanguage language,
                                       @Param("fallback") AppLanguage fallback,
                                       Pageable pageable);

    @Query("""
            select n from News n
            where n.published = true
              and (lower(n.title) like :pattern or lower(n.summary) like :pattern or lower(n.body) like :pattern)
              and (n.language = :language
                   or (n.language = :fallback
                       and not exists (select 1 from News t
                                       where t.translationGroup = n.translationGroup
                                         and t.language = :language
                                         and t.published = true)))
            order by n.publishedAt desc
            """)
    Page<News> searchPublishedInLanguage(@Param("pattern") String pattern,
                                         @Param("language") AppLanguage language,
                                         @Param("fallback") AppLanguage fallback,
                                         Pageable pageable);

    /** Bir maqolaning barcha tildagi nusxalari. */
    List<News> findByTranslationGroupOrderByLanguageAsc(String translationGroup);

    List<News> findByTranslationGroupAndPublishedTrueOrderByLanguageAsc(String translationGroup);

    boolean existsByTranslationGroupAndLanguage(String translationGroup, AppLanguage language);

    boolean existsByTranslationGroupAndLanguageAndIdNot(String translationGroup, AppLanguage language, Long id);

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
