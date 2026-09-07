package api.anticorruption.complaint.dto;

import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.ComplaintStatus;
import api.anticorruption.complaint.ComplaintStatusHistory;

import java.time.Instant;

/**
 * Holat o'zgarishi yozuvi.
 *
 * @param note      xodimning izohi - faqat xodimlarga ko'rsatiladi
 * @param changedBy o'zgartirgan xodim ismi - faqat xodimlarga ko'rsatiladi
 */
public record StatusHistoryResponse(
        Long id,
        ComplaintStatus oldStatus,
        String oldStatusLabel,
        ComplaintStatus newStatus,
        String newStatusLabel,
        String note,
        String changedBy,
        Instant changedAt
) {
    /** Xodimlar uchun to'liq yozuv. */
    public static StatusHistoryResponse forStaff(ComplaintStatusHistory entry, Translator translator) {
        return new StatusHistoryResponse(
                entry.getId(),
                entry.getOldStatus(),
                translator.of(entry.getOldStatus()),
                entry.getNewStatus(),
                translator.of(entry.getNewStatus()),
                note(entry, translator),
                entry.getChangedBy() == null ? null : entry.getChangedBy().getFullName(),
                entry.getChangedAt());
    }

    /** Murojaatchi uchun: ichki izoh va xodim ismi ko'rsatilmaydi. */
    public static StatusHistoryResponse forPublic(ComplaintStatusHistory entry, Translator translator) {
        return new StatusHistoryResponse(
                entry.getId(),
                entry.getOldStatus(),
                translator.of(entry.getOldStatus()),
                entry.getNewStatus(),
                translator.of(entry.getNewStatus()),
                null,
                null,
                entry.getChangedAt());
    }

    /**
     * Murojaat yaratilgan paytdagi birinchi yozuvda izoh bazada saqlanmaydi -
     * u tizim yozuvi bo'lgani uchun har safar joriy tilga o'giriladi.
     * Xodim yozgan izohlar esa qanday yozilgan bo'lsa shundayligicha qaytadi.
     */
    private static String note(ComplaintStatusHistory entry, Translator translator) {
        if (entry.getNote() != null) {
            return entry.getNote();
        }
        return entry.getOldStatus() == null ? translator.get(MessageKeys.EMAIL_HISTORY_INITIAL) : null;
    }
}
