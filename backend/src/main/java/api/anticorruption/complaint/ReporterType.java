package api.anticorruption.complaint;

import api.anticorruption.common.LabeledEnum;

/**
 * Murojaatchining universitetga qanday aloqadorligi.
 *
 * <p>Anonim murojaatda ham so'raladi: shaxsni oshkor qilmaydi, lekin
 * qaysi guruhdan qancha murojaat kelayotganini ko'rsatadi - bu muammo
 * qayerda ekanligini tushunishga yordam beradi.
 *
 * <p>Nomlar tarjima faylida: {@code enum.ReporterType.*}.
 */
public enum ReporterType implements LabeledEnum {

    STUDENT,
    MASTER_STUDENT,
    TEACHER,
    STAFF,
    PARENT,
    ALUMNI,
    EXTERNAL;

    /** Kurs va guruh maydonlari faqat talabalar uchun ma'noli. */
    public boolean isStudent() {
        return this == STUDENT || this == MASTER_STUDENT;
    }
}
