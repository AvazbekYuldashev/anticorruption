package api.anticorruption.poll;

import api.anticorruption.common.LabeledEnum;

/**
 * So'rovnomaning joriy holati.
 *
 * <p>Holat saqlanmaydi, har safar sana, {@code active} bayrog'i va
 * to'xtatilgan vaqtdan hisoblanadi: saqlansa uni muddat tugaganda
 * kimdir yangilab turishi kerak bo'lardi va unutilgan yozuv "ochiq"
 * bo'lib qolardi.
 */
public enum PollStatus implements LabeledEnum {

    /** Faollashtirilmagan - saytda ko'rinmaydi. */
    DRAFT,

    /** Faol, lekin boshlanish sanasi hali kelmagan. */
    SCHEDULED,

    /** Hozir ovoz berish mumkin. */
    OPEN,

    /**
     * Administrator qo'lda to'xtatgan.
     *
     * <p>Saytda ko'rinmaydi, lekin admin panelida natijalari qoladi.
     * Qayta o'tkazish esa alohida yangi so'rovnoma ochadi - eski
     * natijalar yangisiga qo'shilib ketmaydi.
     */
    STOPPED,

    /** Muddati tugagan. */
    CLOSED
}
