package api.anticorruption.content.dto;

import api.anticorruption.content.NewsBlock;

import java.util.List;

/**
 * Yangilik mazmunining bir bo'lagi.
 *
 * @param type   {@code HEADING}, {@code TEXT}, {@code IMAGE} yoki {@code GALLERY}
 * @param text   matn bloklari mazmuni; ichida oddiy belgilar bilan formatlash
 *               bo'lishi mumkin ({@code **qalin**}, {@code *kursiv*})
 * @param url    yakka rasm manzili (boshqa turlarda null)
 * @param images albom rasmlari (boshqa turlarda bo'sh)
 */
public record NewsBlockResponse(
        Long id,
        String type,
        String text,
        String url,
        String originalName,
        String caption,
        List<NewsBlockImageResponse> images,
        int displayOrder
) {
    public static NewsBlockResponse from(NewsBlock block) {
        return new NewsBlockResponse(
                block.getId(),
                block.getType().name(),
                block.getText(),
                MediaUrls.of(block.getStoredName()),
                block.getOriginalName(),
                block.getCaption(),
                block.getImages().stream().map(NewsBlockImageResponse::from).toList(),
                block.getDisplayOrder());
    }
}
