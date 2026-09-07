package api.anticorruption.auth;

import api.anticorruption.auth.dto.AuthResponse;
import api.anticorruption.auth.dto.LoginRequest;
import api.anticorruption.auth.dto.RegisterRequest;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.security.JwtService;
import api.anticorruption.user.Role;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Ro'yxatdan o'tish va tizimga kirish mantig'i.
 *
 * <p>Xatolik matnlari bu yerda yozilmaydi: Spring Security istisnolarining
 * turi kifoya, tarjimani {@code GlobalExceptionHandler} qiladi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Translator translator;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(MessageKeys.ERROR_AUTH_EMAIL_TAKEN);
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(blankToNull(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CITIZEN)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Yangi foydalanuvchi ro'yxatdan o'tdi: id={}", user.getId());

        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
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

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return AuthResponse.of(
                jwtService.generateToken(user),
                jwtService.expiresInSeconds(),
                UserResponse.from(user, translator));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
