package api.anticorruption.common.exception;

import api.anticorruption.common.dto.ApiError;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kontrollerlardan chiqqan xatoliklarni yagona JSON formatiga o'giradi va
 * xabar matnini so'rov tiliga tarjima qiladi.
 *
 * <p>Servislar xabar matnini emas, xabar <em>kalitini</em> tashlaydi
 * ({@link LocalizedException}) - shuning uchun tarjima aynan shu yerda,
 * javob yozilayotgan paytda amalga oshiriladi.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Translator translator;

    // ---------------------------------------------------------------- o'z xatoliklarimiz

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getArgs(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getArgs(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getArgs(), request);
    }

    // ---------------------------------------------------------------- so'rov formati

    /**
     * {@code @Valid} bilan belgilangan DTO tekshiruvdan o'tmaganda maydonlar
     * ro'yxatini qaytaradi. Maydon xabarlari validator tomonidan allaqachon
     * joriy tilga o'girilgan bo'ladi.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                MessageKeys.ERROR_VALIDATION_FAILED,
                translator.get(MessageKeys.ERROR_VALIDATION_FAILED),
                request.getRequestURI(),
                fields
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * So'rov tanasi umuman JSON ga aylanmasa: buzilgan JSON, noto'g'ri enum qiymati,
     * yoki maydon turi mos kelmasligi. Sabab tafsilotini mijozga bermaymiz -
     * u ichki sinf nomlarini oshkor qilishi mumkin.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        log.debug("So'rov tanasini o'qib bo'lmadi: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, MessageKeys.ERROR_REQUEST_UNREADABLE, null, request);
    }

    /** URL parametri noto'g'ri turda, masalan ?status=YOQ yoki ?page=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, MessageKeys.ERROR_REQUEST_PARAMETER_INVALID,
                new Object[]{ex.getName()}, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, MessageKeys.ERROR_REQUEST_PARAMETER_MISSING,
                new Object[]{ex.getParameterName()}, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, MessageKeys.FILE_TOO_LARGE, null, request);
    }

    // ---------------------------------------------------------------- huquq

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, MessageKeys.ERROR_ACCESS_DENIED, null, request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, MessageKeys.ERROR_AUTH_ACCOUNT_DISABLED, null, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, MessageKeys.ERROR_AUTH_BAD_CREDENTIALS, null, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, MessageKeys.ERROR_AUTH_REQUIRED, null, request);
    }

    // ---------------------------------------------------------------- qolgani

    /** Kutilmagan xatoliklar: mijozga tafsilot bermaymiz, logga to'liq yozamiz. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Kutilmagan xatolik: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MessageKeys.ERROR_INTERNAL, null, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, Object[] args,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                code,
                translator.get(code, args),
                request.getRequestURI()));
    }
}
