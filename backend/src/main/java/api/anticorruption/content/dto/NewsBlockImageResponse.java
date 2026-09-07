package api.anticorruption.content.dto;

import api.anticorruption.content.NewsBlockImage;

/**
 * Albom blokidagi bitta rasm.
 *
 * @param url rasm manzili
 */
public record NewsBlockImageResponse(
        Long id,
        String url,
        String originalName,
        String caption,
        int displayOrder
) {
    public static NewsBlockImageResponse from(NewsBlockImage image) {
        return new NewsBlockImageResponse(
                image.getId(),
                MediaUrls.of(image.getStoredName()),
                image.getOriginalName(),
                image.getCaption(),
                image.getDisplayOrder());
    }
}
