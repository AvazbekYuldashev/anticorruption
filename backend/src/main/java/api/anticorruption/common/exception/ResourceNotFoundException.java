package api.anticorruption.common.exception;

/** So'ralgan resurs bazada topilmaganda tashlanadi (HTTP 404). */
public class ResourceNotFoundException extends LocalizedException {

    public ResourceNotFoundException(String code, Object... args) {
        super(code, args);
    }
}
