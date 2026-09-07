package api.anticorruption.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

/** Tarjimalar va til aniqlash sozlamalari. */
@Configuration
public class I18nConfig {

    /**
     * Tarjima fayllari: {@code src/main/resources/i18n/messages*.properties}.
     *
     * <p>{@link ResourceBundleMessageSource} ataylab tanlangan (Reloadable emas):
     * faqat u {@code uz-Cyrl} kabi yozuv (script) belgisi bor tillarni to'g'ri
     * qo'llab-quvvatlaydi va {@code messages_uz_Cyrl.properties} faylini topadi.
     *
     * <p>Kalit topilmasa asosiy {@code messages.properties} (o'zbekcha) ishlatiladi.
     * Tizim tiliga qaytish o'chirilgan - serverning tili javobga ta'sir qilmasin.
     *
     * <p>{@code useCodeAsDefaultMessage} ataylab yoqilmagan. Yoqilsa, har qanday
     * noma'lum kalit o'zining nomini qaytaradi va Hibernate Validator buni
     * suiiste'mol qiladi: u {@code {min}} va {@code {max}} ni ham lug'at kaliti
     * deb qidiradi va cheklov qiymati o'rniga "min", "max" so'zlarini qo'yadi.
     * Bizning xabarlarimiz uchun standart qiymat {@code Translator} da beriladi.
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver() {
        return new QueryParamLocaleResolver();
    }

    /**
     * Validatsiya xabarlarini ham shu tarjima fayllaridan oladi.
     *
     * <p>Bean nomi {@code defaultValidator} bo'lishi shart: Spring Boot o'zining
     * validatorini shu nom bilan yaratadi, biz uni almashtirmoqchimiz. Aks holda
     * {@code @NotBlank(message = "{validation.xyz}")} kalitlari tarjima qilinmasdan
     * xom holda qaytardi.
     */
    @Bean
    public LocalValidatorFactoryBean defaultValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }
}
