package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * So'rovnoma va test guruhlari.
 *
 * <p>Guruh - bitta o'tkazish: "Korrupsiyaga qarshi kurash oyligi 2026" da bir
 * guruh test va bir guruh so'rovnoma o'tkaziladi, keyingi yilgisi esa alohida
 * guruhga tushadi. Shuning uchun guruh majburiy va to'ldirilgan guruhni
 * o'chirib bo'lmaydi.
 */
class PollGroupIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("So'rovnoma guruhga biriktiriladi va guruh nomi bilan qaytadi")
    void pollIsAssignedToGroup() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Oylik 2026 - anketalar", "SURVEY");

        String poll = createPoll(token, "Kuzgi anketa", groupId);

        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}", id(poll, "$.id")), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId))
                .andExpect(jsonPath("$.groupName").value("Oylik 2026 - anketalar"));

        // Guruhdagi yozuvlar soni ro'yxatda ko'rsatiladi.
        mockMvc.perform(authorized(get("/api/v1/admin/poll-groups?type=SURVEY"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].pollCount".formatted(groupId)).value(1));
    }

    @Test
    @DisplayName("Guruhsiz so'rovnoma yaratib bo'lmaydi")
    void groupIsRequired() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "Guruhsiz qolgan anketa",
                                  "questions": [{
                                    "text": "Institutda korrupsiyaga qarshi ishni baholang",
                                    "options": [{"text": "Yaxshi"}, {"text": "Yomon"}]
                                  }]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.groupId").exists());
    }

    @Test
    @DisplayName("Keyingi yilgi o'tkazish boshqa guruhga ko'chiriladi")
    void pollIsMovedToTheNextYearsGroup() throws Exception {
        String token = adminToken();
        long thisYear = createGroup(token, "Oylik 2026 - ko'chirish", "SURVEY");
        long nextYear = createGroup(token, "Oylik 2027 - ko'chirish", "SURVEY");

        String poll = createPoll(token, "Ko'chiriladigan anketa", thisYear);

        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/group?groupId={groupId}",
                                id(poll, "$.id"), nextYear),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(nextYear))
                .andExpect(jsonPath("$.groupName").value("Oylik 2027 - ko'chirish"));
    }

    @Test
    @DisplayName("To'ldirilgan guruhni o'chirib bo'lmaydi, bo'shini bo'ladi")
    void onlyEmptyGroupCanBeDeleted() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "O'chiriladigan guruh", "SURVEY");
        String poll = createPoll(token, "Guruhni band qilib turadigan anketa", groupId);

        mockMvc.perform(authorized(delete("/api/v1/admin/poll-groups/{id}", groupId), token))
                .andExpect(status().isConflict());

        // So'rovnoma o'chirilgach guruh bo'shaydi va o'chadi.
        mockMvc.perform(authorized(delete("/api/v1/admin/polls/{id}", id(poll, "$.id")), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(delete("/api/v1/admin/poll-groups/{id}", groupId), token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Bir turdagi guruhlar orasida nom takrorlanmaydi")
    void groupNameIsUniquePerType() throws Exception {
        String token = adminToken();
        createGroup(token, "Takrorlanadigan nom", "SURVEY");

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/poll-groups"), """
                                {"name": "Takrorlanadigan nom", "type": "SURVEY"}
                                """),
                        token))
                .andExpect(status().isConflict());

        // Boshqa turdagi guruhga o'sha nom to'g'ri keladi: ular alohida
        // sahifada va bir-biriga xalaqit bermaydi.
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/poll-groups"), """
                                {"name": "Takrorlanadigan nom", "type": "QUIZ"}
                                """),
                        token))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("So'rovnomani test guruhiga qo'yib bo'lmaydi")
    void groupTypeMustMatchPollType() throws Exception {
        String token = adminToken();
        long surveyGroupId = createGroup(token, "Anketalar guruhi", "SURVEY");
        long quizGroupId = createGroup(token, "Testlar guruhi", "QUIZ");
        String poll = createPoll(token, "Turi mos kelmaydigan anketa", surveyGroupId);

        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/group?groupId={groupId}",
                                id(poll, "$.id"), quizGroupId),
                        token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Guruhlar ro'yxati turi bo'yicha ajratiladi")
    void groupsAreFilteredByType() throws Exception {
        String token = adminToken();
        createGroup(token, "Faqat anketalar uchun", "SURVEY");
        createGroup(token, "Faqat viktorinalar uchun", "QUIZ");

        mockMvc.perform(authorized(get("/api/v1/admin/poll-groups?type=QUIZ"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Faqat viktorinalar uchun')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Faqat anketalar uchun')]").doesNotExist());
    }

    @Test
    @DisplayName("Qayta o'tkazishda yangi so'rovnoma eski guruhda qoladi")
    void restartKeepsTheGroup() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Qayta o'tkaziladigan guruh", "SURVEY");
        String poll = createPoll(token, "Har semestrdagi anketa", groupId);

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls/{id}/restart", id(poll, "$.id")), "{}"),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runNumber").value(2))
                .andExpect(jsonPath("$.groupId").value(groupId));
    }

    // ---------------------------------------------------------------- yordamchi

    private long createGroup(String token, String name, String type) throws Exception {
        String body = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/poll-groups"), """
                                {"name": "%s", "type": "%s"}
                                """.formatted(name, type)),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.pollCount").value(0))
                .andReturn().getResponse().getContentAsString();

        return id(body, "$.id");
    }

    private String createPoll(String token, String title, long groupId) throws Exception {
        return mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "%s",
                                  "groupId": %d,
                                  "questions": [{
                                    "text": "Institutda korrupsiyaga qarshi ishni baholang",
                                    "options": [{"text": "Yaxshi"}, {"text": "Yomon"}]
                                  }]
                                }
                                """.formatted(title, groupId)),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }
}
