package api.anticorruption.attachment;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.i18n.MessageKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Ochiq rasmlarni beradi: yangilik muqovalari va xodim suratlari.
 *
 * <p>Bu yerda huquq tekshirilmaydi, shuning uchun faqat {@link StorageArea#PUBLIC}
 * zonasidan o'qiladi. Dalil fayllari boshqa papkada yotadi va bu endpoint orqali
 * hech qanday holatda chiqmaydi.
 */
@Tag(name = "Media", description = "Yangilik va xodim rasmlari")
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Rasmni olish")
    @GetMapping("/{storedName}")
    public ResponseEntity<Resource> get(@PathVariable String storedName) {
        requireSimpleName(storedName);

        Resource resource = fileStorageService.load(storedName, StorageArea.PUBLIC);
        MediaType mediaType = MediaType.parseMediaType(fileStorageService.contentTypeOf(storedName));

        return ResponseEntity.ok()
                .contentType(mediaType)
                // Nom tasodifiy va o'zgarmas, shuning uchun uzoq keshlash xavfsiz.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(resource);
    }

    /** Nom faqat UUID va kengaytmadan iborat bo'lishi kerak - yo'l belgilari bo'lmasin. */
    private void requireSimpleName(String storedName) {
        if (storedName == null || !storedName.matches("[a-f0-9]{32}\\.[a-z0-9]{2,5}")) {
            throw new BadRequestException(MessageKeys.FILE_INVALID_NAME);
        }
    }
}
