package api.anticorruption.complaint.dto;

/**
 * Murojaatni xodimga biriktirish so'rovi.
 *
 * @param assigneeId mas'ul xodim id si; {@code null} bo'lsa biriktiruv bekor qilinadi
 */
public record AssignRequest(Long assigneeId) {
}
