package api.anticorruption.security;

import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.QueryParamLocaleResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ochiq yozuv amallarini bir IP dan keladigan oqimdan himoyalaydi.
 *
 * <p>Autentifikatsiyasiz ochiq bo'lgan yo'llar - kirish, ro'yxatdan o'tish,
 * murojaat yuborish, ovoz berish - avtomatik urinishlar uchun eng qulay
 * nishon. Chegara qo'yilmasa parolni saralab topish yoki reyestrni soxta
 * murojaatlar bilan to'ldirish hech narsaga to'sqinlik qilmaydi.
 *
 * <p>Hisob xotirada saqlanadi: ilova bitta nusxada ishlaganda shu yetarli.
 * Bir nechta nusxaga chiqilganda hisobni Redis kabi umumiy omborga
 * ko'chirish kerak bo'ladi - shunda chegara butun klaster uchun umumiy
 * bo'ladi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    /** Chegara qo'yiladigan yo'llar: hammasi ochiq va hammasi yozadi. */
    private static final List<String> GUARDED_PREFIXES = List.of(
            "/api/v1/auth/",
            "/api/v1/complaints",
            "/api/v1/polls/");

    private final RateLimitProperties properties;
    private final MessageSource messageSource;

    /** IP -> joriy oynadagi urinishlar. */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        // O'qish so'rovlari chegaralanmaydi: ular hech narsani o'zgartirmaydi.
        if (!HttpMethod.POST.matches(request.getMethod())
                && !HttpMethod.PUT.matches(request.getMethod())
                && !HttpMethod.PATCH.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return GUARDED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String client = clientIp(request);
        long now = System.currentTimeMillis();
        long windowMillis = properties.window().toMillis();

        Window window = windows.compute(client, (key, existing) ->
                existing == null || now - existing.startedAt >= windowMillis
                        ? new Window(now)
                        : existing);

        if (window.hits.incrementAndGet() > properties.requests()) {
            log.warn("So'rovlar chegarasidan oshildi: ip={}, yo'l={}", client, request.getRequestURI());
            writeTooManyRequests(request, response);
            return;
        }

        // Eskirgan yozuvlar to'planib qolmasin: tozalash arzon va kamdan-kam.
        if (windows.size() > properties.maxTrackedClients()) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis);
        }

        chain.doFilter(request, response);
    }

    /**
     * Mijoz manzili.
     *
     * <p>Ilova teskari proksi ortida turadi, shuning uchun {@code X-Forwarded-For}
     * dagi birinchi manzil olinadi. Sarlavhaga faqat proksi o'zi qo'yganda
     * ishonish mumkin - shuning uchun uni tashqaridan kelgan so'rovda
     * proksi tozalab tashlashi kerak.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Locale locale = AppLanguage.from(request.getParameter(QueryParamLocaleResolver.PARAMETER)).getLocale();
        String code = MessageKeys.ERROR_TOO_MANY_REQUESTS;
        String message = messageSource.getMessage(code, null, code, locale);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(properties.window().toSeconds()));
        response.getWriter().write("""
                {"timestamp":"%s","status":429,"error":"Too Many Requests","code":"%s","message":"%s","path":"%s"}"""
                .formatted(Instant.now(), code, escapeJson(message), escapeJson(request.getRequestURI())));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Bitta mijozning joriy oynasi. */
    private static final class Window {
        private final long startedAt;
        private final AtomicInteger hits = new AtomicInteger();

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
