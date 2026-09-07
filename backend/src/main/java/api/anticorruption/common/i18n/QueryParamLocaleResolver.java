package api.anticorruption.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * Tilni faqat {@code ?lang=} parametridan aniqlaydi.
 *
 * <p>Accept-Language sarlavhasi ataylab e'tiborga olinmaydi: shunda javob tili
 * doim so'rovda ko'rinib turadi va bir xil URL har doim bir xil natija beradi.
 * Bu keshlash va nosozlikni aniqlashni osonlashtiradi - brauzer sozlamasiga
 * qarab javob "o'zgarib turmaydi".
 *
 * <p>Parametr bo'lmasa yoki noma'lum til ko'rsatilsa {@link AppLanguage#DEFAULT}
 * ishlatiladi - xatolik qaytarilmaydi.
 */
public class QueryParamLocaleResolver implements LocaleResolver {

    /** So'rovdagi parametr nomi: {@code /api/v1/...?lang=ru}. */
    public static final String PARAMETER = "lang";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return AppLanguage.from(request.getParameter(PARAMETER)).getLocale();
    }

    @Override
    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response,
                          @Nullable Locale locale) {
        // Til har so'rovda parametrdan olinadi, hech qayerda saqlanmaydi:
        // API stateless bo'lgani uchun sessiya ham, cookie ham ishlatilmaydi.
        throw new UnsupportedOperationException(
                "Tilni o'zgartirish uchun so'rovga ?" + PARAMETER + "= parametrini qo'shing");
    }
}
