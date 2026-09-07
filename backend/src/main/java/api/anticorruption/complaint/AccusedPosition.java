package api.anticorruption.complaint;

import api.anticorruption.common.LabeledEnum;

/**
 * Murojaat kimning harakati haqida ekanligi - lavozim darajasida.
 *
 * <p>Ataylab lavozim, ism emas: tekshiruv tugamaguncha aniq shaxsni bazada
 * ayblab qo'yish noto'g'ri bo'lardi. Aniq ism kerak bo'lsa, murojaatchi uni
 * murojaat matnida yozadi va u faqat xodimlarga ko'rinadi.
 *
 * <p>Nomlar tarjima faylida: {@code enum.AccusedPosition.*}.
 */
public enum AccusedPosition implements LabeledEnum {

    TEACHER,
    HEAD_OF_DEPARTMENT,
    DEAN,
    VICE_DEAN,
    RECTORATE,
    ADMISSION_COMMITTEE,
    DORMITORY_STAFF,
    ACCOUNTING,
    REGISTRAR,
    SECURITY,
    OTHER_STAFF,
    UNKNOWN
}
