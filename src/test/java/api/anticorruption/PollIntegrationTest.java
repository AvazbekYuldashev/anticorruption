package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** So'rovnomalar: yaratish, ovoz berish va takroriy ovozdan himoya. */
class PollIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Tizimga kirgan foydalanuvchi ovoz beradi, ikkinchi marta bera olmaydi")
    void authenticatedUserVotesOnce() throws Exception {
        String adminToken = adminToken();
        PollIds poll = createPoll(adminToken, "Institutda korrupsiyaga qarshi ishlarni qanday baholaysiz?");

        String voterToken = registerAndLogin("Ovoz Beruvchi", "ovoz1@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", poll.pollId()), """
                                {"optionIds": [%d]}
                                """.formatted(poll.firstOptionId())),
                        voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyVoted").value(true))
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.options[0].voteCount").value(1))
                .andExpect(jsonPath("$.options[0].percentage").value(100.0));

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", poll.pollId()), """
                                {"optionIds": [%d]}
                                """.formatted(poll.secondOptionId())),
                        voterToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Bir tanlovli so'rovnomada bir nechta variant tanlab bo'lmaydi")
    void singleChoicePollRejectsMultipleOptions() throws Exception {
        String adminToken = adminToken();
        PollIds poll = createPoll(adminToken, "Bir tanlovli so'rovnoma sinovi uchun savol");

        String voterToken = registerAndLogin("Ikki Tanlovchi", "ovoz2@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", poll.pollId()), """
                                {"optionIds": [%d, %d]}
                                """.formatted(poll.firstOptionId(), poll.secondOptionId())),
                        voterToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Boshqa so'rovnomaning varianti qabul qilinmaydi")
    void optionFromAnotherPollIsRejected() throws Exception {
        String adminToken = adminToken();
        PollIds first = createPoll(adminToken, "Birinchi so'rovnoma savoli sinov uchun");
        PollIds second = createPoll(adminToken, "Ikkinchi so'rovnoma savoli sinov uchun");

        String voterToken = registerAndLogin("Chalkash Ovoz", "ovoz3@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", first.pollId()), """
                                {"optionIds": [%d]}
                                """.formatted(second.firstOptionId())),
                        voterToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Yopilgan so'rovnomada ovoz berib bo'lmaydi")
    void closedPollRejectsVotes() throws Exception {
        String adminToken = adminToken();
        PollIds poll = createPoll(adminToken, "Yopiladigan so'rovnoma savoli sinov uchun");

        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/active", poll.pollId()).param("active", "false"),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        String voterToken = registerAndLogin("Kech Qolgan", "ovoz4@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", poll.pollId()), """
                                {"optionIds": [%d]}
                                """.formatted(poll.firstOptionId())),
                        voterToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Faol so'rovnomalar ro'yxati autentifikatsiyasiz ochiq")
    void activePollsArePublic() throws Exception {
        createPoll(adminToken(), "Ochiq ro'yxatda ko'rinadigan so'rovnoma savoli");

        mockMvc.perform(get("/api/v1/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").isNotEmpty())
                .andExpect(jsonPath("$[0].options").isArray())
                .andExpect(jsonPath("$[0].openForVoting").value(true));
    }

    @Test
    @DisplayName("Kamida ikkita variant bo'lishi shart")
    void pollNeedsAtLeastTwoOptions() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "question": "Bitta variantli so'rovnoma savoli",
                                  "options": [{"text": "Yagona variant"}]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.options").exists());
    }

    @Test
    @DisplayName("Tugash vaqti boshlanishdan oldin bo'lsa rad etiladi")
    void invalidTimeWindowIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "question": "Noto'g'ri vaqt oynasi bilan so'rovnoma",
                                  "startsAt": "2026-06-01T00:00:00Z",
                                  "endsAt": "2026-05-01T00:00:00Z",
                                  "options": [{"text": "Ha"}, {"text": "Yo'q"}]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------- yordamchilar

    private PollIds createPoll(String token, String question) throws Exception {
        String response = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "question": "%s",
                                  "options": [
                                    {"text": "Yaxshi"},
                                    {"text": "Qoniqarli"},
                                    {"text": "Yomon"}
                                  ]
                                }
                                """.formatted(question)),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        return new PollIds(
                ((Number) JsonPath.read(response, "$.id")).longValue(),
                ((Number) JsonPath.read(response, "$.options[0].id")).longValue(),
                ((Number) JsonPath.read(response, "$.options[1].id")).longValue());
    }

    private record PollIds(long pollId, long firstOptionId, long secondOptionId) {
    }
}
