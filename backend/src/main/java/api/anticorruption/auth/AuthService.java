package api.anticorruption.auth;

import api.anticorruption.auth.dto.AuthResponse;
import api.anticorruption.auth.dto.LoginRequest;
import api.anticorruption.common.exception.UnauthorizedException;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.security.JwtService;
import api.anticorruption.user.User;
import api.anticorruption.user.UserRepository;
import api.anticorruption.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Tizimga kirish va seansni yangilash mantig'i.
 *
 * <p>Ikkala amal ham bir xil natija qaytaradi: qisqa muddatli kirish tokeni
 * va yangi yangilash tokeni. Ularni cookie ga joylash kontrollerning ishi -
 * bu yerda HTTP haqida hech narsa bilinmaydi.
 *
 * <p>Xatolik matnlari bu yerda yozilmaydi: Spring Security istisnolarining
 * turi kifoya, tarjimani {@code GlobalExceptionHandler} qiladi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final Translator translator;

    @Transactional
    public Session login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (DisabledException ex) {
            throw ex;
        } catch (AuthenticationException ex) {
            // Email mavjudligini oshkor qilmaslik uchun sabab aniqlashtirilmaydi.
            throw new BadCredentialsException("bad credentials", ex);
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));

        return session(user, refreshTokenService.issue(user));
    }

    /**
     * Yangilash tokenini yangisiga almashtiradi va yangi kirish tokeni beradi.
     *
     * <p>Eski token shu zahoti bekor qilinadi (rotation): agar u birovda ham
     * bo'lsa, keyingi urinishda qayta ishlatish aniqlanadi va foydalanuvchining
     * barcha seanslari uziladi.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Session refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        return session(rotation.user(), rotation.refreshToken());
    }

    /**
     * Foydalanuvchiga yangi seans beradi (parolni almashtirgandan keyin).
     *
     * <p>Parol almashtirilganda barcha yangilash tokenlari bekor qilinadi -
     * shu jumladan joriy qurilmaniki ham. Agar shundan keyin yangisi
     * berilmasa, foydalanuvchi o'z parolini almashtirgani uchun o'zi
     * tizimdan chiqib qolardi.
     */
    @Transactional
    public Session renew(User user) {
        return session(user, refreshTokenService.issue(user));
    }

    private Session session(User user, String refreshToken) {
        return new Session(
                jwtService.generateToken(user),
                refreshToken,
                AuthResponse.of(jwtService.expiresInSeconds(), UserResponse.from(user, translator)));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Ochilgan seans: cookie ga joylanadigan ikkita token va mijozga
     * qaytariladigan javob tanasi.
     */
    public record Session(String accessToken, String refreshToken, AuthResponse body) {
    }
}
