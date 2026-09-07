package api.anticorruption.auth.dto;

import api.anticorruption.user.dto.UserResponse;

/**
 * Muvaffaqiyatli kirish yoki seansni yangilash javobi.
 *
 * <p>Token bu yerda YO'Q va ataylab yo'q: u {@code HttpOnly} cookie orqali
 * beriladi, ya'ni sahifadagi JavaScript unga umuman kira olmaydi. Shu
 * tufayli XSS topilgan taqdirda ham token o'g'irlanmaydi.
 *
 * @param expiresIn kirish tokeni necha sekunddan keyin eskiradi - interfeys
 *                  shu vaqtdan oldin seansni yangilab qo'yishi mumkin
 * @param user      foydalanuvchi ma'lumotlari
 */
public record AuthResponse(
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(long expiresIn, UserResponse user) {
        return new AuthResponse(expiresIn, user);
    }
}
