package api.anticorruption.attachment;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Yuklangan fayllarni diskda saqlaydi.
 *
 * <p>Xavfsizlik qoidalari:
 * <ul>
 *   <li>diskdagi nom har doim tasodifiy UUID - foydalanuvchi kiritgan nom ishlatilmaydi,
 *       shuning uchun "../../etc/passwd" kabi yo'l bilan chiqib ketish mumkin emas;</li>
 *   <li>MIME turi oq ro'yxat bo'yicha tekshiriladi;</li>
 *   <li>fayl mazmuni ham e'lon qilingan turga mos kelishi tekshiriladi
 *       ({@link FileSignatures}) - mijoz yuborgan sarlavhaga yolg'iz ishonilmaydi;</li>
 *   <li>ochiq va yopiq fayllar alohida papkalarda yotadi ({@link StorageArea});</li>
 *   <li>o'qishda ham yo'l o'z zonasi ichida ekanligi qayta tekshiriladi.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    /** MIME turi -> fayl kengaytmasi. Kengaytmani ham foydalanuvchidan emas, o'zimiz belgilaymiz. */
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf",
            "text/plain", ".txt");

    /** Ochiq zonaga faqat rasm tushadi: u hech qanday tekshiruvsiz beriladi. */
    private static final Set<String> PUBLIC_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    /** Fayl nomida saqlanmaydigan belgilar (yo'l ajratkichlari va Windows taqiqlagan belgilar). */
    private static final String UNSAFE_CHARS = "<>:\"/\\|?*";

    /** mark/reset bosh qismni qayta o'qish uchun yetarli bo'lishi kerak. */
    private static final int BUFFER_SIZE = 8192;

    private final StorageProperties properties;

    private final Map<StorageArea, Path> roots = new EnumMap<>(StorageArea.class);

    @PostConstruct
    void init() {
        Path base = Paths.get(properties.location()).toAbsolutePath().normalize();
        for (StorageArea area : StorageArea.values()) {
            Path areaRoot = base.resolve(area.getFolder()).normalize();
            try {
                Files.createDirectories(areaRoot);
            } catch (IOException ex) {
                throw new IllegalStateException("Fayllar uchun papka yaratib bo'lmadi: " + areaRoot, ex);
            }
            roots.put(area, areaRoot);
        }
        log.info("Fayllar saqlanadigan papka: {} (private / public)", base);
    }

    /**
     * Faylni tegishli zonaga yozadi va diskdagi noyob nomini qaytaradi.
     *
     * @throws BadRequestException fayl bo'sh yoki turi ruxsat etilmagan bo'lsa
     */
    public String store(MultipartFile file, StorageArea area) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(MessageKeys.FILE_EMPTY);
        }

        String contentType = normalizeContentType(file.getContentType());
        requireAllowedType(contentType, area);

        String storedName = UUID.randomUUID().toString().replace("-", "")
                + EXTENSIONS.getOrDefault(contentType, ".bin");
        Path target = resolveInside(area, storedName);

        // Oqim bir marta o'qiladi, shuning uchun bosh qismni belgilab olib,
        // tekshirgandan keyin boshiga qaytamiz - butun faylni xotiraga
        // yuklamaslik uchun.
        try (InputStream in = new BufferedInputStream(file.getInputStream(), BUFFER_SIZE)) {
            in.mark(FileSignatures.HEADER_SIZE);
            byte[] header = FileSignatures.readHeader(in);
            in.reset();

            if (!FileSignatures.matches(contentType, header)) {
                throw new BadRequestException(MessageKeys.FILE_CONTENT_MISMATCH, contentType);
            }

            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Faylni saqlab bo'lmadi: " + storedName, ex);
        }

        return storedName;
    }

    /** Diskdagi faylni o'qish uchun qaytaradi. */
    public Resource load(String storedName, StorageArea area) {
        Path target = resolveInside(area, storedName);
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException(MessageKeys.FILE_NOT_FOUND, storedName);
            }
            return resource;
        } catch (IOException ex) {
            throw new ResourceNotFoundException(MessageKeys.FILE_UNREADABLE, storedName);
        }
    }

    /** Faylni o'chiradi. Fayl allaqachon yo'q bo'lsa xatolik bermaydi. */
    public void delete(String storedName, StorageArea area) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveInside(area, storedName));
        } catch (BadRequestException ex) {
            log.warn("O'chirish uchun noto'g'ri fayl nomi: {}", storedName);
        } catch (IOException ex) {
            log.warn("Faylni o'chirib bo'lmadi: {}", storedName, ex);
        }
    }

    /** Faylning MIME turini kengaytmasidan aniqlaydi - javob sarlavhasi uchun. */
    public String contentTypeOf(String storedName) {
        if (storedName == null) {
            return "application/octet-stream";
        }
        for (Map.Entry<String, String> entry : EXTENSIONS.entrySet()) {
            if (storedName.endsWith(entry.getValue())) {
                return entry.getKey();
            }
        }
        return "application/octet-stream";
    }

    /**
     * Asl fayl nomini bazada saqlash uchun tozalaydi.
     * Bu nom faqat ko'rsatish uchun ishlatiladi, diskka yozishda emas.
     */
    public String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "fayl";
        }
        StringBuilder cleaned = new StringBuilder(originalName.length());
        for (char ch : originalName.toCharArray()) {
            boolean unsafe = Character.isISOControl(ch) || UNSAFE_CHARS.indexOf(ch) >= 0;
            cleaned.append(unsafe ? '_' : ch);
        }
        String name = cleaned.toString().trim();
        if (name.isEmpty()) {
            return "fayl";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private void requireAllowedType(String contentType, StorageArea area) {
        if (area == StorageArea.PUBLIC) {
            if (!PUBLIC_CONTENT_TYPES.contains(contentType)) {
                throw new BadRequestException(MessageKeys.FILE_IMAGE_TYPE_NOT_ALLOWED,
                        String.join(", ", PUBLIC_CONTENT_TYPES));
            }
            return;
        }
        List<String> allowed = properties.allowedContentTypes();
        if (!allowed.contains(contentType)) {
            throw new BadRequestException(MessageKeys.FILE_TYPE_NOT_ALLOWED,
                    contentType, String.join(", ", allowed));
        }
    }

    /** Yo'l o'z zonasi ichida qolishini kafolatlaydi. */
    private Path resolveInside(StorageArea area, String storedName) {
        Path root = roots.get(area);
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException(MessageKeys.FILE_INVALID_NAME);
        }
        return target;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "application/octet-stream";
        }
        int separator = contentType.indexOf(';');
        String value = separator > 0 ? contentType.substring(0, separator) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
