package api.anticorruption.common.exception;

/** Autentifikatsiya o'tmadi yoki seans yaroqsiz (HTTP 401). */
public class UnauthorizedException extends LocalizedException {

    public UnauthorizedException(String code, Object... args) {
        super(code, args);
    }
}
