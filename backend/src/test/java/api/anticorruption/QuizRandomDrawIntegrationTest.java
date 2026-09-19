package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Savollari tasodifiy tanlanadigan test: administrator test guruhini yaratayotganda
 * "har biriga 50 ta savol" deb belgilaydi va guruhdagi testga 200 ta savol kiritadi -
 * har bir ishtirokchiga shu bazadan takrorlanmaydigan 50 ta savol tushadi.
 */
class QuizRandomDrawIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Har bir ishtirokchiga bazadan takrorlanmaydigan to'plam tushadi, qayta boshlasa o'zgarmaydi")
    void eachParticipantGetsDistinctQuestionsThatStayTheSame() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Tasodifiy - to'plam", 10);
        String quiz = createQuiz(token, groupId, "Tasodifiy to'plam sinovi", 30);
        long quizId = id(quiz, "$.id");
        List<Long> bank = ids(quiz, "$.questions[*].id");

        String voter = createUserAndLogin("Tasodifiy Birinchi", "draw1@test.uz", "Draw12345678!");

        String started = mockMvc.perform(authorized(post("/api/v1/polls/{id}/start", quizId), voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionCount").value(30))
                .andExpect(jsonPath("$.questionsPerAttempt").value(10))
                .andExpect(jsonPath("$.questions.length()").value(10))
                .andExpect(jsonPath("$.attemptToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        List<Long> drawn = ids(started, "$.questions[*].id");
        // Bitta savol ikki marta tushmaydi va hammasi shu testning bazasidan
        assertThat(new HashSet<>(drawn)).hasSize(10);
        assertThat(bank).containsAll(drawn);
        // To'g'ri javoblar test ishlanmaguncha ochilmaydi
        assertThat(JsonPath.<List<Object>>read(started, "$.questions[*].options[*].correct"))
                .containsOnlyNulls();

        // Qayta boshlasa (sahifani yangilasa) o'sha savollar o'sha tartibda qaytadi
        String again = mockMvc.perform(authorized(post("/api/v1/polls/{id}/start", quizId), voter))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(ids(again, "$.questions[*].id")).isEqualTo(drawn);
        assertThat(JsonPath.<String>read(again, "$.attemptToken"))
                .isEqualTo(JsonPath.<String>read(started, "$.attemptToken"));

        // Boshqa ishtirokchi o'z to'plamini oladi
        String other = createUserAndLogin("Tasodifiy Ikkinchi", "draw2@test.uz", "Draw12345678!");
        String otherStarted = mockMvc.perform(authorized(post("/api/v1/polls/{id}/start", quizId), other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(10))
                .andReturn().getResponse().getContentAsString();

        assertThat(ids(otherStarted, "$.questions[*].id")).isNotEqualTo(drawn);
    }

    @Test
    @DisplayName("Javob faqat tushgan savollarga qabul qilinadi va natija ular sonidan chiqadi")
    void answersAreCheckedAndScoredAgainstTheDrawnQuestions() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Tasodifiy - baholash", 5);
        String quiz = createQuiz(token, groupId, "Tasodifiy baholash sinovi", 12);
        long quizId = id(quiz, "$.id");

        String voter = createUserAndLogin("Baholanuvchi", "draw3@test.uz", "Draw12345678!");
        String started = start(quizId, voter);
        String attemptToken = JsonPath.read(started, "$.attemptToken");
        List<Long> drawn = ids(started, "$.questions[*].id");

        Map<Long, Long> correct = correctOptions(token, quizId);

        // Bazada bor, lekin ishtirokchiga tushmagan savol
        long notDrawn = ids(quiz, "$.questions[*].id").stream()
                .filter(questionId -> !drawn.contains(questionId))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(authorized(json(post("/api/v1/polls/{id}/vote", quizId),
                        voteBody(attemptToken, Map.of(notDrawn, correct.get(notDrawn)))), voter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.unknownQuestion"));

        // Tushgan savollardan biri javobsiz qolsa ham qabul qilinmaydi
        Map<Long, Long> partial = new LinkedHashMap<>();
        drawn.subList(0, 4).forEach(questionId -> partial.put(questionId, correct.get(questionId)));

        mockMvc.perform(authorized(json(post("/api/v1/polls/{id}/vote", quizId),
                        voteBody(attemptToken, partial)), voter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.questionRequired"));

        // Hammasiga to'g'ri javob: 12 tadan emas, tushgan 5 tadan baholanadi
        Map<Long, Long> all = new LinkedHashMap<>();
        drawn.forEach(questionId -> all.put(questionId, correct.get(questionId)));

        mockMvc.perform(authorized(json(post("/api/v1/polls/{id}/vote", quizId),
                        voteBody(attemptToken, all)), voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyVoted").value(true))
                .andExpect(jsonPath("$.quizResult.questionCount").value(5))
                .andExpect(jsonPath("$.quizResult.correctCount").value(5))
                .andExpect(jsonPath("$.quizResult.percentage").value(100.0))
                .andExpect(jsonPath("$.questions.length()").value(5))
                .andExpect(jsonPath("$.questions[0].options[0].correct").value(true));

        mockMvc.perform(authorized(json(post("/api/v1/polls/{id}/vote", quizId),
                        voteBody(attemptToken, all)), voter))
                .andExpect(status().isConflict());

        mockMvc.perform(authorized(post("/api/v1/polls/{id}/start", quizId), voter))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("error.poll.alreadyVoted"));
    }

    @Test
    @DisplayName("Ishlangan testda ham butun savollar bazasi saytga chiqmaydi")
    void solvedQuizDoesNotRevealTheWholeBank() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Tasodifiy - yopiq baza", 3);
        String quiz = createQuiz(token, groupId, "Bazasi yopiq test sinovi", 8);
        long quizId = id(quiz, "$.id");

        // Boshlanmagan testda savollar ko'rinmaydi, faqat nechtasi berilishi
        mockMvc.perform(get("/api/v1/polls/{id}", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionCount").value(8))
                .andExpect(jsonPath("$.questionsPerAttempt").value(3))
                .andExpect(jsonPath("$.questions.length()").value(0));

        String voter = createUserAndLogin("Yopiq Baza", "draw4@test.uz", "Draw12345678!");
        String started = start(quizId, voter);
        List<Long> drawn = ids(started, "$.questions[*].id");
        Map<Long, Long> correct = correctOptions(token, quizId);

        Map<Long, Long> all = new LinkedHashMap<>();
        drawn.forEach(questionId -> all.put(questionId, correct.get(questionId)));
        mockMvc.perform(authorized(json(post("/api/v1/polls/{id}/vote", quizId),
                        voteBody(JsonPath.read(started, "$.attemptToken"), all)), voter))
                .andExpect(status().isOk());

        // Ishlagan odam faqat o'ziga tushgan savollarni javoblari bilan ko'radi
        String detail = mockMvc.perform(authorized(get("/api/v1/polls/{id}", quizId), voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyVoted").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(ids(detail, "$.questions[*].id")).isEqualTo(drawn);

        // Ro'yxatda esa savollar umuman berilmaydi
        mockMvc.perform(authorized(get("/api/v1/polls").param("type", "QUIZ"), voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].alreadyVoted".formatted(quizId)).value(true))
                .andExpect(jsonPath("$[?(@.id == %d)].questions[*]".formatted(quizId)).isEmpty());
    }

    @Test
    @DisplayName("Anonim ishtirokchining manzili test davomida o'zgarsa ham javobi o'z to'plamiga yoziladi")
    void anonymousParticipantKeepsTheirDrawWhenTheirAddressChanges() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Tasodifiy - anonim", 2);
        long quizId = id(createQuiz(token, groupId, "Anonim ishtirokchi sinovi", 6), "$.id");

        String started = mockMvc.perform(post("/api/v1/polls/{id}/start", quizId)
                        .header("User-Agent", "Birinchi brauzer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        List<Long> drawn = ids(started, "$.questions[*].id");
        Map<Long, Long> correct = correctOptions(token, quizId);
        Map<Long, Long> all = new LinkedHashMap<>();
        drawn.forEach(questionId -> all.put(questionId, correct.get(questionId)));

        // Javob boshqa "manzil"dan keladi - belgi to'plamni topadi
        mockMvc.perform(json(post("/api/v1/polls/{id}/vote", quizId)
                                .header("User-Agent", "Ikkinchi brauzer"),
                        voteBody(JsonPath.read(started, "$.attemptToken"), all)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizResult.correctCount").value(2));

        // Ovoz to'plam egasi nomidan yozilgan: u qayta boshlay olmaydi
        mockMvc.perform(post("/api/v1/polls/{id}/start", quizId)
                        .header("User-Agent", "Birinchi brauzer"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/polls/{id}", quizId)
                        .header("User-Agent", "Birinchi brauzer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyVoted").value(true));

        // Soxta belgi bilan javob qabul qilinmaydi
        mockMvc.perform(json(post("/api/v1/polls/{id}/vote", quizId)
                                .header("User-Agent", "Uchinchi brauzer"),
                        voteBody("mavjud-bo'lmagan-belgi", all)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.attemptNotFound"));
    }

    @Test
    @DisplayName("Savollar soni guruhda belgilanadi va guruhdagi testlarga tarqaladi")
    void drawSizeIsSetOnTheGroup() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(json(post("/api/v1/admin/poll-groups"), """
                        {"name": "Tasodifiy - nol", "type": "QUIZ", "questionsPerAttempt": 0}
                        """), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.questionsPerAttempt").exists());

        long groupId = createGroup(token, "Tasodifiy - guruh sozlamasi", 5);

        mockMvc.perform(authorized(get("/api/v1/admin/poll-groups").param("type", "QUIZ"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].questionsPerAttempt".formatted(groupId)).value(5));

        // Testda guruhda belgilangandan kam savol bo'lsa - bori, aralashtirilgan holda
        long quizId = id(createQuiz(token, groupId, "Savollari hali kam test", 3), "$.id");

        mockMvc.perform(authorized(post("/api/v1/polls/{id}/start", quizId),
                        createUserAndLogin("Kam Savol", "draw8@test.uz", "Draw12345678!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionsPerAttempt").value(3))
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.attemptToken").isNotEmpty());

        // Guruhda son olib tashlansa test barcha savollarni kiritilgan tartibda beradi
        mockMvc.perform(authorized(json(put("/api/v1/admin/poll-groups/{id}", groupId), """
                        {"name": "Tasodifiy - guruh sozlamasi", "questionsPerAttempt": null}
                        """), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionsPerAttempt").doesNotExist());

        mockMvc.perform(authorized(post("/api/v1/polls/{id}/start", quizId),
                        createUserAndLogin("Hamma Savol", "draw9@test.uz", "Draw12345678!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionsPerAttempt").doesNotExist())
                .andExpect(jsonPath("$.questions[0].text").value("Korrupsiya bo'yicha savol 1"))
                .andExpect(jsonPath("$.attemptToken").doesNotExist());
    }

    @Test
    @DisplayName("Bir xil savol ikki marta kiritilsa saqlanmaydi")
    void duplicateQuestionIsRejected() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Tasodifiy - takroriy savol", 2);

        // Katta-kichik harf, bo'shliq va variantlar tartibi farq qiladi - baribir o'sha savol
        mockMvc.perform(authorized(json(post("/api/v1/admin/polls"), """
                        {"groupId": %d,
                          "title": "Takroriy savolli test sinovi",
                          "type": "QUIZ",
                          "questions": [
                            {"text": "Pora berish jinoyatmi?",
                             "options": [{"text": "Ha", "correct": true}, {"text": "Yo'q"}]},
                            {"text": "  pora  berish JINOYATMI? ",
                             "options": [{"text": "Yo'q"}, {"text": "ha", "correct": true}]}
                          ]
                        }
                        """.formatted(groupId)), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.poll.duplicateQuestion"));

        // Matni bir xil, variantlari boshqa savol - takroriy emas
        mockMvc.perform(authorized(json(post("/api/v1/admin/polls"), """
                        {"groupId": %d,
                          "title": "Umumiy savolli test sinovi",
                          "type": "QUIZ",
                          "questions": [
                            {"text": "To'g'ri javobni belgilang",
                             "options": [{"text": "Pora - jinoyat", "correct": true}, {"text": "Pora - odat"}]},
                            {"text": "To'g'ri javobni belgilang",
                             "options": [{"text": "Ishonch telefoni bor", "correct": true}, {"text": "Xabar berib bo'lmaydi"}]}
                          ]
                        }
                        """.formatted(groupId)), token))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Hisobot har bir ishtirokchini o'ziga tushgan savollar sonidan baholaydi")
    void statisticsUseEachParticipantsOwnQuestionCount() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Tasodifiy - hisobot", 4);
        long quizId = id(createQuiz(token, groupId, "Tasodifiy test hisoboti", 12), "$.id");
        Map<Long, Long> correct = correctOptions(token, quizId);
        Map<Long, Long> wrong = wrongOptions(token, quizId);

        // Birinchisi hammasiga to'g'ri, ikkinchisi hammasiga noto'g'ri javob beradi
        submit(quizId, createUserAndLogin("A'lochi", "draw5@test.uz", "Draw12345678!"), correct);
        submit(quizId, createUserAndLogin("Qoloq", "draw6@test.uz", "Draw12345678!"), wrong);

        mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}/statistics", quizId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(2))
                .andExpect(jsonPath("$.questionCount").value(12))
                .andExpect(jsonPath("$.questionsPerAttempt").value(4))
                // Har biriga tushgan 4 ta savolning hammasi javoblangan
                .andExpect(jsonPath("$.completionRate").value(100.0))
                .andExpect(jsonPath("$.quiz.participants").value(2))
                .andExpect(jsonPath("$.quiz.averageCorrect").value(2.0))
                // 8 ta berilgan savoldan 4 tasi to'g'ri - 12 ta savolga bo'linmaydi
                .andExpect(jsonPath("$.quiz.averagePercentage").value(50.0));
    }

    @Test
    @DisplayName("Qayta o'tkazilgan test guruhning sozlamasini oladi, test esa to'plamlari bilan o'chadi")
    void restartKeepsTheDrawAndDeleteRemovesDraws() throws Exception {
        String token = adminToken();
        long groupId = createGroup(token, "Tasodifiy - qayta o'tkazish", 3);
        long quizId = id(createQuiz(token, groupId, "Qayta o'tkaziladigan tasodifiy test", 6), "$.id");

        start(quizId, createUserAndLogin("Boshlab Qo'ygan", "draw7@test.uz", "Draw12345678!"));

        mockMvc.perform(authorized(post("/api/v1/admin/polls/{id}/restart", quizId), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionsPerAttempt").value(3));

        mockMvc.perform(authorized(delete("/api/v1/admin/polls/{id}", quizId), token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("So'rovnoma guruhida savollar tasodifiy berilmaydi")
    void surveyGroupIgnoresTheDrawSize() throws Exception {
        String token = adminToken();
        long groupId = id(mockMvc.perform(authorized(json(post("/api/v1/admin/poll-groups"), """
                                {"name": "Tasodifiy - so'rovnomalar", "type": "SURVEY", "questionsPerAttempt": 1}
                                """), token))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.questionsPerAttempt").doesNotExist())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        String survey = mockMvc.perform(authorized(json(post("/api/v1/admin/polls"), """
                        {"groupId": %d,
                          "title": "Hamma savoli beriladigan so'rovnoma",
                          "questions": [
                            {"text": "Birinchi savol matni", "options": [{"text": "Ha"}, {"text": "Yo'q"}]},
                            {"text": "Ikkinchi savol matni", "options": [{"text": "Ha"}, {"text": "Yo'q"}]}
                          ]
                        }
                        """.formatted(groupId)), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionsPerAttempt").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/polls/{id}/start", id(survey, "$.id"))
                        .header("User-Agent", "So'rovnoma brauzeri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.attemptToken").doesNotExist());
    }

    // ------------------------------------------------------------- yordamchilar

    /** Test guruhi: har bir ishtirokchiga shuncha savol tasodifiy beriladi. */
    private long createGroup(String token, String name, Integer perAttempt) throws Exception {
        return id(mockMvc.perform(authorized(json(post("/api/v1/admin/poll-groups"), """
                                {"name": "%s", "type": "QUIZ", "questionsPerAttempt": %s}
                                """.formatted(name, perAttempt)), token))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.questionsPerAttempt").value(perAttempt))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");
    }

    /** Har bir savolda bitta to'g'ri va bitta noto'g'ri variantli test. */
    private String createQuiz(String token, long groupId, String title, int questionCount)
            throws Exception {

        String questions = IntStream.rangeClosed(1, questionCount)
                .mapToObj(number -> """
                        {"text": "Korrupsiya bo'yicha savol %d",
                         "options": [{"text": "To'g'ri javob %d", "correct": true}, {"text": "Noto'g'ri javob %d"}]}
                        """.formatted(number, number, number))
                .collect(Collectors.joining(","));

        return mockMvc.perform(authorized(json(post("/api/v1/admin/polls"), """
                        {"groupId": %d, "title": "%s", "type": "QUIZ", "questions": [%s]}
                        """.formatted(groupId, title, questions)), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionCount").value(questionCount))
                .andReturn().getResponse().getContentAsString();
    }

    private String start(long quizId, String voterToken) throws Exception {
        return mockMvc.perform(authorized(post("/api/v1/polls/{id}/start", quizId), voterToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Testni boshlaydi va tushgan har bir savolga berilgan jadvaldan javob yuboradi. */
    private void submit(long quizId, String voterToken, Map<Long, Long> optionByQuestion)
            throws Exception {

        String started = start(quizId, voterToken);
        Map<Long, Long> answers = new LinkedHashMap<>();
        ids(started, "$.questions[*].id")
                .forEach(questionId -> answers.put(questionId, optionByQuestion.get(questionId)));

        mockMvc.perform(authorized(json(post("/api/v1/polls/{id}/vote", quizId),
                        voteBody(JsonPath.read(started, "$.attemptToken"), answers)), voterToken))
                .andExpect(status().isOk());
    }

    private String voteBody(String attemptToken, Map<Long, Long> optionByQuestion) {
        String answers = optionByQuestion.entrySet().stream()
                .map(answer -> """
                        {"questionId": %d, "optionIds": [%d]}
                        """.formatted(answer.getKey(), answer.getValue()))
                .collect(Collectors.joining(","));

        return """
                {"attemptToken": "%s", "answers": [%s]}
                """.formatted(attemptToken, answers);
    }

    /** Savol id si -> to'g'ri variant id si, admin panelidan. */
    private Map<Long, Long> correctOptions(String token, long quizId) throws Exception {
        return optionsWhere(token, quizId, true);
    }

    private Map<Long, Long> wrongOptions(String token, long quizId) throws Exception {
        return optionsWhere(token, quizId, false);
    }

    private Map<Long, Long> optionsWhere(String token, long quizId, boolean correct) throws Exception {
        String detail = mockMvc.perform(authorized(get("/api/v1/admin/polls/{id}", quizId), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<Long, Long> options = new LinkedHashMap<>();
        for (long questionId : ids(detail, "$.questions[*].id")) {
            List<Number> matching = JsonPath.read(detail,
                    "$.questions[?(@.id == %d)].options[?(@.correct == %s)].id".formatted(questionId, correct));
            options.put(questionId, matching.get(0).longValue());
        }
        return options;
    }

    private List<Long> ids(String json, String path) {
        List<Number> values = JsonPath.read(json, path);
        return values.stream().map(Number::longValue).toList();
    }

    private long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }
}
