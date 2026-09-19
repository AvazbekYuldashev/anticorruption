package api.anticorruption.content;

import api.anticorruption.content.dto.AboutSectionResponse;
import api.anticorruption.content.dto.HomeBannerImageResponse;
import api.anticorruption.content.dto.ReorderRequest;
import api.anticorruption.content.dto.SaveAboutSectionRequest;
import api.anticorruption.content.dto.SaveSiteTextsRequest;
import api.anticorruption.content.dto.SaveStaffMemberRequest;
import api.anticorruption.content.dto.StaffMemberResponse;
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
import java.util.Map;

/** Bo'lim xodimlari va "Bo'lim haqida" sahifasini boshqarish. */
@Tag(name = "Admin - sayt bo'limlari", description = "Xodimlar va \"Bo'lim haqida\" sahifasi")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSiteContentController {

    private final SiteContentService siteContentService;
    private final SiteTextService siteTextService;
    private final HomeBannerService homeBannerService;

    @Operation(summary = "Barcha xodimlar", description = "Nofaollari ham")
    @GetMapping("/staff")
    public ResponseEntity<List<StaffMemberResponse>> listStaff() {
        return ResponseEntity.ok(siteContentService.listStaffForAdmin());
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

    // ================================================================ bo'lim haqida

    @Operation(summary = "Bo'lim haqida sahifasi", description = "Tahrirlash uchun joriy mazmun")
    @GetMapping("/about")
    public ResponseEntity<AboutSectionResponse> about() {
        return ResponseEntity.ok(siteContentService.aboutForAdmin());
    }

    @Operation(
            summary = "Bo'lim haqida sahifasini saqlash",
            description = "Sahifa bitta nusxada: birinchi saqlashda yaratiladi, keyin yangilanadi")
    @PutMapping("/about")
    public ResponseEntity<AboutSectionResponse> saveAbout(
            @Valid @RequestBody SaveAboutSectionRequest request) {

        return ResponseEntity.ok(siteContentService.saveAbout(request));
    }

    // ================================================================ bosh sahifa matnlari

    @Operation(
            summary = "Bosh sahifa matnlari",
            description = "O'zgartirilgan matnlar: til kodi -> (kalit -> matn)")
    @GetMapping("/site-texts")
    public ResponseEntity<Map<String, Map<String, String>>> siteTexts() {
        return ResponseEntity.ok(siteTextService.all());
    }

    @Operation(
            summary = "Bosh sahifa matnlarini saqlash",
            description = "To'liq holat yuboriladi: ro'yxatda yo'q yoki bo'sh matn asl holiga qaytadi. "
                    + "Faqat bosh sahifa kalitlari va uz, uz-cyrl, ru, en tillari qabul qilinadi")
    @PutMapping("/site-texts")
    public ResponseEntity<Map<String, Map<String, String>>> saveSiteTexts(
            @Valid @RequestBody SaveSiteTextsRequest request) {

        return ResponseEntity.ok(siteTextService.save(request));
    }

    // ================================================================ bosh banner foni

    @Operation(summary = "Bosh banner fonining rasmlari", description = "Albom tartibida")
    @GetMapping("/home-banner")
    public ResponseEntity<List<HomeBannerImageResponse>> homeBanner() {
        return ResponseEntity.ok(homeBannerService.listForAdmin());
    }

    @Operation(
            summary = "Bosh banner foniga rasm yuklash",
            description = "Bitta yoki bir nechta rasm (jpeg, png, webp) albom oxiriga qo'shiladi. "
                    + "Albomda ko'pi bilan 10 ta rasm. Yuklanishi bilan saqlanadi")
    @PostMapping(path = "/home-banner", consumes = "multipart/form-data")
    public ResponseEntity<List<HomeBannerImageResponse>> uploadHomeBanner(
            @RequestParam("files") List<MultipartFile> files) {

        return ResponseEntity.status(HttpStatus.CREATED).body(homeBannerService.add(files));
    }

    @Operation(
            summary = "Bosh banner rasmlari tartibi",
            description = "Albomdagi barcha rasmlarning id lari yangi tartibda")
    @PutMapping("/home-banner/order")
    public ResponseEntity<List<HomeBannerImageResponse>> reorderHomeBanner(
            @Valid @RequestBody ReorderRequest request) {

        return ResponseEntity.ok(homeBannerService.reorder(request.ids()));
    }

    @Operation(summary = "Bosh banner rasmini o'chirish", description = "Fayl diskdan ham o'chadi")
    @DeleteMapping("/home-banner/{id}")
    public ResponseEntity<List<HomeBannerImageResponse>> deleteHomeBanner(@PathVariable Long id) {
        return ResponseEntity.ok(homeBannerService.delete(id));
    }
}
