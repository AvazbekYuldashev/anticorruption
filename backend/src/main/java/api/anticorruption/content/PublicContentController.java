package api.anticorruption.content;

import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.content.dto.AboutSectionResponse;
import api.anticorruption.content.dto.NewsDetailResponse;
import api.anticorruption.content.dto.NewsSummaryResponse;
import api.anticorruption.content.dto.StaffMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Saytning ochiq bo'limlari: yangiliklar, bo'lim xodimlari va "Bo'lim haqida".
 * Ikkalasi ham autentifikatsiyasiz o'qiladi.
 */
@Tag(name = "Sayt bo'limlari", description = "Yangiliklar va bo'lim xodimlari")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicContentController {

    private final NewsService newsService;
    private final SiteContentService siteContentService;

    // ------------------------------------------------------------- yangiliklar

    @Operation(summary = "Yangiliklar ro'yxati", description = "Faqat chop etilganlari, yangisi birinchi")
    @GetMapping("/news")
    public ResponseEntity<PageResponse<NewsSummaryResponse>> news(
            @Parameter(description = "Sarlavha va matn bo'yicha qidiruv")
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(newsService.listPublished(query, pageable));
    }

    @Operation(summary = "Yangilikni o'qish", description = "Ko'rishlar sonini bittaga oshiradi")
    @GetMapping("/news/{slug}")
    public ResponseEntity<NewsDetailResponse> newsDetail(@PathVariable String slug) {
        return ResponseEntity.ok(newsService.readPublished(slug));
    }

    // ------------------------------------------------------------- xodimlar

    @Operation(summary = "Bo'lim xodimlari", description = "Qabul vaqti va aloqa ma'lumotlari bilan")
    @GetMapping("/staff")
    public ResponseEntity<List<StaffMemberResponse>> staff() {
        return ResponseEntity.ok(siteContentService.listStaff(false));
    }

    // ------------------------------------------------------------- bo'lim haqida

    @Operation(
            summary = "Bo'lim haqida",
            description = "Sahifa hali to'ldirilmagan bo'lsa bo'sh mazmun qaytadi")
    @GetMapping("/about")
    public ResponseEntity<AboutSectionResponse> about() {
        return ResponseEntity.ok(siteContentService.about());
    }
}
