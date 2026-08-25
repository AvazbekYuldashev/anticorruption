package api.anticorruption.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NewsBlockRepository extends JpaRepository<NewsBlock, Long> {

    /**
     * Bir sahifadagi yangiliklar uchun yakka rasm bloklari sonini bitta
     * so'rovda hisoblaydi. Natija: [yangilik_id, soni] juftliklari.
     */
    @Query("select b.news.id, count(b) from NewsBlock b "
            + "where b.news.id in :ids and b.type = api.anticorruption.content.NewsBlock$Type.IMAGE "
            + "group by b.news.id")
    List<Object[]> countSingleImagesGroupedByNewsIds(@Param("ids") Collection<Long> ids);

    /**
     * Albom bloklaridagi rasmlar soni. Yakka rasmlar bilan qo'shilib,
     * ro'yxatdagi "N ta rasm" belgisini beradi.
     */
    @Query("select i.block.news.id, count(i) from NewsBlockImage i "
            + "where i.block.news.id in :ids "
            + "group by i.block.news.id")
    List<Object[]> countGalleryImagesGroupedByNewsIds(@Param("ids") Collection<Long> ids);
}
