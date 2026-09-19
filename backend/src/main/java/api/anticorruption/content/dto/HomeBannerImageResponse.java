package api.anticorruption.content.dto;

import api.anticorruption.content.HomeBannerImage;

/**
 * Bosh banner fonidagi rasm.
 *
 * @param originalName yuklangan asl nom - faqat admin panelida; saytda null
 */
public record HomeBannerImageResponse(
        Long id,
        String url,
        String originalName,
        int displayOrder
) {
    public static HomeBannerImageResponse forSite(HomeBannerImage image) {
        return new HomeBannerImageResponse(
                image.getId(), MediaUrls.of(image.getStoredName()), null, image.getDisplayOrder());
    }

    public static HomeBannerImageResponse forAdmin(HomeBannerImage image) {
        return new HomeBannerImageResponse(
                image.getId(), MediaUrls.of(image.getStoredName()), image.getOriginalName(),
                image.getDisplayOrder());
    }
}
