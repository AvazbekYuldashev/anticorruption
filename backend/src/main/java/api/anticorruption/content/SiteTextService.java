package api.anticorruption.content;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.content.dto.SaveSiteTextsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bosh sahifa matnlari: administrator yozadi, sayt o'qiydi.
 *
 * <p>Sayt tarjima fayllaridagi asl matnlar bilan ishga tushadi va shu yerdan
 * kelgan matnlar bilan ularning ustidan yozadi. Yozuvi yo'q matn asl holicha
 * qoladi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteTextService {

    private final SiteTextRepository siteTextRepository;

    /**
     * Barcha o'zgartirilgan matnlar: til kodi -> (kalit -> matn).
     *
     * <p>Hamma tillar birga qaytadi: matnlar oz, sayt esa tilni almashtirganda
     * qayta so'rov yubormasligi kerak.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> all() {
        Map<String, Map<String, String>> byLanguage = new LinkedHashMap<>();
        for (AppLanguage language : AppLanguage.values()) {
            byLanguage.put(language.getCode(), new LinkedHashMap<>());
        }

        for (SiteText text : siteTextRepository.findAllByOrderByLanguageCodeAscTextKeyAsc()) {
            byLanguage.computeIfAbsent(text.getLanguageCode(), code -> new LinkedHashMap<>())
                    .put(text.getTextKey(), text.getTextValue());
        }
        return byLanguage;
    }

    /**
     * Matnlarni saqlaydi.
     *
     * <p>So'rov to'liq holat: unda yo'q yoki bo'sh qiymatli matn o'chiriladi va
     * sayt asl matnga qaytadi. Mavjud yozuv o'chirilib qayta yaratilmaydi,
     * yangilanadi - noyoblik cheklovi bir tranzaksiyada to'qnashmasin.
     */
    @Transactional
    public Map<String, Map<String, String>> save(SaveSiteTextsRequest request) {
        Map<String, String> wanted = new LinkedHashMap<>();

        for (SaveSiteTextsRequest.Item item : request.texts()) {
            String key = item.key().trim();
            String language = item.language().trim().toLowerCase(java.util.Locale.ROOT);

            if (SiteTextKey.fromKey(key).isEmpty()) {
                throw new BadRequestException(MessageKeys.SITE_TEXT_UNKNOWN_KEY, key);
            }
            // Til kodi aynan bo'lishi kerak: noma'lum kod standart tilga
            // tushib qolsa, ruscha matn o'zbekcha sahifaga yozilib ketardi.
            if (Arrays.stream(AppLanguage.values()).noneMatch(value -> value.getCode().equals(language))) {
                throw new BadRequestException(MessageKeys.SITE_TEXT_UNKNOWN_LANGUAGE, item.language());
            }

            String value = item.value().strip();
            if (!value.isEmpty()) {
                wanted.put(identity(key, language), value);
            }
        }

        Map<String, SiteText> existing = new HashMap<>();
        for (SiteText text : siteTextRepository.findAll()) {
            existing.put(identity(text.getTextKey(), text.getLanguageCode()), text);
        }

        List<SiteText> removed = existing.entrySet().stream()
                .filter(entry -> !wanted.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        siteTextRepository.deleteAll(removed);

        for (Map.Entry<String, String> entry : wanted.entrySet()) {
            SiteText text = existing.get(entry.getKey());
            if (text == null) {
                String[] parts = entry.getKey().split("\\|", 2);
                text = SiteText.builder().textKey(parts[0]).languageCode(parts[1]).build();
            }
            if (!entry.getValue().equals(text.getTextValue())) {
                text.setTextValue(entry.getValue());
                siteTextRepository.save(text);
            }
        }

        log.info("Bosh sahifa matnlari saqlandi: {} ta o'zgartirilgan, {} ta asl holiga qaytdi",
                wanted.size(), removed.size());
        return all();
    }

    /** Kalitda ham, til kodida ham "|" belgisi yo'q - ajratgich sifatida xavfsiz. */
    private static String identity(String key, String languageCode) {
        return key + "|" + languageCode;
    }
}
