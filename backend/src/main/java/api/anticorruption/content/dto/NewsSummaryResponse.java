package api.anticorruption.content.dto;

import api.anticorruption.content.News;

import java.time.Instant;

/**
 * Yangiliklar ro'yxatidagi bitta yozuv - matnning o'zisiz.
 *
 * @param imageCount albomdagi rasmlar soni; ro'yxatda "galereya bor" belgisi
 *                   sifatida ishlatiladi
 */
public record NewsSummaryResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String coverImageUrl,
        boolean published,
        Instant publishedAt,
        long viewCount,
        long imageCount
) {
    /**
     * @param imageCount alohida guruhlangan so'rovdan olinadi, shuning uchun
     *                   bu yerda {@code news.getImages()} chaqirilmaydi (N+1 dan qochish)
     */
    public static NewsSummaryResponse from(News news, long imageCount) {
        return new NewsSummaryResponse(
                news.getId(),
                news.getSlug(),
                news.getTitle(),
                news.getSummary(),
                MediaUrls.of(news.getCoverImage()),
                news.isPublished(),
                news.getPublishedAt(),
                news.getViewCount(),
                imageCount);
    }
}
