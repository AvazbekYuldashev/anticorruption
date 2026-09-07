package api.anticorruption.complaint.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.AccusedPosition;
import api.anticorruption.complaint.Complaint;
import api.anticorruption.complaint.ComplaintCategory;
import api.anticorruption.complaint.ComplaintStatus;
import api.anticorruption.complaint.ReporterType;
import api.anticorruption.complaint.StudyForm;
import api.anticorruption.user.dto.UserResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Murojaatning to'liq ko'rinishi - moderator/admin uchun.
 * Bu yerda murojaatchining aloqa ma'lumotlari va ichki izohlar ham bo'ladi.
 */
public record ComplaintResponse(
        Long id,
        String trackingCode,
        String title,
        String description,
        ComplaintCategory category,
        String categoryLabel,

        Long facultyId,
        String facultyName,
        Long departmentId,
        String departmentName,
        String subjectName,
        AccusedPosition accusedPosition,
        String accusedPositionLabel,

        LocalDate incidentDate,
        String incidentPlace,

        boolean anonymous,
        ReporterType reporterType,
        String reporterTypeLabel,
        Integer courseYear,
        String groupName,
        StudyForm studyForm,
        String studyFormLabel,
        String reporterName,
        String reporterEmail,
        String reporterPhone,
        UserResponse author,

        UserResponse assignee,
        ComplaintStatus status,
        String statusLabel,
        String officialResponse,
        boolean hiddenFromRegister,

        List<AttachmentResponse> attachments,
        List<StatusHistoryResponse> history,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {
    /** Faqat tranzaksiya ichida chaqirilishi kerak - lazy bog'lanishlarga murojaat qiladi. */
    public static ComplaintResponse from(Complaint complaint, Translator translator) {
        return new ComplaintResponse(
                complaint.getId(),
                complaint.getTrackingCode(),
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getCategory(),
                translator.of(complaint.getCategory()),

                complaint.getFaculty() == null ? null : complaint.getFaculty().getId(),
                complaint.getFaculty() == null ? null : complaint.getFaculty().getName(),
                complaint.getDepartment() == null ? null : complaint.getDepartment().getId(),
                complaint.getDepartment() == null ? null : complaint.getDepartment().getName(),
                complaint.getSubjectName(),
                complaint.getAccusedPosition(),
                translator.of(complaint.getAccusedPosition()),

                complaint.getIncidentDate(),
                complaint.getIncidentPlace(),

                complaint.isAnonymous(),
                complaint.getReporterType(),
                translator.of(complaint.getReporterType()),
                complaint.getCourseYear(),
                complaint.getGroupName(),
                complaint.getStudyForm(),
                translator.of(complaint.getStudyForm()),
                complaint.getReporterName(),
                complaint.getReporterEmail(),
                complaint.getReporterPhone(),
                complaint.getAuthor() == null ? null : UserResponse.from(complaint.getAuthor(), translator),

                complaint.getAssignee() == null ? null : UserResponse.from(complaint.getAssignee(), translator),
                complaint.getStatus(),
                translator.of(complaint.getStatus()),
                complaint.getOfficialResponse(),
                complaint.isHiddenFromRegister(),

                complaint.getAttachments().stream().map(AttachmentResponse::from).toList(),
                complaint.getHistory().stream()
                        .map(entry -> StatusHistoryResponse.forStaff(entry, translator))
                        .toList(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                complaint.getClosedAt());
    }
}
