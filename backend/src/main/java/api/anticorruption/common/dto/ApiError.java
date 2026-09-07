package api.anticorruption.common.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Barcha xatoliklar uchun yagona javob formati.
 *
 * @param status  HTTP status kodi
 * @param error   status nomi, masalan "Not Found" (HTTP standarti, tarjima qilinmaydi)
 * @param code    xatolik kaliti, masalan {@code error.complaint.notFoundByCode};
 *                frontend tilga bog'liq bo'lmagan holda shu bo'yicha qaror qabul qiladi
 * @param message joriy tilga o'girilgan, foydalanuvchiga ko'rsatiladigan xabar
 * @param path    so'rov yuborilgan manzil
 * @param fields  maydonlar bo'yicha validatsiya xatoliklari (bo'lmasa null)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> fields
) {
    public static ApiError of(int status, String error, String code, String message, String path) {
        return new ApiError(Instant.now(), status, error, code, message, path, null);
    }
}
