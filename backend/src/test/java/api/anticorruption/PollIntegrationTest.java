package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * So'rovnomalar: ko'p savolli anketa, mavsumiy muddat, ovoz berish,
 * takroriy ovozdan himoya va statistika.
 */
class PollIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Tizimga kirgan foydalanuvchi ovoz beradi, ikkinchi marta bera olmaydi")
    void authenticatedUserVotesOnce() throws Exception {
        String poll = createPoll(adminToken(), "Institutda korrupsiyaga qarshi ish qanday?");
        long questionId = id(poll, "$.questions[0].id");
        long optionId = id(poll, "$.questions[0].options[0].id");
        long otherId = id(poll, "$.questions[0].options[1].id");

        String voterToken = createUserAndLogin("Ovoz Beruvchi", "ovoz1@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(questionId, optionId)),
                        voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyVoted").value(true))
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].answeredCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].percentage").value(100.0));

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(questionId, otherId)),
                        voterToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Har bir savolning javoblari alohida sanaladi")
    void eachQuestionIsCountedSeparately() throws Exception {
        String poll = createTwoQuestionPoll(adminToken(), true);
        String voterToken = createUserAndLogin("Ikki Savol", "ovoz5@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [
                                  {"questionId": %d, "optionIds": [%d]},
                                  {"questionId": %d, "optionIds": [%d, %d]}
                                ]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[1].id"),
                                id(poll, "$.questions[1].id"),
                                id(poll, "$.questions[1].options[0].id"),
                                id(poll, "$.questions[1].options[2].id"))),
                        voterToken))
                .andExpect(status().isOk())
                // Bitta ishtirokchi - ikkala savol ham javoblangan
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questionCount").value(2))
                .andExpect(jsonPath("$.questions[0].answeredCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[1].voteCount").value(1))
                // Ko'p tanlovli savolda ikkita variant belgilangan, lekin javob bergan bitta
                .andExpect(jsonPath("$.questions[1].answeredCount").value(1))
                .andExpect(jsonPath("$.questions[1].options[0].voteCount").value(1))
                .andExpect(jsonPath("$.questions[1].options[1].voteCount").value(0))
                .andExpect(jsonPath("$.questions[1].options[2].voteCount").value(1));
    }

    @Test
    @DisplayName("Majburiy bo'lmagan savolni tashlab ketish mumkin")
    void optionalQuestionCanBeSkipped() throws Exception {
        String poll = createTwoQuestionPoll(adminToken(), false);
        String voterToken = createUserAndLogin("Yarim Javob", "ovoz6@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].answeredCount").value(1))
                .andExpect(jsonPath("$.questions[1].answeredCount").value(0));
    }

    @Test
    @DisplayName("Majburiy savol javobsiz qolsa ovoz qabul qilinmaydi")
    void requiredQuestionMustBeAnswered() throws Exception {
        String poll = createTwoQuestionPoll(adminToken(), true);
        String voterToken = createUserAndLogin("Chala Javob", "ovoz7@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.questionRequired"));
    }

    @Test
    @DisplayName("Bir tanlovli savolda bir nechta variant tanlab bo'lmaydi")
    void singleChoiceQuestionRejectsMultipleOptions() throws Exception {
        String poll = createPoll(adminToken(), "Bir tanlovli savol sinovi uchun matn");
        String voterToken = createUserAndLogin("Ikki Tanlovchi", "ovoz2@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d, %d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"),
                                id(poll, "$.questions[0].options[1].id"))),
                        voterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.singleChoiceOnly"));
    }

    @Test
    @DisplayName("Boshqa so'rovnomaning varianti qabul qilinmaydi")
    void optionFromAnotherPollIsRejected() throws Exception {
        String token = adminToken();
        String first = createPoll(token, "Birinchi so'rovnoma savoli sinov uchun");
        String second = createPoll(token, "Ikkinchi so'rovnoma savoli sinov uchun");

        String voterToken = createUserAndLogin("Chalkash Ovoz", "ovoz3@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(first, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(first, "$.questions[0].id"),
                                id(second, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.unknownOption"));
    }

    @Test
    @DisplayName("Yopilgan so'rovnomada ovoz berib bo'lmaydi")
    void closedPollRejectsVotes() throws Exception {
        String token = adminToken();
        String poll = createPoll(token, "Yopiladigan so'rovnoma savoli sinov uchun");

        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/active", id(poll, "$.id")).param("active", "false"),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        String voterToken = createUserAndLogin("Kech Qolgan", "ovoz4@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.closed"));
    }

    // ------------------------------------------------------------- mavsumiylik

    @Test
    @DisplayName("Boshlanish sanasi kelmagan so'rovnoma ochiq ro'yxatda ko'rinmaydi")
    void scheduledPollIsHiddenUntilItStarts() throws Exception {
        String poll = createScheduledPoll(
                adminToken(),
                "Kelasi oy boshlanadigan so'rovnoma",
                Instant.now().plus(Duration.ofDays(7)),
                Instant.now().plus(Duration.ofDays(14)));

        mockMvc.perform(authorized(
                        get("/api/v1/admin/polls/{id}", id(poll, "$.id")), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.openForVoting").value(false));

        mockMvc.perform(get("/api/v1/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(id(poll, "$.id"))).isEmpty());
    }

    @Test
    @DisplayName("Muddati tugagan so'rovnoma ko'rinadi, lekin ovoz qabul qilmaydi")
    void endedPollStaysVisibleWithoutAcceptingVotes() throws Exception {
        String poll = createScheduledPoll(
                adminToken(),
                "Muddati tugagan so'rovnoma sinovi",
                Instant.now().minus(Duration.ofDays(14)),
                Instant.now().minus(Duration.ofDays(1)));

        long pollId = id(poll, "$.id");

        mockMvc.perform(get("/api/v1/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].status".formatted(pollId)).value("CLOSED"));

        String voterToken = createUserAndLogin("Kechikkan", "ovoz8@test.uz", "Ovoz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", pollId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.closed"));
    }

    // ------------------------------------------------------------- to'xtatish

    @Test
    @DisplayName("Qo'lda to'xtatilgan so'rovnoma saytda ko'rinmaydi, statistikasi esa qoladi")
    void stoppedPollIsHiddenButKeepsItsStatistics() throws Exception {
        String token = adminToken();
        String poll = createPoll(token, "To'xtatiladigan so'rovnoma sinovi");
        long pollId = id(poll, "$.id");

        String voterToken = createUserAndLogin("To'xtashdan Oldin", "ovoz13@test.uz", "Ovoz12345678!");
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", pollId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/stopped", pollId).param("stopped", "true"),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.stoppedAt").isNotEmpty())
                .andExpect(jsonPath("$.openForVoting").value(false));

        // Saytda ko'rinmaydi
        mockMvc.perform(get("/api/v1/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(pollId)).isEmpty());

        // Admin panelidagi statistika joyida
        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}/statistics", pollId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(1));

        // Ovoz ham qabul qilinmaydi
        String lateVoter = createUserAndLogin("Kech Kelgan", "ovoz14@test.uz", "Ovoz12345678!");
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", pollId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        lateVoter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.closed"));
    }

    @Test
    @DisplayName("To'xtatilgan so'rovnomani davom ettirish mumkin")
    void stoppedPollCanBeResumed() throws Exception {
        String token = adminToken();
        long pollId = id(createPoll(token, "Davom ettiriladigan so'rovnoma sinovi"), "$.id");

        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/stopped", pollId).param("stopped", "true"),
                        token))
                .andExpect(status().isOk());

        // Ikkinchi marta to'xtatib bo'lmaydi
        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/stopped", pollId).param("stopped", "true"),
                        token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.alreadyStopped"));

        mockMvc.perform(authorized(
                        patch("/api/v1/admin/polls/{id}/stopped", pollId).param("stopped", "false"),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.stoppedAt").doesNotExist());
    }

    // ------------------------------------------------------------- qayta o'tkazish

    @Test
    @DisplayName("Qayta o'tkazish yangi so'rovnoma ochadi, eskisining hisoboti tegilmaydi")
    void restartStartsAFreshRunAndLeavesThePreviousOneIntact() throws Exception {
        String token = adminToken();
        String poll = createPoll(token, "Har yili takrorlanadigan so'rovnoma");
        long firstId = id(poll, "$.id");

        String voterToken = createUserAndLogin("Birinchi Mavsum", "ovoz15@test.uz", "Ovoz12345678!");
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", firstId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isOk());

        String restarted = mockMvc.perform(authorized(
                        post("/api/v1/admin/polls/{id}/restart", firstId), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runNumber").value(2))
                .andExpect(jsonPath("$.previousPollId").value((int) firstId))
                .andExpect(jsonPath("$.status").value("OPEN"))
                // Yangi o'tkazish noldan boshlanadi
                .andExpect(jsonPath("$.voterCount").value(0))
                .andExpect(jsonPath("$.questions[0].answeredCount").value(0))
                .andExpect(jsonPath("$.questions[0].options.length()").value(3))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(0))
                .andReturn().getResponse().getContentAsString();

        long secondId = id(restarted, "$.id");

        // Eskisi to'xtatilgan, lekin hisoboti butun
        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}/statistics", firstId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.runNumber").value(1))
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(1));

        // Ilgari ovoz bergan odam yangi o'tkazishda yana ovoz bera oladi
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", secondId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(restarted, "$.questions[0].id"),
                                id(restarted, "$.questions[0].options[1].id"))),
                        voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[1].voteCount").value(1));

        // Yangi ovoz eskisiga qo'shilib ketmagan
        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}", firstId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[1].voteCount").value(0));

        // Saytda faqat yangi o'tkazish ko'rinadi
        mockMvc.perform(get("/api/v1/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(firstId)).isEmpty())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(secondId)).isNotEmpty());
    }

    @Test
    @DisplayName("Oldingi o'tkazish o'chirilsa yangisi joyida qoladi")
    void deletingAPreviousRunKeepsTheNewOne() throws Exception {
        String token = adminToken();
        long firstId = id(createPoll(token, "O'chiriladigan oldingi o'tkazish"), "$.id");

        long secondId = id(mockMvc.perform(authorized(
                                post("/api/v1/admin/polls/{id}/restart", firstId), token))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        mockMvc.perform(authorized(delete("/api/v1/admin/polls/{id}", firstId), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}", secondId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runNumber").value(2))
                .andExpect(jsonPath("$.previousPollId").doesNotExist());
    }

    // ------------------------------------------------------------- statistika

    @Test
    @DisplayName("Statistika savollar kesimida ishtirok darajasini ko'rsatadi")
    void statisticsReportParticipationPerQuestion() throws Exception {
        String token = adminToken();
        String poll = createTwoQuestionPoll(token, false);
        long pollId = id(poll, "$.id");

        String voterToken = createUserAndLogin("Statistik", "ovoz9@test.uz", "Ovoz12345678!");
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", pollId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}/statistics", pollId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questionCount").value(2))
                // Ikkita savoldan bittasi javoblangan
                .andExpect(jsonPath("$.completionRate").value(50.0))
                .andExpect(jsonPath("$.firstVoteAt").isNotEmpty())
                .andExpect(jsonPath("$.lastVoteAt").isNotEmpty())
                .andExpect(jsonPath("$.questions[0].options[0].percentage").value(100.0))
                .andExpect(jsonPath("$.questions[1].answeredCount").value(0));
    }

    @Test
    @DisplayName("Tahrirlashda id si yuborilgan variant ovozlarini saqlab qoladi")
    void updateKeepsVotesOfExistingOptions() throws Exception {
        String token = adminToken();
        String poll = createPoll(token, "Tahrirlanadigan so'rovnoma savoli sinov");
        long pollId = id(poll, "$.id");
        long questionId = id(poll, "$.questions[0].id");
        long keptOptionId = id(poll, "$.questions[0].options[0].id");

        String voterToken = createUserAndLogin("Tahrir Ovozi", "ovoz10@test.uz", "Ovoz12345678!");
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", pollId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(questionId, keptOptionId)),
                        voterToken))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(
                        json(put("/api/v1/admin/polls/{id}", pollId), """
                                {
                                  "title": "Tahrirlangan so'rovnoma sarlavhasi",
                                  "questions": [{
                                    "id": %d,
                                    "text": "Tahrirlangan savol matni",
                                    "options": [
                                      {"id": %d, "text": "Yaxshi"},
                                      {"text": "Yangi variant"}
                                    ]
                                  }]
                                }
                                """.formatted(questionId, keptOptionId)),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tahrirlangan so'rovnoma sarlavhasi"))
                .andExpect(jsonPath("$.questions[0].text").value("Tahrirlangan savol matni"))
                .andExpect(jsonPath("$.questions[0].answeredCount").value(1))
                .andExpect(jsonPath("$.questions[0].options.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[1].voteCount").value(0));
    }

    @Test
    @DisplayName("Ovoz berilgan variant o'chirilsa ovozlari ham o'chadi va hisob tiklanadi")
    void removingAnOptionRemovesItsVotes() throws Exception {
        String token = adminToken();
        String poll = createTwoQuestionPoll(token, false);
        long pollId = id(poll, "$.id");
        long firstQuestionId = id(poll, "$.questions[0].id");
        long secondQuestionId = id(poll, "$.questions[1].id");

        // Ishtirokchi ikkala savolga javob beradi
        String voterToken = createUserAndLogin("Ovozi Ochadi", "ovoz11@test.uz", "Ovoz12345678!");
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", pollId), """
                                {"answers": [
                                  {"questionId": %d, "optionIds": [%d]},
                                  {"questionId": %d, "optionIds": [%d]}
                                ]}
                                """.formatted(
                                firstQuestionId, id(poll, "$.questions[0].options[0].id"),
                                secondQuestionId, id(poll, "$.questions[1].options[0].id"))),
                        voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[1].answeredCount").value(1));

        // Ikkinchi savolning ovoz berilgan varianti ro'yxatdan chiqariladi
        mockMvc.perform(authorized(
                        json(put("/api/v1/admin/polls/{id}", pollId), """
                                {
                                  "title": "Variant olib tashlangan so'rovnoma",
                                  "questions": [
                                    {"id": %d, "text": "Korrupsiyaga duch kelganmisiz?",
                                     "options": [{"id": %d, "text": "Ha"}, {"id": %d, "text": "Yo'q"}]},
                                    {"id": %d, "text": "Qaysi yo'nalishlarni kuchaytirish kerak?",
                                     "multipleChoice": true, "required": false,
                                     "options": [{"id": %d, "text": "Stipendiya"}, {"id": %d, "text": "Yotoqxona"}]}
                                  ]
                                }
                                """.formatted(
                                firstQuestionId,
                                id(poll, "$.questions[0].options[0].id"),
                                id(poll, "$.questions[0].options[1].id"),
                                secondQuestionId,
                                id(poll, "$.questions[1].options[1].id"),
                                id(poll, "$.questions[1].options[2].id"))),
                        token))
                .andExpect(status().isOk())
                // Birinchi savol tegilmagan - ovozi joyida
                .andExpect(jsonPath("$.questions[0].answeredCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(1))
                // Ikkinchi savolning yagona ovozi o'chirilgan variantda edi
                .andExpect(jsonPath("$.questions[1].options.length()").value(2))
                .andExpect(jsonPath("$.questions[1].answeredCount").value(0))
                // Ishtirokchi birinchi savolga javob berganicha qolyapti
                .andExpect(jsonPath("$.voterCount").value(1));
    }

    @Test
    @DisplayName("Ovoz berilgan so'rovnoma ovozlari bilan birga o'chadi")
    void pollWithVotesCanBeDeleted() throws Exception {
        String token = adminToken();
        String poll = createPoll(token, "O'chiriladigan so'rovnoma sinovi uchun");
        long pollId = id(poll, "$.id");

        String voterToken = createUserAndLogin("O'chiruvchi", "ovoz12@test.uz", "Ovoz12345678!");
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", pollId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        voterToken))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(delete("/api/v1/admin/polls/{id}", pollId), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}", pollId), token))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------- tekshiruvlar

    @Test
    @DisplayName("Faol so'rovnomalar ro'yxati autentifikatsiyasiz ochiq")
    void activePollsArePublic() throws Exception {
        createPoll(adminToken(), "Ochiq ro'yxatda ko'rinadigan so'rovnoma savoli");

        mockMvc.perform(get("/api/v1/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").isNotEmpty())
                .andExpect(jsonPath("$[0].questions").isArray())
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].statusLabel").value("Ochiq"))
                .andExpect(jsonPath("$[0].openForVoting").value(true));
    }

    @Test
    @DisplayName("Bitta variantli savol ham qabul qilinadi")
    void singleOptionQuestionIsAccepted() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "Bitta variantli so'rovnoma sinovi",
                                  "questions": [{
                                    "text": "Bitta variantli savol",
                                    "options": [{"text": "Yagona variant"}]
                                  }]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[0].options.length()").value(1));
    }

    @Test
    @DisplayName("Variantsiz savol qabul qilinmaydi")
    void questionWithoutOptionsIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "Variantsiz so'rovnoma sinovi",
                                  "questions": [{"text": "Variantsiz savol", "options": []}]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").isNotEmpty());
    }

    @Test
    @DisplayName("Savolsiz so'rovnoma qabul qilinmaydi")
    void pollWithoutQuestionsIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {"title": "Savolsiz so'rovnoma sinovi", "questions": []}
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.questions").exists());
    }

    @Test
    @DisplayName("Tugash vaqti boshlanishdan oldin bo'lsa rad etiladi")
    void invalidTimeWindowIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "Noto'g'ri vaqt oynasi bilan so'rovnoma",
                                  "startsAt": "2026-06-01T00:00:00Z",
                                  "endsAt": "2026-05-01T00:00:00Z",
                                  "questions": [{
                                    "text": "Vaqt oynasi sinovi savoli",
                                    "options": [{"text": "Ha"}, {"text": "Yo'q"}]
                                  }]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.invalidWindow"));
    }

    // ------------------------------------------------------------- testlar (viktorina)

    @Test
    @DisplayName("Test ishlangunicha to'g'ri javob berkitiladi, yakunda natija bilan ochiladi")
    void quizRevealsAnswersOnlyAfterSubmitting() throws Exception {
        String quiz = createQuiz(adminToken());
        long quizId = id(quiz, "$.id");
        long questionId = id(quiz, "$.questions[0].id");
        long correctId = id(quiz, "$.questions[0].options[0].id");

        // Ochiq ro'yxatda javob berilmagunicha to'g'ri variant ko'rinmaydi
        mockMvc.perform(get("/api/v1/polls").param("type", "QUIZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("QUIZ"))
                .andExpect(jsonPath("$[0].typeLabel").value("Test"))
                .andExpect(jsonPath("$[0].questions[0].options[0].correct").doesNotExist());

        String voterToken = createUserAndLogin("Test Ishlovchi", "quiz1@test.uz", "Quiz12345678!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", quizId), """
                                {"answers": [
                                  {"questionId": %d, "optionIds": [%d]},
                                  {"questionId": %d, "optionIds": [%d]}
                                ]}
                                """.formatted(
                                // Birinchi savolga to'g'ri, ikkinchisiga noto'g'ri javob
                                questionId, correctId,
                                id(quiz, "$.questions[1].id"),
                                id(quiz, "$.questions[1].options[0].id"))),
                        voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizResult.questionCount").value(2))
                .andExpect(jsonPath("$.quizResult.correctCount").value(1))
                .andExpect(jsonPath("$.quizResult.percentage").value(50.0))
                .andExpect(jsonPath("$.quizResult.questions[0].correct").value(true))
                .andExpect(jsonPath("$.quizResult.questions[1].correct").value(false))
                // Javobdan keyin to'g'ri variantlar ochiladi
                .andExpect(jsonPath("$.questions[0].options[0].correct").value(true))
                .andExpect(jsonPath("$.questions[0].options[1].correct").value(false));

        // Ochiq sahifada test statistikasi ko'rinmaydi
        mockMvc.perform(get("/api/v1/polls/{id}", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(0))
                .andExpect(jsonPath("$.questions[0].answeredCount").value(0))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(0))
                // Boshqa tashrifchi uchun javoblar yana berkitilgan
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());

        // Admin panelida esa raqamlar ham, to'g'ri javoblar ham joyida
        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}", quizId), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].correct").value(true));
    }

    @Test
    @DisplayName("Test hisoboti savollar kesimida to'g'ri javob berganlarni sanaydi")
    void quizStatisticsCountsCorrectAnswersPerQuestion() throws Exception {
        String quiz = createQuiz(adminToken());
        long quizId = id(quiz, "$.id");

        // Birinchi ishtirokchi: ikkala savolga ham to'g'ri javob
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", quizId), """
                                {"answers": [
                                  {"questionId": %d, "optionIds": [%d]},
                                  {"questionId": %d, "optionIds": [%d]}
                                ]}
                                """.formatted(
                                id(quiz, "$.questions[0].id"),
                                id(quiz, "$.questions[0].options[0].id"),
                                id(quiz, "$.questions[1].id"),
                                id(quiz, "$.questions[1].options[1].id"))),
                        createUserAndLogin("Birinchi Ishtirokchi", "quiz3@test.uz", "Quiz12345678!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizResult.correctCount").value(2));

        // Ikkinchi ishtirokchi: faqat ikkinchi savolga to'g'ri javob
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", quizId), """
                                {"answers": [
                                  {"questionId": %d, "optionIds": [%d]},
                                  {"questionId": %d, "optionIds": [%d]}
                                ]}
                                """.formatted(
                                id(quiz, "$.questions[0].id"),
                                id(quiz, "$.questions[0].options[1].id"),
                                id(quiz, "$.questions[1].id"),
                                id(quiz, "$.questions[1].options[1].id"))),
                        createUserAndLogin("Ikkinchi Ishtirokchi", "quiz4@test.uz", "Quiz12345678!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizResult.correctCount").value(1));

        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}/statistics", quizId), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quiz.participants").value(2))
                // Ikki ishtirokchi jami uchta savolga to'g'ri javob berdi
                .andExpect(jsonPath("$.quiz.averageCorrect").value(1.5))
                .andExpect(jsonPath("$.quiz.averagePercentage").value(75.0))
                .andExpect(jsonPath("$.quiz.questions[0].correctCount").value(1))
                .andExpect(jsonPath("$.quiz.questions[0].correctRate").value(50.0))
                .andExpect(jsonPath("$.quiz.questions[1].correctCount").value(2))
                .andExpect(jsonPath("$.quiz.questions[1].correctRate").value(100.0));
    }

    @Test
    @DisplayName("So'rovnomada natijalar ochiq qoladi")
    void surveyResultsStayPublic() throws Exception {
        String poll = createPoll(adminToken(), "Natijasi ochiq so'rovnoma sinovi");

        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", id(poll, "$.id")), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(
                                id(poll, "$.questions[0].id"),
                                id(poll, "$.questions[0].options[0].id"))),
                        createUserAndLogin("Ochiq Natija", "survey9@test.uz", "Ovoz12345678!")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/polls/{id}", id(poll, "$.id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].voteCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].percentage").value(100.0));
    }

    @Test
    @DisplayName("Ko'p tanlovli test savolida barcha to'g'ri javoblar belgilanishi kerak")
    void multipleChoiceQuizQuestionNeedsEveryCorrectOption() throws Exception {
        String quiz = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "Ko'p javobli test sinovi",
                                  "type": "QUIZ",
                                  "questions": [{
                                    "text": "Qaysilari korrupsiya belgisi hisoblanadi?",
                                    "multipleChoice": true,
                                    "options": [
                                      {"text": "Pora so'rash", "correct": true},
                                      {"text": "Qarindoshni imtiyozli o'tkazish", "correct": true},
                                      {"text": "Navbatda kutish"}
                                    ]
                                  }]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long quizId = id(quiz, "$.id");
        long questionId = id(quiz, "$.questions[0].id");
        long first = id(quiz, "$.questions[0].options[0].id");

        // Faqat bitta to'g'ri variant belgilangan - savol to'liq to'g'ri emas
        mockMvc.perform(authorized(
                        json(post("/api/v1/polls/{id}/vote", quizId), """
                                {"answers": [{"questionId": %d, "optionIds": [%d]}]}
                                """.formatted(questionId, first)),
                        createUserAndLogin("Yarim Javob", "quiz2@test.uz", "Quiz12345678!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizResult.correctCount").value(0))
                .andExpect(jsonPath("$.quizResult.questions[0].correctOptionIds.length()").value(2));
    }

    @Test
    @DisplayName("To'g'ri javobi belgilanmagan test qabul qilinmaydi")
    void quizQuestionWithoutCorrectOptionIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "To'g'ri javobsiz test sinovi",
                                  "type": "QUIZ",
                                  "questions": [{
                                    "text": "To'g'ri javobi belgilanmagan savol",
                                    "options": [{"text": "Birinchi"}, {"text": "Ikkinchi"}]
                                  }]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.quizNoCorrect"));
    }

    @Test
    @DisplayName("So'rovnomalar ro'yxatiga testlar aralashmaydi")
    void surveysAndQuizzesAreListedSeparately() throws Exception {
        createPoll(adminToken(), "Turlarni ajratish uchun so'rovnoma sinovi");
        createQuiz(adminToken());

        mockMvc.perform(get("/api/v1/polls").param("type", "SURVEY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'QUIZ')]").isEmpty())
                .andExpect(jsonPath("$[?(@.type == 'SURVEY')]").isNotEmpty());

        mockMvc.perform(get("/api/v1/polls").param("type", "QUIZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'SURVEY')]").isEmpty())
                .andExpect(jsonPath("$[?(@.type == 'QUIZ')]").isNotEmpty());
    }

    // ------------------------------------------------------------- yordamchilar

    /** Ikkita savolli test: har birida bitta to'g'ri javob. */
    private String createQuiz(String token) throws Exception {
        return mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "Korrupsiyaga qarshi kurash bo'yicha test",
                                  "type": "QUIZ",
                                  "questions": [
                                    {
                                      "text": "Pora berish qanday javobgarlikka olib keladi?",
                                      "options": [
                                        {"text": "Jinoiy javobgarlik", "correct": true},
                                        {"text": "Hech qanday javobgarlik yo'q"}
                                      ]
                                    },
                                    {
                                      "text": "Korrupsiya haqida qayerga xabar berish mumkin?",
                                      "options": [
                                        {"text": "Hech qayerga"},
                                        {"text": "Ishonch telefoniga", "correct": true}
                                      ]
                                    }
                                  ]
                                }
                                """),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("QUIZ"))
                .andReturn().getResponse().getContentAsString();
    }

    /** Bitta savolli, uchta variantli oddiy so'rovnoma. */
    private String createPoll(String token, String title) throws Exception {
        return mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "%s",
                                  "questions": [{
                                    "text": "Institutda korrupsiyaga qarshi ishni baholang",
                                    "options": [
                                      {"text": "Yaxshi"},
                                      {"text": "Qoniqarli"},
                                      {"text": "Yomon"}
                                    ]
                                  }]
                                }
                                """.formatted(title)),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[0].options.length()").value(3))
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * Ikkita savolli so'rovnoma: birinchisi bir tanlovli, ikkinchisi ko'p tanlovli.
     *
     * @param secondRequired ikkinchi savolga javob berish majburiymi
     */
    private String createTwoQuestionPoll(String token, boolean secondRequired) throws Exception {
        return mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "Ikki savolli anketa sinovi",
                                  "questions": [
                                    {
                                      "text": "Korrupsiyaga duch kelganmisiz?",
                                      "options": [{"text": "Ha"}, {"text": "Yo'q"}]
                                    },
                                    {
                                      "text": "Qaysi yo'nalishlarni kuchaytirish kerak?",
                                      "multipleChoice": true,
                                      "required": %s,
                                      "options": [
                                        {"text": "Imtihonlar"},
                                        {"text": "Stipendiya"},
                                        {"text": "Yotoqxona"}
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(secondRequired)),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionCount").value(2))
                .andReturn().getResponse().getContentAsString();
    }

    /** Muddati belgilangan so'rovnoma. */
    private String createScheduledPoll(String token, String title, Instant startsAt, Instant endsAt)
            throws Exception {

        return mockMvc.perform(authorized(
                        json(post("/api/v1/admin/polls"), """
                                {
                                  "title": "%s",
                                  "startsAt": "%s",
                                  "endsAt": "%s",
                                  "questions": [{
                                    "text": "Mavsumiy so'rovnoma savoli",
                                    "options": [{"text": "Ha"}, {"text": "Yo'q"}]
                                  }]
                                }
                                """.formatted(title, startsAt, endsAt)),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }
}
