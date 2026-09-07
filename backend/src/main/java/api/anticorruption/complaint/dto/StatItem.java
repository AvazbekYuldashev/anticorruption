package api.anticorruption.complaint.dto;

/**
 * Statistikaning bitta qatori.
 *
 * @param key   enum nomi - frontendda filtrlash uchun
 * @param label o'zbekcha nomi - ekranga chiqarish uchun
 * @param count nechta murojaat
 */
public record StatItem(String key, String label, long count) {
}
