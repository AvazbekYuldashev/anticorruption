package api.anticorruption.common.i18n;

import api.anticorruption.common.LabeledEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Xabar kalitini joriy so'rov tiliga o'giradi.
 *
 * <p>Til {@link LocaleContextHolder} dan olinadi - uni har so'rov boshida
 * {@link QueryParamLocaleResolver} to'ldiradi.
 *
 * <p>Kalit topilmasa kalitning o'zi qaytariladi (MessageSource shunday
 * sozlangan): tarjima unutilsa API buzilmaydi, lekin nima yetishmayotgani
 * javobda darrov ko'rinadi.
 */
@Component
@RequiredArgsConstructor
public class Translator {

    private final MessageSource messageSource;

    /**
     * Kalit topilmasa kalitning o'zi qaytariladi: tarjima unutilsa API buzilmaydi,
     * lekin nima yetishmayotgani javobda darrov ko'rinadi.
     */
    public String get(String code, Object... args) {
        return messageSource.getMessage(code, args, code, currentLocale());
    }

    /** Enum konstantasining tarjima qilingan nomi. */
    public String of(LabeledEnum value) {
        return value == null ? null : get(value.messageKey());
    }

    public Locale currentLocale() {
        return LocaleContextHolder.getLocale();
    }

    public AppLanguage currentLanguage() {
        return AppLanguage.fromLocale(currentLocale());
    }
}
