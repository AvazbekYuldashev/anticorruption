package api.anticorruption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tarjimalar to'rtala tilda ham ishlashini tekshiradi.
 *
 * <p>Til faqat {@code ?lang=} parametridan olinadi, shuning uchun testlar
 * Accept-Language sarlavhasini umuman ishlatmaydi.
 */
class I18nIntegrationTest extends AbstractIntegrationTest {

    // ------------------------------------------------------------- enum nomlari

    @ParameterizedTest(name = "lang={0} -> {1}")
    @DisplayName("Murojaat holati nomi tanlangan tilda qaytadi")
    @CsvSource({
            "uz,      Yangi",
            "uz-cyrl, Янги",
            "ru,      Новое",
            "en,      New"
    })
    void statusLabelIsTranslated(String lang, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/reference/statuses").param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.value == 'NEW')].label").value(expected));
    }

    @ParameterizedTest(name = "lang={0} -> {1}")
    @DisplayName("Murojaatchi maqomi nomi tanlangan tilda qaytadi")
    @CsvSource({
            "uz,      Talaba",
            "uz-cyrl, Талаба",
            "ru,      Студент",
            "en,      Student"
    })
    void reporterTypeLabelIsTranslated(String lang, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/reference/reporter-types").param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.value == 'STUDENT')].label").value(expected));
    }

    @ParameterizedTest(name = "lang={0} -> {1}")
    @DisplayName("Rol nomi tanlangan tilda qaytadi")
    @CsvSource({
            "uz,      Foydalanuvchi",
            "uz-cyrl, Фойдаланувчи",
            "ru,      Пользователь",
            "en,      User"
    })
    void roleLabelIsTranslated(String lang, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/reference/roles").param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.value == 'CITIZEN')].label").value(expected));
    }

    @Test
    @DisplayName("Til ko'rsatilmasa o'zbekcha (lotin) qaytadi")
    void defaultLanguageIsUzbekLatin() throws Exception {
        mockMvc.perform(get("/api/v1/reference/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.value == 'NEW')].label").value("Yangi"));
    }

    @Test
    @DisplayName("Noma'lum til ko'rsatilsa xatolik emas, standart til qaytadi")
    void unknownLanguageFallsBackToDefault() throws Exception {
        mockMvc.perform(get("/api/v1/reference/statuses").param("lang", "de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.value == 'NEW')].label").value("Yangi"));
    }

    @Test
    @DisplayName("Qo'llab-quvvatlanadigan tillar ro'yxati ochiq")
    void supportedLanguagesAreListed() throws Exception {
        mockMvc.perform(get("/api/v1/reference/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.value == 'uz-cyrl')].label").value("Ўзбекча"))
                .andExpect(jsonPath("$[?(@.value == 'ru')].label").value("Русский"));
    }

    @Test
    @DisplayName("Ma'lumotnoma javobida joriy til ko'rsatiladi")
    void referenceReportsCurrentLanguage() throws Exception {
        mockMvc.perform(get("/api/v1/reference").param("lang", "ru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("ru"));
    }

    // ------------------------------------------------------------- xatolik xabarlari

    @ParameterizedTest(name = "lang={0}")
    @DisplayName("Topilmadi xatoligi tanlangan tilda qaytadi va kalit doim bir xil")
    @CsvSource({
            "uz,      Bunday kuzatuv kodi bilan murojaat topilmadi: AC-2026-XXXXXX",
            "uz-cyrl, Бундай кузатув коди билан мурожаат топилмади: AC-2026-XXXXXX",
            "ru,      'Обращение с таким кодом отслеживания не найдено: AC-2026-XXXXXX'",
            "en,      'No report found with this tracking code: AC-2026-XXXXXX'"
    })
    void notFoundMessageIsTranslated(String lang, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/complaints/track/{code}", "AC-2026-XXXXXX").param("lang", lang))
                .andExpect(status().isNotFound())
                // Kalit tilga bog'liq emas - frontend shu bo'yicha qaror qabul qiladi
                .andExpect(jsonPath("$.code").value("error.complaint.notFoundByCode"))
                .andExpect(jsonPath("$.message").value(expected));
    }

    @ParameterizedTest(name = "lang={0} -> {1}")
    @DisplayName("Validatsiya xabarlari ham tarjima qilinadi")
    @CsvSource({
            "uz, Bu maydon to'ldirilishi shart",
            "ru, Это поле обязательно для заполнения",
            "en, This field is required"
    })
    void validationMessageIsTranslated(String lang, String expected) throws Exception {
        mockMvc.perform(post("/api/v1/complaints")
                        .param("lang", lang)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": null, "description": null, "category": null, "reporterType": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation.failed"))
                .andExpect(jsonPath("$.fields.category").value(expected));
    }

    @Test
    @DisplayName("Uzunlik cheklovi xabariga min va max qiymatlari qo'yiladi")
    void sizeMessageIncludesBounds() throws Exception {
        mockMvc.perform(post("/api/v1/complaints")
                        .param("lang", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "qisqa",
                                  "description": "juda qisqa",
                                  "category": "OTHER",
                                  "reporterType": "STUDENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").value("Length must be between 10 and 200 characters"));
    }

    @Test
    @DisplayName("Tizimga kirish talab qilinishi haqidagi xabar ham tarjima qilinadi")
    void securityFilterMessageIsTranslated() throws Exception {
        // Bu xabar filtr zanjirida yoziladi, ya'ni MVC dan tashqarida -
        // til baribir so'rov parametridan olinishi kerak.
        mockMvc.perform(get("/api/v1/admin/complaints").param("lang", "ru"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("error.auth.required"))
                .andExpect(jsonPath("$.message")
                        .value("Для доступа к этому ресурсу требуется вход в систему"));
    }

    @Test
    @DisplayName("Ruxsat yo'qligi haqidagi xabar tanlangan tilda qaytadi")
    void accessDeniedMessageIsTranslated() throws Exception {
        String token = registerAndLogin("Til Sinovi", "til@test.uz", "TilSinov12345!");

        mockMvc.perform(authorized(get("/api/v1/admin/complaints").param("lang", "en"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("error.access.denied"))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    @DisplayName("Argumentli xabarda holat nomi ham o'sha tilda chiqadi")
    void messageArgumentsAreTranslatedToo() throws Exception {
        String trackingCode = submitSimpleComplaint();
        String token = adminToken();
        int complaintId = findComplaintId(token, trackingCode);

        // Murojaat NEW holatida, yana NEW ga o'tkazishga urinamiz
        mockMvc.perform(authorized(
                        json(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/v1/admin/complaints/{id}/status", complaintId)
                                .param("lang", "en"), """
                                {"status": "NEW"}
                                """),
                        token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.complaint.alreadyInStatus"))
                // Holat nomi ham inglizcha bo'lishi kerak, o'zbekcha emas
                .andExpect(jsonPath("$.message").value("The report is already in this status: New"));
    }

    // ------------------------------------------------------------- murojaat javoblari

    @Test
    @DisplayName("Murojaat qabul qilindi xabari yuborilgan tilda qaytadi")
    void complaintAcceptedMessageIsTranslated() throws Exception {
        mockMvc.perform(post("/api/v1/complaints")
                        .param("lang", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simpleComplaintJson("Обращение на русском языке для проверки")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusLabel").value("Новое"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.startsWith("Ваше обращение принято")));
    }

    @Test
    @DisplayName("Kuzatuv javobidagi nomlar so'rov tiliga qarab o'zgaradi")
    void trackingLabelsFollowRequestLanguage() throws Exception {
        String trackingCode = submitSimpleComplaint();

        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusLabel").value("New"))
                .andExpect(jsonPath("$.categoryLabel").value("Other"));

        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode).param("lang", "uz-cyrl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusLabel").value("Янги"))
                .andExpect(jsonPath("$.categoryLabel").value("Бошқа ҳолат"));
    }

    @Test
    @DisplayName("Fakulteti yo'q murojaat reyestrda tarjima qilingan izoh bilan chiqadi")
    void registerShowsTranslatedFallbackFaculty() throws Exception {
        String trackingCode = submitSimpleComplaint();

        mockMvc.perform(get("/api/v1/complaints/register")
                        .param("code", trackingCode)
                        .param("lang", "ru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].facultyName").value("По всему университету"));
    }

    // ------------------------------------------------------------- yordamchilar

    private String simpleComplaintJson(String title) {
        return """
                {
                  "title": "%s",
                  "description": "Til sinovi uchun yuborilgan murojaat matni. Kamida o'ttiz belgidan iborat bo'lishi kerak.",
                  "category": "OTHER",
                  "reporterType": "STUDENT",
                  "anonymous": true
                }
                """.formatted(title);
    }

    private String submitSimpleComplaint() throws Exception {
        String response = mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simpleComplaintJson("Til sinovi uchun murojaat sarlavhasi")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.trackingCode");
    }

    private int findComplaintId(String token, String trackingCode) throws Exception {
        String response = mockMvc.perform(authorized(
                        get("/api/v1/admin/complaints").param("query", trackingCode), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.content[0].id");
    }
}
