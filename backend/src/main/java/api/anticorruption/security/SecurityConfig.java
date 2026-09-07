package api.anticorruption.security;

import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.QueryParamLocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Stateless JWT autentifikatsiyasi.
 *
 * <p>Ochiq (token talab qilinmaydigan) yo'llar: ro'yxatdan o'tish/kirish,
 * murojaat yuborish, tracking kod bo'yicha holatni tekshirish, ma'lumotnomalar,
 * ommaviy statistika va Swagger hujjatlari.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppUserDetailsService userDetailsService;
    private final MessageSource messageSource;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                /*
                 * Brauzer himoyasi sarlavhalari. HSTS faqat HTTPS ustida
                 * ma'noga ega - ilova teskari proksi ortida TLS bilan
                 * ishlaydi deb hisoblanadi.
                 */
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // API JSON qaytaradi; yuklangan fayl brauzerda skript sifatida ishlamasin.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; "
                                        + "img-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "script-src 'self'; connect-src 'self'")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(request, response, HttpStatus.UNAUTHORIZED,
                                        MessageKeys.ERROR_AUTH_REQUIRED))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(request, response, HttpStatus.FORBIDDEN,
                                        MessageKeys.ERROR_ACCESS_DENIED)))
                .authorizeHttpRequests(auth -> auth
                        // --- Ochiq yollar ---
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/complaints").permitAll()
                        .requestMatchers("/api/v1/complaints/track/**").permitAll()
                        .requestMatchers("/api/v1/reference/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stats/**").permitAll()

                        // --- Saytning ochiq bo'limlari ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/news", "/api/v1/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/staff").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/about").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/media/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/polls", "/api/v1/polls/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/polls/*/vote").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- Faqat administrator: foydalanuvchilar va universitet tuzilmasi ---
                        .requestMatchers("/api/v1/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/faculties/**").hasRole("ADMIN")

                        // --- Moderator va administrator: murojaatlar, statistika, kontent ---
                        // Ochiq reyestr ham shu yerda: u saytda ko'rsatilmaydi,
                        // faqat xodimlar ko'radigan ro'yxat bo'lib qoldi.
                        .requestMatchers(HttpMethod.GET, "/api/v1/complaints/register")
                        .hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("MODERATOR", "ADMIN")

                        // --- Qolgan hammasi token talab qiladi ---
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Login uchun ishlatiladigan menejer.
     *
     * <p>Provayder ataylab alohida bean sifatida chiqarilmagan: aks holda Spring Security
     * uni global konfiguratsiyaga ham ulab, ogohlantirish beradi. Bu yerda u faqat
     * {@code AuthService.login()} da parolni tekshirish uchun kerak - qolgan so'rovlarda
     * foydalanuvchini {@link JwtAuthenticationFilter} o'zi aniqlaydi.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Xatolikni JSON ko'rinishida yozadi.
     *
     * <p>Bu yerda hali filtr zanjiridamiz: {@code @RestControllerAdvice} ham,
     * {@code LocaleContextHolder} ham hali to'ldirilmagan (ularni DispatcherServlet
     * qiladi). Shuning uchun JSON qo'lda yoziladi va til to'g'ridan-to'g'ri
     * so'rov parametridan olinadi.
     */
    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String code) throws IOException {
        Locale locale = AppLanguage.from(request.getParameter(QueryParamLocaleResolver.PARAMETER)).getLocale();
        String message = messageSource.getMessage(code, null, code, locale);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"error":"%s","code":"%s","message":"%s","path":"%s"}"""
                .formatted(Instant.now(), status.value(), status.getReasonPhrase(),
                        code, escapeJson(message), escapeJson(request.getRequestURI())));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
