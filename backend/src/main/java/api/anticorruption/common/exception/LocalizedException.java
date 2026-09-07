package api.anticorruption.common.exception;

/**
 * Foydalanuvchiga ko'rsatiladigan, tarjima qilinadigan xatolik.
 *
 * <p>Xabar matni emas, xabar <em>kaliti</em> saqlanadi. Matn faqat javob
 * yozilayotganda, so'rov tiliga qarab hosil qilinadi
 * ({@code GlobalExceptionHandler} shu ishni qiladi).
 *
 * <p>{@code getMessage()} kalitning o'zini qaytaradi - loglarda va stack
 * trace'da nima bo'lganini ko'rish uchun shu yetarli, tarjima esa u yerda
 * keraksiz.
 */
public abstract class LocalizedException extends RuntimeException {

    private final String code;

    /**
     * Xabar shabloniga qo'yiladigan qiymatlar.
     *
     * <p>{@code transient}: xatolik serializatsiya qilinsa, argumentlar
     * ichida serializatsiya qilinmaydigan obyekt bo'lishi mumkin.
     */
    private final transient Object[] args;

    protected LocalizedException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
