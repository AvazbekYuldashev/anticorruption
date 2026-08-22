package api.anticorruption.complaint.dto;

import api.anticorruption.complaint.AccusedPosition;
import api.anticorruption.complaint.ComplaintCategory;
import api.anticorruption.complaint.ComplaintStatus;
import api.anticorruption.complaint.ReporterType;

import java.time.Instant;

/**
 * Admin panelidagi qidiruv shartlari. Har bir maydon ixtiyoriy -
 * null bo'lsa, o'sha bo'yicha filtrlanmaydi.
 *
 * @param query      sarlavha/matn/fan/kuzatuv kodi bo'yicha erkin qidiruv
 * @param unassigned true bo'lsa faqat hech kimga biriktirilmaganlari
 */
public record ComplaintSearchFilter(
        String query,
        ComplaintStatus status,
        ComplaintCategory category,
        Long facultyId,
        Long departmentId,
        ReporterType reporterType,
        AccusedPosition accusedPosition,
        Long assigneeId,
        Boolean unassigned,
        Instant from,
        Instant to
) {
}
