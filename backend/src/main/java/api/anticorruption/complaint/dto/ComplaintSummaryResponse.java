package api.anticorruption.complaint.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.Complaint;
import api.anticorruption.complaint.ComplaintCategory;
import api.anticorruption.complaint.ComplaintStatus;
import api.anticorruption.complaint.ReporterType;

import java.time.Instant;

/** Ro'yxatlarda ko'rsatiladigan qisqartirilgan ko'rinish. */
public record ComplaintSummaryResponse(
        Long id,
        String trackingCode,
        String title,
        ComplaintCategory category,
        String categoryLabel,
        String facultyName,
        String departmentName,
        String subjectName,
        ReporterType reporterType,
        String reporterTypeLabel,
        ComplaintStatus status,
        String statusLabel,
        boolean anonymous,
        String assigneeName,
        long attachmentCount,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * @param attachmentCount alohida guruhlangan so'rovdan olinadi, shuning uchun
     *                        bu yerda {@code complaint.getAttachments()} chaqirilmaydi (N+1 dan qochish)
     */
    public static ComplaintSummaryResponse from(Complaint complaint, long attachmentCount,
                                                Translator translator) {
        return new ComplaintSummaryResponse(
                complaint.getId(),
                complaint.getTrackingCode(),
                complaint.getTitle(),
                complaint.getCategory(),
                translator.of(complaint.getCategory()),
                complaint.getFaculty() == null ? null : complaint.getFaculty().getName(),
                complaint.getDepartment() == null ? null : complaint.getDepartment().getName(),
                complaint.getSubjectName(),
                complaint.getReporterType(),
                translator.of(complaint.getReporterType()),
                complaint.getStatus(),
                translator.of(complaint.getStatus()),
                complaint.isAnonymous(),
                complaint.getAssignee() == null ? null : complaint.getAssignee().getFullName(),
                attachmentCount,
                complaint.getCreatedAt(),
                complaint.getUpdatedAt());
    }
}
