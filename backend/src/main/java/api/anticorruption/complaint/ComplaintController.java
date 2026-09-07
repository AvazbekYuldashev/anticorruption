package api.anticorruption.complaint;

import api.anticorruption.attachment.AttachmentService;
import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.complaint.dto.AttachmentResponse;
import api.anticorruption.complaint.dto.ComplaintCreatedResponse;
import api.anticorruption.complaint.dto.ComplaintResponse;
import api.anticorruption.complaint.dto.ComplaintSummaryResponse;
import api.anticorruption.complaint.dto.ComplaintTrackingResponse;
import api.anticorruption.complaint.dto.CreateComplaintRequest;
import api.anticorruption.complaint.dto.RegisterEntryResponse;
import api.anticorruption.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Murojaatlar", description = "Murojaat yuborish, kuzatish va ochiq reyestr")
@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AttachmentService attachmentService;

    @Operation(
            summary = "Yangi murojaat yuborish",
            description = "Tizimga kirmasdan ham yuborish mumkin. Javobda qaytgan kuzatuv kodini saqlab qo'yish kerak.")
    @PostMapping
    public ResponseEntity<ComplaintCreatedResponse> create(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        // principal null bo'lishi mumkin - bu tizimga kirmasdan yuborilgan murojaat.
        ComplaintCreatedResponse response = complaintService.create(
                request, principal == null ? null : principal.user());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Kuzatuv kodi bo'yicha holatni tekshirish",
            description = "Ochiq yo'l. Murojaatchining shaxsiy ma'lumotlari va ichki izohlar qaytarilmaydi.")
    @GetMapping("/track/{trackingCode}")
    public ResponseEntity<ComplaintTrackingResponse> track(@PathVariable String trackingCode) {
        return ResponseEntity.ok(complaintService.trackByCode(trackingCode));
    }

    @Operation(
            summary = "Murojaatga dalil fayli biriktirish",
            description = "Kuzatuv kodini bilish murojaat egasi ekanligining isboti hisoblanadi.")
    @PostMapping(path = "/track/{trackingCode}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<AttachmentResponse> attach(
            @PathVariable String trackingCode,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.attachByTrackingCode(trackingCode, file));
    }

    @Operation(
            summary = "Ochiq reyestr",
            description = "Barcha murojaatlarning anonimlashtirilgan ro'yxati: kuzatuv kodi, "
                    + "kategoriya, fakultet, holat va sanalar. Matn maydonlari chiqmaydi.")
    @GetMapping("/register")
    public ResponseEntity<PageResponse<RegisterEntryResponse>> register(
            @Parameter(description = "Kuzatuv kodi bo'yicha qidiruv")
            @RequestParam(required = false) String code,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Long facultyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                complaintService.publicRegister(code, category, status, facultyId, pageable));
    }

    @Operation(summary = "O'z murojaatlarim ro'yxati", description = "Tizimga kirish talab qilinadi")
    @GetMapping("/my")
    public ResponseEntity<PageResponse<ComplaintSummaryResponse>> myComplaints(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(complaintService.findMine(principal.id(), pageable));
    }

    @Operation(summary = "O'z murojaatimning to'liq ko'rinishi")
    @GetMapping("/my/{id}")
    public ResponseEntity<ComplaintResponse> myComplaintDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return ResponseEntity.ok(complaintService.findMineById(id, principal.user()));
    }
}
