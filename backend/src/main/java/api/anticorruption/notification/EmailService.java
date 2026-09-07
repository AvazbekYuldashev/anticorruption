package api.anticorruption.notification;

import api.anticorruption.common.LabeledEnum;
import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.common.i18n.MessageKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Murojaatchiga email xabarnoma yuboradi.
 *
 * <p>Muhim tafsilotlar:
 * <ul>
 *   <li>metodlar {@code @Async} - xat yuborish murojaatni saqlashni sekinlashtirmasin;</li>
 *   <li>til <em>parametr sifatida</em> beriladi: async oqimda
 *       {@code LocaleContextHolder} bo'sh bo'ladi, shuning uchun so'rov tilini
 *       o'sha yerdan olib bo'lmaydi;</li>
 *   <li>parametrlar oddiy qiymatlar (entity emas), chunki async oqimda JPA sessiyasi yo'q;</li>
 *   <li>SMTP sozlanmagan bo'lsa xatolik chiqarmaydi, faqat logga yozadi -
 *       xabarnoma yuborilmagani murojaatni yo'qotishga sabab bo'lmasligi kerak.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MessageSource messageSource;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@anticorruption.uz}")
    private String from;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    /** Murojaat qabul qilinganini va kuzatuv kodini xabar qiladi. */
    @Async
    public void sendComplaintReceived(String to, String languageCode, String trackingCode, String title) {
        if (to == null || to.isBlank()) {
            return;
        }
        Locale locale = AppLanguage.from(languageCode).getLocale();

        send(to,
                text(MessageKeys.EMAIL_RECEIVED_SUBJECT, locale, trackingCode),
                text(MessageKeys.EMAIL_RECEIVED_BODY, locale, trackingCode, title, publicUrl));
    }

    /** Murojaat holati o'zgarganini xabar qiladi. */
    @Async
    public void sendStatusChanged(String to, String languageCode, String trackingCode, String title,
                                  LabeledEnum oldStatus, LabeledEnum newStatus, String officialResponse) {
        if (to == null || to.isBlank()) {
            return;
        }
        Locale locale = AppLanguage.from(languageCode).getLocale();

        String responseBlock = (officialResponse == null || officialResponse.isBlank())
                ? ""
                : text(MessageKeys.EMAIL_STATUS_RESPONSE_BLOCK, locale, officialResponse);

        String body = text(MessageKeys.EMAIL_STATUS_BODY, locale,
                trackingCode,
                title,
                label(oldStatus, locale),
                label(newStatus, locale),
                responseBlock,
                publicUrl);

        send(to, text(MessageKeys.EMAIL_STATUS_SUBJECT, locale, trackingCode), body);
    }

    /**
     * Holat o'zgarmasdan yozilgan rasmiy javob haqida xabar qiladi.
     *
     * <p>{@link #sendStatusChanged} dan alohida: u xatda "oldingi holat ->
     * yangi holat" deb yozadi, bu yerda esa holat o'zgarmagan - faqat javob
     * qo'shilgan.
     */
    @Async
    public void sendResponseAdded(String to, String languageCode, String trackingCode, String title,
                                  String officialResponse) {
        if (to == null || to.isBlank()) {
            return;
        }
        Locale locale = AppLanguage.from(languageCode).getLocale();

        send(to,
                text(MessageKeys.EMAIL_RESPONSE_SUBJECT, locale, trackingCode),
                text(MessageKeys.EMAIL_RESPONSE_BODY, locale, trackingCode, title, officialResponse, publicUrl));
    }

    private String text(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }

    private String label(LabeledEnum value, Locale locale) {
        return value == null ? "-" : messageSource.getMessage(value.messageKey(), null, value.name(), locale);
    }

    private void send(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!enabled || mailSender == null) {
            log.info("Email o'chirilgan, yuborilmadi. Qabul qiluvchi: {}, mavzu: {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email yuborildi: {}", subject);
        } catch (MailException ex) {
            log.error("Email yuborishda xatolik. Qabul qiluvchi: {}, mavzu: {}", to, subject, ex);
        }
    }
}
