package api.anticorruption.complaint;

import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.complaint.dto.AssignRequest;
import api.anticorruption.complaint.dto.ComplaintResponse;
import api.anticorruption.complaint.dto.ComplaintSearchFilter;
import api.anticorruption.complaint.dto.ComplaintSummaryResponse;
import api.anticorruption.complaint.dto.RegisterVisibilityRequest;
import api.anticorruption.complaint.dto.UpdateStatusRequest;
import api.anticorruption.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Admin paneli: murojaatlarni ko'rib chiqish.
 * Bu yo'llarga faqat MODERATOR va ADMIN kira oladi (SecurityConfig da belgilangan).
 */
@Tag(name = "Admin - murojaatlar", description = "Murojaatlarni ko'rib chiqish va boshqarish")
@RestController
@RequestMapping("/api/v1/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @Operation(
            summary = "Murojaatlar ro'yxati",
            description = "Barcha filtrlar ixtiyoriy. Sahifalash: ?page=0&size=20&sort=createdAt,desc")
    @GetMapping
    public ResponseEntity<PageResponse<ComplaintSummaryResponse>> search(
            @Parameter(description = "Sarlavha, matn, fan nomi yoki kuzatuv kodi bo'yicha qidiruv")
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ReporterType reporterType,
            @RequestParam(required = false) AccusedPosition accusedPosition,
            @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "true bo'lsa faqat hech kimga biriktirilmagan murojaatlar")
            @RequestParam(required = false) Boolean unassigned,
            @Parameter(description = "Shu vaqtdan keyin kelganlar, ISO-8601: 2026-01-01T00:00:00Z")
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        ComplaintSearchFilter filter = new ComplaintSearchFilter(
                query, status, category, facultyId, departmentId,
                reporterType, accusedPosition, assigneeId, unassigned, from, to);

        return ResponseEntity.ok(complaintService.search(filter, pageable));
    }

    @Operation(summary = "Murojaatning to'liq ko'rinishi", description = "Aloqa ma'lumotlari va ichki izohlar bilan")
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.findById(id));
    }

    @Operation(
            summary = "Holatni o'zgartirish",
            description = "Faqat ruxsat etilgan o'tishlar qabul qilinadi. Yakunlangan murojaat qayta ochilmaydi.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return ResponseEntity.ok(complaintService.updateStatus(id, request, principal.user()));
    }

    @Operation(
            summary = "Mas'ul xodimni belgilash",
            description = "assigneeId null bo'lsa biriktiruv bekor qilinadi")
    @PatchMapping("/{id}/assign")
    public ResponseEntity<ComplaintResponse> assign(
            @PathVariable Long id,
            @RequestBody AssignRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return ResponseEntity.ok(complaintService.assign(id, request, principal.user()));
    }

    @Operation(
            summary = "Ochiq reyestrda ko'rinishini boshqarish",
            description = "Murojaat tafsilotlari murojaatchini bilvosita oshkor qilishi mumkin bo'lsa yashiriladi")
    @PatchMapping("/{id}/register-visibility")
    public ResponseEntity<ComplaintResponse> setRegisterVisibility(
            @PathVariable Long id,
            @Valid @RequestBody RegisterVisibilityRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return ResponseEntity.ok(
                complaintService.setRegisterVisibility(id, request.hidden(), principal.user()));
    }
}
