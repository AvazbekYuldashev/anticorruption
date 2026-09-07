package api.anticorruption.poll;

/**
 * Test hisoboti uchun bitta yozuv: kim, qaysi savolda, qaysi variantni tanlagan.
 *
 * <p>Entity o'rniga proyeksiya olinadi: hisobot uchun faqat id lar kerak,
 * ovozlarning o'zini yuklash esa katta so'rovnomada qimmatga tushardi.
 */
public record QuizAnswerRow(Long questionId, Long optionId, String voterKey) {
}
