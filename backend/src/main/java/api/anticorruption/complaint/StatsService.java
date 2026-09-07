package api.anticorruption.complaint;

import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.dto.FacultyRatingResponse;
import api.anticorruption.complaint.dto.StatItem;
import api.anticorruption.complaint.dto.StatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Murojaatlar bo'yicha umumlashtirilgan raqamlar.
 *
 * <p>Bu ma'lumot ochiq: portalning maqsadi shaffoflik, shuning uchun jamlanma
 * raqamlar (kim, qayerdan, qanday murojaat qilgani emas) hamma uchun ko'rinadi.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final int RECENT_DAYS = 30;
    private static final double MINUTES_IN_DAY = 60.0 * 24.0;

    private final ComplaintRepository complaintRepository;
    private final Translator translator;

    @Transactional(readOnly = true)
    public StatsResponse overview() {
        long total = complaintRepository.count();
        long last30Days = complaintRepository.countByCreatedAtAfter(
                Instant.now().minus(RECENT_DAYS, ChronoUnit.DAYS));

        Map<ComplaintStatus, Long> statusCounts = new HashMap<>();
        for (ComplaintRepository.StatusCount row : complaintRepository.countGroupedByStatus()) {
            statusCounts.put(row.getStatus(), row.getTotal());
        }

        long resolved = statusCounts.getOrDefault(ComplaintStatus.RESOLVED, 0L);
        long rejected = statusCounts.getOrDefault(ComplaintStatus.REJECTED, 0L);
        long open = total - resolved - rejected;

        return new StatsResponse(
                total,
                last30Days,
                open,
                resolved,
                rejected,
                averageResolutionDays(),
                buildStatusItems(statusCounts),
                buildCategoryItems(),
                buildFacultyItems(),
                buildReporterTypeItems());
    }

    /**
     * Fakultetlar kesimidagi ko'rsatkichlar - "Reytinglar" bo'limi uchun.
     * Murojaati bor fakultetlargina chiqadi, ko'pdan kamga tartibda.
     */
    @Transactional(readOnly = true)
    public List<FacultyRatingResponse> facultyRating() {
        List<FacultyRatingResponse> rating = new ArrayList<>();

        for (ComplaintRepository.FacultyRatingRow row
                : complaintRepository.facultyRating(ComplaintStatus.RESOLVED)) {

            long total = row.getTotal();
            long closed = row.getClosed();

            rating.add(new FacultyRatingResponse(
                    row.getFacultyId(),
                    row.getFacultyName(),
                    total,
                    row.getResolved(),
                    closed,
                    total - closed,
                    total == 0 ? 0.0 : Math.round(closed * 1000.0 / total) / 10.0,
                    toDays(row.getAverageMinutes())));
        }
        return rating;
    }

    /** Bir kasrli aniqlikda, hali yopilgan murojaat bo'lmasa null. */
    private Double averageResolutionDays() {
        return toDays(complaintRepository.averageResolutionMinutes());
    }

    private Double toDays(Double minutes) {
        return minutes == null ? null : Math.round(minutes / MINUTES_IN_DAY * 10.0) / 10.0;
    }

    /** Barcha holatlar ko'rsatiladi - hatto nol bo'lganlari ham, grafik to'liq chiqishi uchun. */
    private List<StatItem> buildStatusItems(Map<ComplaintStatus, Long> counts) {
        List<StatItem> items = new ArrayList<>();
        for (ComplaintStatus status : ComplaintStatus.values()) {
            items.add(new StatItem(status.name(), translator.of(status), counts.getOrDefault(status, 0L)));
        }
        return items;
    }

    private List<StatItem> buildCategoryItems() {
        Map<ComplaintCategory, Long> counts = new HashMap<>();
        for (ComplaintRepository.CategoryCount row : complaintRepository.countGroupedByCategory()) {
            counts.put(row.getCategory(), row.getTotal());
        }
        List<StatItem> items = new ArrayList<>();
        for (ComplaintCategory category : ComplaintCategory.values()) {
            items.add(new StatItem(category.name(), translator.of(category), counts.getOrDefault(category, 0L)));
        }
        return items;
    }

    private List<StatItem> buildReporterTypeItems() {
        Map<ReporterType, Long> counts = new HashMap<>();
        for (ComplaintRepository.ReporterTypeCount row : complaintRepository.countGroupedByReporterType()) {
            counts.put(row.getReporterType(), row.getTotal());
        }
        List<StatItem> items = new ArrayList<>();
        for (ReporterType type : ReporterType.values()) {
            items.add(new StatItem(type.name(), translator.of(type), counts.getOrDefault(type, 0L)));
        }
        return items;
    }

    /**
     * Fakultetlar bazadan olinadi, shuning uchun enum kabi oldindan to'ldirib
     * bo'lmaydi - faqat murojaati bor fakultetlar chiqadi. Fakulteti
     * ko'rsatilmagan murojaatlar alohida qator bo'lib turadi.
     */
    private List<StatItem> buildFacultyItems() {
        List<StatItem> items = new ArrayList<>();
        for (ComplaintRepository.FacultyCount row : complaintRepository.countGroupedByFaculty()) {
            boolean noFaculty = row.getFacultyId() == null;
            items.add(new StatItem(
                    noFaculty ? "NONE" : String.valueOf(row.getFacultyId()),
                    noFaculty ? translator.get(MessageKeys.STATS_NO_FACULTY) : row.getFacultyName(),
                    row.getTotal()));
        }
        return items;
    }
}
