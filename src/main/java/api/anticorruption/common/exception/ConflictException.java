package api.anticorruption.common.exception;

/** Resurs allaqachon mavjud yoki holat mos kelmaganda tashlanadi (HTTP 409). */
public class ConflictException extends LocalizedException {

    public ConflictException(String code, Object... args) {
        super(code, args);
    }
}
