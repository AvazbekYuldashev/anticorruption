package api.anticorruption.complaint.dto;

import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.Complaint;
import api.anticorruption.complaint.ComplaintCategory;
import api.anticorruption.complaint.ComplaintStatus;
import api.anticorruption.complaint.ReporterType;

import java.time.Instant;

/**
 * Ochiq reyestrdagi bitta yozuv - hamma ko'ra oladi.
 *
 * <p>Bu yerda ataylab birorta ham erkin matn maydoni yo'q: sarlavha ham,
 * murojaat matni ham, rasmiy javob ham chiqmaydi. Sabab oddiy - erkin matnda
 * murojaatchini yoki uchinchi shaxsni oshkor qiladigan tafsilot bo'lishi mumkin,
 * va uni har safar qo'lda tekshirib o'tirib bo'lmaydi. Faqat oldindan belgilangan
 * qiymatlar (kategoriya, holat, fakultet) va sanalar ko'rsatiladi, shuning uchun
 * reyestr tuzilishi bo'yicha xavfsiz.
 *
 * <p>Murojaatchining o'zi to'liq javobni kuzatuv kodi orqali ko'radi.
 */
public record RegisterEntryResponse(
        String trackingCode,
        ComplaintCategory category,
        String categoryLabel,
        String facultyName,
        ReporterType reporterType,
        String reporterTypeLabel,
        ComplaintStatus status,
        String statusLabel,
        Instant createdAt,
        Instant closedAt
) {
    public static RegisterEntryResponse from(Complaint complaint, Translator translator) {
        // Fakultet ko'rsatilmagan murojaat butun universitetga taalluqli.
        String facultyName = complaint.getFaculty() == null
                ? translator.get(MessageKeys.STATS_NO_FACULTY)
                : complaint.getFaculty().getName();

        return new RegisterEntryResponse(
                complaint.getTrackingCode(),
                complaint.getCategory(),
                translator.of(complaint.getCategory()),
                facultyName,
                complaint.getReporterType(),
                translator.of(complaint.getReporterType()),
                complaint.getStatus(),
                translator.of(complaint.getStatus()),
                complaint.getCreatedAt(),
                complaint.getClosedAt());
    }
}
