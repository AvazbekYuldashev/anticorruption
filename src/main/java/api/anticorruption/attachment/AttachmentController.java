package api.anticorruption.attachment;

import api.anticorruption.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Tag(name = "Fayllar", description = "Biriktirilgan dalil fayllarini yuklab olish")
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(
            summary = "Faylni yuklab olish",
            description = "Xodimlar barcha fayllarni, oddiy foydalanuvchi faqat o'z murojaatiniki")
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        AttachmentService.DownloadableAttachment file = attachmentService.prepareDownload(
                id, principal == null ? null : principal.user());

        // Fayl nomi kirillcha yoki o'zbekcha bo'lishi mumkin - UTF-8 da kodlaymiz.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        MediaType mediaType = file.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.contentType());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(file.resource());
    }
}
