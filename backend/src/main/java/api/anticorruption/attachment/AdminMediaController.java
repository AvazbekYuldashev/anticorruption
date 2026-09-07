package api.anticorruption.attachment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Muharrir uchun rasm yuklash.
 *
 * <p>Rasm hech qanday yozuvga bog'lanmasdan saqlanadi va uning nomi
 * qaytariladi. Blokli muharrir aynan shunga muhtoj: foydalanuvchi rasmni
 * qo'shishi bilan uni ko'rishi kerak, lekin yangilikning o'zi hali
 * saqlanmagan bo'ladi. Saqlashda blok shu nomga havola qiladi.
 *
 * <p>Saqlanmasdan tashlab ketilgan rasm diskda qolib ketadi - buni
 * keyinchalik davriy tozalash hal qiladi (README dagi ro'yxatda).
 */
@Tag(name = "Admin - media", description = "Muharrir uchun rasm yuklash")
@RestController
@RequestMapping("/api/v1/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {

    private final FileStorageService fileStorageService;

    /**
     * Yuklangan rasm haqidagi ma'lumot.
     *
     * @param storedName blokda ko'rsatiladigan nom
     * @param url        darhol ko'rsatish uchun manzil
     */
    public record UploadedMedia(String storedName, String originalName, String url) {
    }

    @Operation(summary = "Rasm yuklash", description = "Faqat jpeg, png yoki webp")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<UploadedMedia> upload(@RequestParam("file") MultipartFile file) {
        String storedName = fileStorageService.store(file, StorageArea.PUBLIC);

        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadedMedia(
                storedName,
                fileStorageService.sanitizeOriginalName(file.getOriginalFilename()),
                "/api/v1/media/" + storedName));
    }
}
