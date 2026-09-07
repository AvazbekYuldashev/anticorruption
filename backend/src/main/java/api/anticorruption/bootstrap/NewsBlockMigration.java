package api.anticorruption.bootstrap;

import api.anticorruption.content.News;
import api.anticorruption.content.NewsBlock;
import api.anticorruption.content.NewsImage;
import api.anticorruption.content.NewsImageRepository;
import api.anticorruption.content.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Eski yangiliklarni blokli modelga ko'chiradi.
 *
 * <p>Ilgari yangilik bitta {@code body} matni va alohida rasm albomidan
 * iborat edi. Endi mazmun bloklar ketma-ketligi. Bu ish bir marta
 * bajariladi: bloklari bor yangilikka tegilmaydi, shuning uchun
 * takroriy ishga tushirish xavfsiz.
 *
 * <p>Ko'chirish barcha muhitlarda bajarilgach, bu sinf {@code NewsImage}
 * bilan birga o'chirilishi mumkin.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class NewsBlockMigration implements ApplicationRunner {

    private final NewsRepository newsRepository;
    private final NewsImageRepository newsImageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migrated = 0;

        for (News news : newsRepository.findAll()) {
            if (!news.getBlocks().isEmpty()) {
                continue;
            }

            List<NewsImage> images = newsImageRepository.findByNewsIdOrderByDisplayOrderAsc(news.getId());
            boolean hasBody = news.getBody() != null && !news.getBody().isBlank();

            if (!hasBody && images.isEmpty()) {
                continue;
            }

            int order = 0;

            if (hasBody) {
                news.getBlocks().add(NewsBlock.builder()
                        .news(news)
                        .type(NewsBlock.Type.TEXT)
                        .text(news.getBody().trim())
                        .displayOrder(order++)
                        .build());
            }

            for (NewsImage image : images) {
                news.getBlocks().add(NewsBlock.builder()
                        .news(news)
                        .type(NewsBlock.Type.IMAGE)
                        .storedName(image.getStoredName())
                        .originalName(image.getOriginalName())
                        .caption(image.getCaption())
                        .displayOrder(order++)
                        .build());
            }

            newsRepository.save(news);
            // Fayllar diskda qoladi - ularga endi bloklar havola qiladi.
            newsImageRepository.deleteAll(images);
            migrated++;
        }

        if (migrated > 0) {
            log.info("Blokli modelga ko'chirildi: {} ta yangilik", migrated);
        }
    }
}
