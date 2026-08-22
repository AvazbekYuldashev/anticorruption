package api.anticorruption.content;

import api.anticorruption.content.dto.SaveStaffMemberRequest;
import api.anticorruption.content.dto.SaveStaticPageRequest;
import api.anticorruption.content.dto.SaveUsefulLinkRequest;
import api.anticorruption.content.dto.StaffMemberResponse;
import api.anticorruption.content.dto.StaticPageResponse;
import api.anticorruption.content.dto.UsefulLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Xodimlar, matnli sahifalar va foydali havolalarni boshqarish. */
@Tag(name = "Admin - sayt bo'limlari", description = "Xodimlar, sahifalar va havolalarni boshqarish")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSiteContentController {

    private final SiteContentService siteContentService;

    // ================================================================ xodimlar

    @Operation(summary = "Barcha xodimlar", description = "Nofaollari ham")
    @GetMapping("/staff")
    public ResponseEntity<List<StaffMemberResponse>> listStaff() {
        return ResponseEntity.ok(siteContentService.listStaff(true));
    }

    @Operation(summary = "Xodim qo'shish")
    @PostMapping("/staff")
    public ResponseEntity<StaffMemberResponse> createStaff(@Valid @RequestBody SaveStaffMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteContentService.createStaff(request));
    }

    @Operation(summary = "Xodim ma'lumotini tahrirlash")
    @PutMapping("/staff/{id}")
    public ResponseEntity<StaffMemberResponse> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody SaveStaffMemberRequest request) {

        return ResponseEntity.ok(siteContentService.updateStaff(id, request));
    }

    @Operation(summary = "Xodim suratini yuklash")
    @PostMapping(path = "/staff/{id}/photo", consumes = "multipart/form-data")
    public ResponseEntity<StaffMemberResponse> uploadStaffPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(siteContentService.replaceStaffPhoto(id, file));
    }

    @Operation(summary = "Xodimni o'chirish")
    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        siteContentService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    // ================================================================ sahifalar

    @Operation(summary = "Barcha sahifalar", description = "Chop etilmaganlari ham, matni bilan")
    @GetMapping("/pages")
    public ResponseEntity<List<StaticPageResponse>> listPages() {
        return ResponseEntity.ok(siteContentService.listPages(true));
    }

    @Operation(summary = "Bitta sahifa")
    @GetMapping("/pages/{id}")
    public ResponseEntity<StaticPageResponse> pageDetail(@PathVariable Long id) {
        return ResponseEntity.ok(siteContentService.findPage(id));
    }

    @Operation(summary = "Sahifa yaratish", description = "slug bo'sh qoldirilsa sarlavhadan yasaladi")
    @PostMapping("/pages")
    public ResponseEntity<StaticPageResponse> createPage(@Valid @RequestBody SaveStaticPageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteContentService.createPage(request));
    }

    @Operation(summary = "Sahifani tahrirlash")
    @PutMapping("/pages/{id}")
    public ResponseEntity<StaticPageResponse> updatePage(
            @PathVariable Long id,
            @Valid @RequestBody SaveStaticPageRequest request) {

        return ResponseEntity.ok(siteContentService.updatePage(id, request));
    }

    @Operation(summary = "Sahifani o'chirish")
    @DeleteMapping("/pages/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        siteContentService.deletePage(id);
        return ResponseEntity.noContent().build();
    }

    // ================================================================ havolalar

    @Operation(summary = "Barcha havolalar", description = "Nofaollari ham")
    @GetMapping("/links")
    public ResponseEntity<List<UsefulLinkResponse>> listLinks() {
        return ResponseEntity.ok(siteContentService.listLinks(true));
    }

    @Operation(summary = "Havola qo'shish")
    @PostMapping("/links")
    public ResponseEntity<UsefulLinkResponse> createLink(@Valid @RequestBody SaveUsefulLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteContentService.createLink(request));
    }

    @Operation(summary = "Havolani tahrirlash")
    @PutMapping("/links/{id}")
    public ResponseEntity<UsefulLinkResponse> updateLink(
            @PathVariable Long id,
            @Valid @RequestBody SaveUsefulLinkRequest request) {

        return ResponseEntity.ok(siteContentService.updateLink(id, request));
    }

    @Operation(summary = "Havolani o'chirish")
    @DeleteMapping("/links/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id) {
        siteContentService.deleteLink(id);
        return ResponseEntity.noContent().build();
    }
}
