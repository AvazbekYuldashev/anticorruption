package api.anticorruption.complaint;

import api.anticorruption.common.LabeledEnum;

/**
 * Universitetda uchraydigan korrupsiya holatlarining turlari.
 *
 * <p>Ro'yxat ataylab batafsil: umumiy "poraxo'rlik" o'rniga aniq holat
 * tanlangani statistikani ma'noli qiladi va murojaatni to'g'ri xodimga
 * yo'naltirishga yordam beradi.
 *
 * <p>Nomlar tarjima faylida: {@code enum.ComplaintCategory.*}.
 */
public enum ComplaintCategory implements LabeledEnum {

    EXAM_BRIBERY,
    GRADE_SELLING,
    RETAKE_PAYMENT,
    ATTENDANCE_PAYMENT,
    ACADEMIC_FRAUD,
    THESIS_FRAUD,
    ADMISSION_FRAUD,
    TRANSFER_FRAUD,
    SCHOLARSHIP_FRAUD,
    CONTRACT_PAYMENT_FRAUD,
    DORMITORY_CORRUPTION,
    FORCED_COLLECTION,
    PROCUREMENT_FRAUD,
    HIRING_NEPOTISM,
    ABUSE_OF_POWER,
    PROPERTY_MISUSE,
    OTHER
}
