package api.anticorruption.content;

import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.content.dto.NewsDetailResponse;
import api.anticorruption.content.dto.NewsSummaryResponse;
import api.anticorruption.content.dto.SaveNewsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Yangiliklarni boshqarish. MODERATOR va ADMIN uchun. */
@Tag(name = "Admin - yangiliklar", description = "Yangilik yaratish, tahrirlash va chop etish")
@RestController
@RequestMapping("/api/v1/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsService newsService;

    @Operation(summary = "Barcha yangiliklar", description = "Qoralamalari ham ko'rsatiladi")
    @GetMapping
    public ResponseEntity<PageResponse<NewsSummaryResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(newsService.listAll(pageable));
    }

    @Operation(summary = "Bitta yangilik")
    @GetMapping("/{id}")
    public ResponseEntity<NewsDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.findById(id));
    }

    @Operation(summary = "Yangilik yaratish", description = "published ko'rsatilmasa qoralama bo'lib qoladi")
    @PostMapping
    public ResponseEntity<NewsDetailResponse> create(@Valid @RequestBody SaveNewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(request));
    }

    @Operation(
            summary = "Yangilikni tahrirlash",
            description = "Chop etilgan yangilikning manzili (slug) o'zgarmaydi - tashqi havolalar buzilmasin")
    @PutMapping("/{id}")
    public ResponseEntity<NewsDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveNewsRequest request) {

        return ResponseEntity.ok(newsService.update(id, request));
    }

    @Operation(summary = "Chop etish yoki qoralamaga qaytarish")
    @PatchMapping("/{id}/publish")
    public ResponseEntity<NewsDetailResponse> setPublished(
            @PathVariable Long id,
            @RequestParam boolean published) {

        return ResponseEntity.ok(newsService.setPublished(id, published));
    }

    @Operation(summary = "Muqova rasmini yuklash", description = "Faqat rasm: jpeg, png yoki webp")
    @PostMapping(path = "/{id}/cover", consumes = "multipart/form-data")
    public ResponseEntity<NewsDetailResponse> uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(newsService.replaceCover(id, file));
    }

    @Operation(summary = "Yangilikni o'chirish")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
