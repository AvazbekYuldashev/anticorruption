package api.anticorruption.content.dto;

import api.anticorruption.content.NewsImage;

/** Albomdagi bitta rasm. */
public record NewsImageResponse(
        Long id,
        String url,
        String originalName,
        String caption,
        int displayOrder
) {
    public static NewsImageResponse from(NewsImage image) {
        return new NewsImageResponse(
                image.getId(),
                MediaUrls.of(image.getStoredName()),
                image.getOriginalName(),
                image.getCaption(),
                image.getDisplayOrder());
    }
}
