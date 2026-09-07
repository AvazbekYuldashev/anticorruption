package api.anticorruption.security;

import api.anticorruption.auth.AuthCookieService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Har bir so'rovda kirish tokenini topib, foydalanuvchini SecurityContext ga joylaydi.
 *
 * <p>Token avval {@code HttpOnly} cookie'dan qidiriladi - brauzer uchun asosiy
 * yo'l shu. Cookie bo'lmasa "Authorization: Bearer" sarlavhasiga qaraladi:
 * bu brauzerdan tashqari mijozlar (skript, integratsiya) uchun qoldirilgan.
 *
 * <p>Token bo'lmasa yoki yaroqsiz bo'lsa so'rov to'xtatilmaydi - shunchaki
 * autentifikatsiyasiz davom etadi. Ruxsat masalasini keyin SecurityFilterChain hal qiladi.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final AuthCookieService cookieService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parse(token).ifPresent(claims -> authenticate(claims, request));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!userDetails.isEnabled()) {
                return;
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException ex) {
            // Token to'g'ri, lekin foydalanuvchi bazadan o'chirilgan - autentifikatsiyasiz davom etamiz.
            logger.debug("Token egasi bazada topilmadi: " + claims.getSubject());
        }
    }

    private String extractToken(HttpServletRequest request) {
        return cookieService.readAccessToken(request).orElseGet(() -> headerToken(request));
    }

    private String headerToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String value = header.substring(PREFIX.length()).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}
