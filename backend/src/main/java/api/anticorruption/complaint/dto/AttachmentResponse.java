package api.anticorruption.complaint.dto;

import api.anticorruption.attachment.Attachment;

import java.time.Instant;

/** Biriktirilgan fayl haqidagi ma'lumot (faylning o'zi alohida so'rov bilan olinadi). */
public record AttachmentResponse(
        Long id,
        String originalName,
        String contentType,
        long sizeBytes,
        String downloadUrl,
        Instant createdAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                "/api/v1/attachments/" + attachment.getId(),
                attachment.getCreatedAt());
    }
}
