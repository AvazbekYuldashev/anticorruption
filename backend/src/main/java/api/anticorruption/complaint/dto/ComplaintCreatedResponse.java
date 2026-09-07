package api.anticorruption.complaint.dto;

import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.Complaint;
import api.anticorruption.complaint.ComplaintStatus;

import java.time.Instant;

/**
 * Murojaat qabul qilinganidan keyingi javob.
 * Eng muhimi - {@code trackingCode}: murojaatchi keyinchalik shu kod orqali
 * holatni tekshiradi, shuning uchun uni saqlab qo'yish kerakligi eslatiladi.
 */
public record ComplaintCreatedResponse(
        Long id,
        String trackingCode,
        ComplaintStatus status,
        String statusLabel,
        String message,
        Instant createdAt
) {
    public static ComplaintCreatedResponse from(Complaint complaint, Translator translator) {
        return new ComplaintCreatedResponse(
                complaint.getId(),
                complaint.getTrackingCode(),
                complaint.getStatus(),
                translator.of(complaint.getStatus()),
                translator.get(MessageKeys.COMPLAINT_ACCEPTED, complaint.getTrackingCode()),
                complaint.getCreatedAt());
    }
}
