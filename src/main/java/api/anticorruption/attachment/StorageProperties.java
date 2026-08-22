package api.anticorruption.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Fayl saqlash sozlamalari (application.properties dagi "app.storage.*").
 *
 * @param location            fayllar saqlanadigan papka
 * @param maxFilesPerComplaint bitta murojaatga nechta fayl biriktirish mumkin
 * @param allowedContentTypes ruxsat etilgan MIME turlari
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String location,
        int maxFilesPerComplaint,
        List<String> allowedContentTypes
) {
    public StorageProperties {
        if (location == null || location.isBlank()) {
            location = "uploads";
        }
        if (maxFilesPerComplaint <= 0) {
            maxFilesPerComplaint = 5;
        }
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = List.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp",
                    "application/pdf",
                    "text/plain");
        }
    }
}
