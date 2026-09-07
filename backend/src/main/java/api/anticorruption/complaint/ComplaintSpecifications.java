package api.anticorruption.complaint;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ro'yxatlar uchun filtrlar.
 * Har bir metod filtr qiymati bo'sh bo'lsa {@code null} qaytaradi -
 * bunday shartlarni {@code ComplaintService} birlashtirishda o'tkazib yuboradi.
 */
public final class ComplaintSpecifications {

    private ComplaintSpecifications() {
    }

    public static Specification<Complaint> statusIs(ComplaintStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Complaint> categoryIs(ComplaintCategory category) {
        return category == null ? null : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Complaint> facultyIs(Long facultyId) {
        return facultyId == null
                ? null
                : (root, query, cb) -> cb.equal(root.join("faculty", JoinType.LEFT).get("id"), facultyId);
    }

    public static Specification<Complaint> departmentIs(Long departmentId) {
        return departmentId == null
                ? null
                : (root, query, cb) -> cb.equal(root.join("department", JoinType.LEFT).get("id"), departmentId);
    }

    public static Specification<Complaint> reporterTypeIs(ReporterType reporterType) {
        return reporterType == null ? null : (root, query, cb) -> cb.equal(root.get("reporterType"), reporterType);
    }

    public static Specification<Complaint> accusedPositionIs(AccusedPosition position) {
        return position == null ? null : (root, query, cb) -> cb.equal(root.get("accusedPosition"), position);
    }

    public static Specification<Complaint> assigneeIs(Long assigneeId) {
        return assigneeId == null
                ? null
                : (root, query, cb) -> cb.equal(root.join("assignee", JoinType.LEFT).get("id"), assigneeId);
    }

    /** Biriktirilmagan murojaatlar. */
    public static Specification<Complaint> unassigned(Boolean unassigned) {
        if (unassigned == null || !unassigned) {
            return null;
        }
        return (root, query, cb) -> cb.isNull(root.get("assignee"));
    }

    public static Specification<Complaint> createdAfter(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Complaint> createdBefore(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    /** Ochiq reyestr uchun: xodim yashirgan murojaatlar chiqmaydi. */
    public static Specification<Complaint> visibleInRegister() {
        return (root, query, cb) -> cb.isFalse(root.get("hiddenFromRegister"));
    }

    /** Sarlavha, matn, fan nomi va kuzatuv kodi bo'yicha erkin qidiruv - xodimlar uchun. */
    public static Specification<Complaint> textContains(String text) {
        String pattern = likePattern(text);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> matches = new ArrayList<>();
            matches.add(cb.like(cb.lower(root.get("title")), pattern));
            matches.add(cb.like(cb.lower(root.get("description")), pattern));
            matches.add(cb.like(cb.lower(root.get("subjectName")), pattern));
            matches.add(cb.like(cb.lower(root.get("trackingCode")), pattern));
            return cb.or(matches.toArray(new Predicate[0]));
        };
    }

    /**
     * Ochiq reyestr qidiruvi - faqat kuzatuv kodi bo'yicha.
     * Matn bo'yicha qidirishga ruxsat berilmaydi: u murojaat mazmunini
     * bilvosita oshkor qilish yo'li bo'lib qolardi.
     */
    public static Specification<Complaint> trackingCodeContains(String text) {
        String pattern = likePattern(text);
        return pattern == null
                ? null
                : (root, query, cb) -> cb.like(cb.lower(root.get("trackingCode")), pattern);
    }

    private static String likePattern(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
