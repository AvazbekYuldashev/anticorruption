package api.anticorruption.content.dto;

import api.anticorruption.content.StaticPage;

import java.time.Instant;

/** Matnli sahifa. */
public record StaticPageResponse(
        Long id,
        String slug,
        String title,
        String body,
        int displayOrder,
        boolean published,
        Instant updatedAt
) {
    public static StaticPageResponse from(StaticPage page) {
        return new StaticPageResponse(
                page.getId(),
                page.getSlug(),
                page.getTitle(),
                page.getBody(),
                page.getDisplayOrder(),
                page.isPublished(),
                page.getUpdatedAt());
    }

    /** Menyu uchun: matnsiz, faqat sarlavha va manzil. */
    public static StaticPageResponse withoutBody(StaticPage page) {
        return new StaticPageResponse(
                page.getId(),
                page.getSlug(),
                page.getTitle(),
                null,
                page.getDisplayOrder(),
                page.isPublished(),
                page.getUpdatedAt());
    }
}
