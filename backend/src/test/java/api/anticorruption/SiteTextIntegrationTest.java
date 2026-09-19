package api.anticorruption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bosh sahifa matnlari: administrator o'zgartiradi, sayt kirishsiz o'qiydi,
 * bo'sh qoldirilgan matn asl holiga qaytadi.
 *
 * <p>Sinovlar bitta bazani bo'lishadi, shuning uchun har biri to'liq holatni
 * o'zi yozadi va o'zidan keyin hech narsa qoldirmaydi.
 */
class SiteTextIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Administrator matnni o'zgartiradi, sayt uni kirishsiz o'qiydi, bo'shi asl holiga qaytadi")
    void adminChangesTextsAndEmptyOnesFallBack() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(json(put("/api/v1/admin/site-texts"), """
                        {"texts": [
                          {"key": "home.heroTitle", "language": "uz", "value": "  Korrupsiyaga yo'l yo'q  "},
                          {"key": "home.heroTitle", "language": "ru", "value": "Коррупции - нет"},
                          {"key": "site.institute", "language": "uz", "value": "ADTI"},
                          {"key": "home.heroText", "language": "en", "value": "   "}
                        ]}
                        """), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uz['home.heroTitle']").value("Korrupsiyaga yo'l yo'q"))
                // Bo'sh matn saqlanmaydi - sayt asl matnni ko'rsatadi
                .andExpect(jsonPath("$.en['home.heroText']").doesNotExist());

        mockMvc.perform(get("/api/v1/site-texts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uz['home.heroTitle']").value("Korrupsiyaga yo'l yo'q"))
                .andExpect(jsonPath("$.uz['site.institute']").value("ADTI"))
                .andExpect(jsonPath("$.ru['home.heroTitle']").value("Коррупции - нет"))
                .andExpect(jsonPath("$['uz-cyrl']").isMap());

        // Ro'yxatda qolmagan matn o'chadi, qolgani yangilanadi
        mockMvc.perform(authorized(json(put("/api/v1/admin/site-texts"), """
                        {"texts": [
                          {"key": "home.heroTitle", "language": "uz", "value": "Yangi sarlavha"}
                        ]}
                        """), token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/site-texts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uz['home.heroTitle']").value("Yangi sarlavha"))
                .andExpect(jsonPath("$.uz['site.institute']").doesNotExist())
                .andExpect(jsonPath("$.ru['home.heroTitle']").doesNotExist());

        // Hammasini asl holiga qaytarish
        mockMvc.perform(authorized(json(put("/api/v1/admin/site-texts"), """
                        {"texts": []}
                        """), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uz['home.heroTitle']").doesNotExist());
    }

    @Test
    @DisplayName("Faqat bosh sahifa matnlari va ma'lum tillar qabul qilinadi")
    void onlyHomePageKeysAndKnownLanguagesAreAccepted() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(json(put("/api/v1/admin/site-texts"), """
                        {"texts": [{"key": "errors.network", "language": "uz", "value": "Buzilgan xabar"}]}
                        """), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.siteText.unknownKey"));

        mockMvc.perform(authorized(json(put("/api/v1/admin/site-texts"), """
                        {"texts": [{"key": "home.heroTitle", "language": "de", "value": "Titel"}]}
                        """), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.siteText.unknownLanguage"));

        mockMvc.perform(authorized(json(put("/api/v1/admin/site-texts"), """
                        {"texts": [{"key": "home.heroTitle", "language": "uz", "value": "%s"}]}
                        """.formatted("a".repeat(1001))), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").isNotEmpty());
    }

    @Test
    @DisplayName("Matnlarni tizimga kirmagan yoki oddiy foydalanuvchi o'zgartira olmaydi")
    void onlyStaffChangesTexts() throws Exception {
        String body = """
                {"texts": [{"key": "home.heroTitle", "language": "uz", "value": "Begona matn"}]}
                """;

        mockMvc.perform(anonymous(json(put("/api/v1/admin/site-texts"), body)))
                .andExpect(status().isUnauthorized());

        String citizen = createUserAndLogin("Oddiy Fuqaro", "matn.fuqaro@test.uz", "Fuqaro12345!");
        mockMvc.perform(authorized(json(put("/api/v1/admin/site-texts"), body), citizen))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/site-texts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uz['home.heroTitle']").doesNotExist());
    }
}
