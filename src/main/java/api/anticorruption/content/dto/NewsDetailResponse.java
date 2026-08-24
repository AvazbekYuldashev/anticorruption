package api.anticorruption.content.dto;

import api.anticorruption.content.News;

import java.time.Instant;
import java.util.List;

/** Yangilikning to'liq matni va albomi bilan ko'rinishi. */
public record NewsDetailResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String body,
        String coverImageUrl,
        boolean published,
        Instant publishedAt,
        long viewCount,
        List<NewsImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
    public static NewsDetailResponse from(News news) {
        return from(news, news.getViewCount());
    }

    /**
     * Ko'rishlar sonini alohida qabul qiladi.
     *
     * <p>O'qish paytida hisob atomik {@code UPDATE} bilan oshiriladi va entity
     * o'zgartirilmaydi - aks holda tranzaksiya oxiridagi dirty-check eski
     * qiymatni qaytarib yozib, bir vaqtda kelgan o'qishlar yo'qolardi.
     *
     * <p>Faqat tranzaksiya ichida chaqirilishi kerak - albom lazy yuklanadi.
     */
    public static NewsDetailResponse from(News news, long viewCount) {
        return new NewsDetailResponse(
                news.getId(),
                news.getSlug(),
                news.getTitle(),
                news.getSummary(),
                news.getBody(),
                MediaUrls.of(news.getCoverImage()),
                news.isPublished(),
                news.getPublishedAt(),
                viewCount,
                news.getImages().stream().map(NewsImageResponse::from).toList(),
                news.getCreatedAt(),
                news.getUpdatedAt());
    }
}
