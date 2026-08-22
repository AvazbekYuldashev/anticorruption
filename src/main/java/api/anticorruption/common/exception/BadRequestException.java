package api.anticorruption.common.exception;

/** Mijoz noto'g'ri ma'lumot yuborganda tashlanadi (HTTP 400). */
public class BadRequestException extends LocalizedException {

    public BadRequestException(String code, Object... args) {
        super(code, args);
    }
}
