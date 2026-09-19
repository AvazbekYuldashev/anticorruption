package api.anticorruption.poll;

/**
 * Hisobot uchun bitta yozuv: ishtirokchi va unga berilgan savollar soni.
 *
 * <p>{@link QuizAnswerRow} kabi entity o'rniga proyeksiya: to'plamning
 * savollar ro'yxati hisobotga kerak emas.
 */
public record AttemptSizeRow(String voterKey, int questionCount) {
}
