package api.anticorruption.auth.dto;

import api.anticorruption.user.dto.UserResponse;

/**
 * Muvaffaqiyatli kirish yoki ro'yxatdan o'tish javobi.
 *
 * @param accessToken JWT token
 * @param tokenType   doim "Bearer"
 * @param expiresIn   token necha sekunddan keyin eskiradi
 * @param user        foydalanuvchi ma'lumotlari
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
