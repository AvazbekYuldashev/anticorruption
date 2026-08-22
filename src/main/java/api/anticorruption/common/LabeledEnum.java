package api.anticorruption.common;

/**
 * Foydalanuvchiga ko'rsatiladigan nomi tarjima faylida turadigan sanaluvchi tur.
 *
 * <p>Nom enum ichida saqlanmaydi: u {@code i18n/messages*.properties} da
 * {@code enum.<TurNomi>.<KONSTANTA>} kaliti ostida yotadi. Shu tufayli
 * yangi til qo'shish uchun Java kodini o'zgartirish shart emas.
 *
 * <p>Frontend ochiluvchi ro'yxatlarni {@code /api/v1/reference} dan oladi:
 * {@code name()} - mashina uchun qiymat, tarjima esa joriy tilga qarab keladi.
 */
public interface LabeledEnum {

    /** Enum konstantasining nomi - API da shu qiymat yuboriladi. */
    String name();

    /**
     * Tarjima kaliti, masalan {@code enum.ComplaintStatus.NEW}.
     *
     * <p>Tur nomi {@code getDeclaringClass()} orqali olinadi: agar enum
     * konstantasining o'z tanasi bo'lsa, {@code getClass()} anonim sinfni
     * qaytarib, kalit noto'g'ri chiqardi.
     */
    default String messageKey() {
        Class<?> type = this instanceof Enum<?> constant ? constant.getDeclaringClass() : getClass();
        return "enum." + type.getSimpleName() + "." + name();
    }
}
