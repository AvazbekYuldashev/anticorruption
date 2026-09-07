package api.anticorruption.complaint.dto;

import java.util.List;

/**
 * Umumlashtirilgan statistika.
 *
 * @param total                 jami murojaatlar soni
 * @param last30Days            oxirgi 30 kunda kelgan murojaatlar
 * @param open                  hali yakunlanmagan murojaatlar
 * @param resolved              ijobiy hal qilinganlari
 * @param rejected              rad etilganlari
 * @param averageResolutionDays o'rtacha ko'rib chiqish muddati (kunlarda);
 *                              hali birorta murojaat yopilmagan bo'lsa null
 */
public record StatsResponse(
        long total,
        long last30Days,
        long open,
        long resolved,
        long rejected,
        Double averageResolutionDays,
        List<StatItem> byStatus,
        List<StatItem> byCategory,
        List<StatItem> byFaculty,
        List<StatItem> byReporterType
) {
}
