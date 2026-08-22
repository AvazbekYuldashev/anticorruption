package api.anticorruption.content.dto;

import api.anticorruption.content.News;

import java.time.Instant;

/** Yangiliklar ro'yxatidagi bitta yozuv - matnning o'zisiz. */
public record NewsSummaryResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String coverImageUrl,
        boolean published,
        Instant publishedAt,
        long viewCount
) {
    public static NewsSummaryResponse from(News news) {
        return new NewsSummaryResponse(
                news.getId(),
                news.getSlug(),
                news.getTitle(),
                news.getSummary(),
                MediaUrls.of(news.getCoverImage()),
                news.isPublished(),
                news.getPublishedAt(),
                news.getViewCount());
    }
}
