package api.anticorruption.complaint.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.Complaint;
import api.anticorruption.complaint.ComplaintCategory;
import api.anticorruption.complaint.ComplaintStatus;

import java.time.Instant;
import java.util.List;

/**
 * Kuzatuv kodi bo'yicha ko'rinish - murojaatchining o'zi uchun.
 *
 * <p>Kodni bilgan odam murojaat egasi deb hisoblanadi, shuning uchun bu yerda
 * rasmiy javob ham bor. Lekin ichki ma'lumotlar - xodimlarning izohlari,
 * kim ko'rib chiqayotgani, murojaatchining aloqa ma'lumotlari - chiqarilmaydi.
 */
public record ComplaintTrackingResponse(
        String trackingCode,
        String title,
        ComplaintCategory category,
        String categoryLabel,
        String facultyName,
        String departmentName,
        String subjectName,
        ComplaintStatus status,
        String statusLabel,
        String officialResponse,
        int attachmentCount,
        List<StatusHistoryResponse> history,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {
    public static ComplaintTrackingResponse from(Complaint complaint, Translator translator) {
        return new ComplaintTrackingResponse(
                complaint.getTrackingCode(),
                complaint.getTitle(),
                complaint.getCategory(),
                translator.of(complaint.getCategory()),
                complaint.getFaculty() == null ? null : complaint.getFaculty().getName(),
                complaint.getDepartment() == null ? null : complaint.getDepartment().getName(),
                complaint.getSubjectName(),
                complaint.getStatus(),
                translator.of(complaint.getStatus()),
                complaint.getOfficialResponse(),
                complaint.getAttachments().size(),
                complaint.getHistory().stream()
                        .map(entry -> StatusHistoryResponse.forPublic(entry, translator))
                        .toList(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                complaint.getClosedAt());
    }
}
