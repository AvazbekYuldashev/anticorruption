package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Murojaat bilan bog'liq asosiy stsenariylar: yuborish, kuzatish,
 * fayl biriktirish, xodim tomonidan ko'rib chiqish va ruxsatlar chegarasi.
 */
class ComplaintFlowIntegrationTest extends AbstractIntegrationTest {

    // ------------------------------------------------------------- murojaat yuborish

    @Test
    @DisplayName("Anonim murojaat tizimga kirmasdan yuboriladi va kuzatuv kodi bilan topiladi")
    void anonymousComplaintCanBeSubmittedAndTracked() throws Exception {
        String trackingCode = submitComplaint(complaintJson("Anonim murojaat sinovi uchun sarlavha", true));

        assertThat(trackingCode).startsWith("AC-");

        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value(trackingCode))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.statusLabel").value("Yangi"))
                // Ochiq javobda ichki ma'lumotlar bo'lmasligi kerak
                .andExpect(jsonPath("$.reporterEmail").doesNotExist())
                .andExpect(jsonPath("$.history[0].note").doesNotExist());
    }

    @Test
    @DisplayName("Kuzatuv kodi kichik harfda kiritilsa ham topiladi")
    void trackingCodeIsCaseInsensitive() throws Exception {
        String trackingCode = submitComplaint(complaintJson("Katta-kichik harf sinovi sarlavhasi", true));

        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value(trackingCode));
    }

    @Test
    @DisplayName("Mavjud bo'lmagan kuzatuv kodi uchun 404 qaytadi")
    void unknownTrackingCodeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/complaints/track/{code}", "AC-2026-XXXXXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Noto'g'ri ma'lumot yuborilsa maydonlar bo'yicha xatolik qaytadi")
    void invalidComplaintIsRejectedWithFieldErrors() throws Exception {
        String invalid = """
                {
                  "title": "Qisqa",
                  "description": "Juda qisqa matn",
                  "category": null,
                  "reporterType": null
                }
                """;

        mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.description").exists())
                .andExpect(jsonPath("$.fields.category").exists())
                .andExpect(jsonPath("$.fields.reporterType").exists());
    }

    @Test
    @DisplayName("anonymous maydoni yuborilmasa murojaat baribir qabul qilinadi")
    void anonymousFieldIsOptional() throws Exception {
        String withoutAnonymousField = """
                {
                  "title": "anonymous maydonisiz yuborilgan murojaat",
                  "description": "Bu murojaatda anonymous maydoni umuman yo'q. Shakl uni yubormasa ham qabul qilinishi kerak.",
                  "category": "OTHER",
                  "reporterType": "TEACHER"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withoutAnonymousField))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Alohida o'zgaruvchiga olinadi: JsonPath.read natijasi to'g'ridan-to'g'ri
        // varargs ga berilsa, Java uni Object[] deb chiqarib xato beradi.
        String trackingCode = JsonPath.read(response, "$.trackingCode");

        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @DisplayName("Buzilgan JSON 400 qaytaradi, 500 emas")
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ bu json emas "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Noto'g'ri enum qiymati 400 qaytaradi")
    void unknownEnumValueReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Mavjud bo'lmagan kategoriya sinovi",
                                  "description": "Bu murojaatda kategoriya qiymati ro'yxatda yo'q, shuning uchun rad etilishi kerak.",
                                  "category": "BUNDAY_TUR_YOQ",
                                  "reporterType": "STUDENT",
                                  "anonymous": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Talaba maydonlari o'qituvchi murojaatida saqlanmaydi")
    void studentFieldsAreIgnoredForNonStudents() throws Exception {
        String trackingCode = submitComplaint("""
                {
                  "title": "O'qituvchi yuborgan murojaat sarlavhasi",
                  "description": "Bu murojaatni o'qituvchi yubormoqda, lekin shaklda kurs va guruh ham to'ldirilgan.",
                  "category": "PROCUREMENT_FRAUD",
                  "reporterType": "TEACHER",
                  "courseYear": 3,
                  "groupName": "KI-21-01",
                  "studyForm": "FULL_TIME",
                  "anonymous": true
                }
                """);

        String token = adminToken();
        int id = findComplaintId(token, trackingCode);

        mockMvc.perform(authorized(get("/api/v1/admin/complaints/{id}", id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporterType").value("TEACHER"))
                .andExpect(jsonPath("$.courseYear").doesNotExist())
                .andExpect(jsonPath("$.groupName").doesNotExist());
    }

    // ------------------------------------------------------------- fayl biriktirish

    @Test
    @DisplayName("Kuzatuv kodi bilan dalil fayli biriktiriladi")
    void attachmentCanBeUploadedWithTrackingCode() throws Exception {
        String trackingCode = submitComplaint(complaintJson("Fayl biriktirish sinovi sarlavhasi", true));

        MockMultipartFile file = new MockMultipartFile(
                "file", "dalil.txt", "text/plain", "sinov matni".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/complaints/track/{code}/attachments", trackingCode).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalName").value("dalil.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"));

        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentCount").value(1));
    }

    @Test
    @DisplayName("Ruxsat etilmagan turdagi fayl rad etiladi")
    void unsupportedFileTypeIsRejected() throws Exception {
        String trackingCode = submitComplaint(complaintJson("Yaroqsiz fayl turi sinovi sarlavhasi", true));

        MockMultipartFile file = new MockMultipartFile(
                "file", "zararli.exe", "application/x-msdownload", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/complaints/track/{code}/attachments", trackingCode).file(file))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------- foydalanuvchi

    @Test
    @DisplayName("Foydalanuvchi ro'yxatdan o'tadi, kiradi va o'z murojaatini ko'radi")
    void registeredUserCanSubmitAndSeeOwnComplaints() throws Exception {
        String email = "talaba1@test.uz";
        String token = registerAndLogin("Alisher Karimov", email, "Talaba12345!");

        mockMvc.perform(authorized(get("/api/v1/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CITIZEN"));

        mockMvc.perform(authorized(
                        json(post("/api/v1/complaints"),
                                complaintJson("Ro'yxatdan o'tgan foydalanuvchi murojaati", false)),
                        token))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/v1/complaints/my"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].anonymous").value(false));
    }

    @Test
    @DisplayName("Tizimga kirgan foydalanuvchi anonim yuborsa, murojaat unga bog'lanmaydi")
    void anonymousSubmissionIsNotLinkedEvenWhenLoggedIn() throws Exception {
        String token = registerAndLogin("Anonim Foydalanuvchi", "anonim@test.uz", "Anonim12345!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/complaints"),
                                complaintJson("Kirgan holda anonim yuborilgan murojaat", true)),
                        token))
                .andExpect(status().isCreated());

        // Anonim murojaat "mening murojaatlarim" ro'yxatiga tushmaydi
        mockMvc.perform(authorized(get("/api/v1/complaints/my"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Bir xil email bilan ikki marta ro'yxatdan o'tib bo'lmaydi")
    void duplicateEmailIsRejected() throws Exception {
        String body = """
                {"fullName": "Takroriy Foydalanuvchi", "email": "takror@test.uz", "password": "Takror12345!"}
                """;

        mockMvc.perform(json(post("/api/v1/auth/register"), body))
                .andExpect(status().isCreated());

        mockMvc.perform(json(post("/api/v1/auth/register"), body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Noto'g'ri parol bilan kirish 401 qaytaradi")
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "%s", "password": "notogriparol"}
                        """.formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- xodimlar

    @Test
    @DisplayName("Xodim murojaat holatini bosqichma-bosqich o'zgartiradi")
    void staffCanMoveComplaintThroughStatuses() throws Exception {
        String trackingCode = submitComplaint(complaintJson("Xodim ko'rib chiqadigan murojaat sarlavhasi", true));
        String token = adminToken();
        int complaintId = findComplaintId(token, trackingCode);

        mockMvc.perform(authorized(
                        json(patch("/api/v1/admin/complaints/{id}/status", complaintId), """
                                {"status": "IN_REVIEW", "note": "Ichki izoh: tekshiruv boshlandi"}
                                """),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                // Ko'rib chiqishni boshlagan xodim avtomatik mas'ul bo'ladi
                .andExpect(jsonPath("$.assignee.email").value(ADMIN_EMAIL));

        mockMvc.perform(authorized(
                        json(patch("/api/v1/admin/complaints/{id}/status", complaintId), """
                                {"status": "RESOLVED", "officialResponse": "Tekshiruv o'tkazildi, chora ko'rildi."}
                                """),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        // Yakunlangan murojaatni qayta ochib bo'lmaydi
        mockMvc.perform(authorized(
                        json(patch("/api/v1/admin/complaints/{id}/status", complaintId), """
                                {"status": "IN_REVIEW"}
                                """),
                        token))
                .andExpect(status().isBadRequest());

        // Murojaatchi rasmiy javobni ko'radi, ichki izohni esa ko'rmaydi
        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.officialResponse").isNotEmpty())
                .andExpect(jsonPath("$.history[0].note").doesNotExist());
    }

    // ------------------------------------------------------------- reyestr (xodimlar uchun)

    @Test
    @DisplayName("Reyestrda murojaat ko'rinadi, lekin matn maydonlarisiz")
    void registerShowsAnonymisedEntries() throws Exception {
        String trackingCode = submitComplaint(complaintJson("Reyestrda ko'rinadigan murojaat sarlavhasi", true));

        mockMvc.perform(authorized(
                        get("/api/v1/complaints/register").param("code", trackingCode),
                        adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].trackingCode").value(trackingCode))
                .andExpect(jsonPath("$.content[0].categoryLabel").isNotEmpty())
                .andExpect(jsonPath("$.content[0].statusLabel").isNotEmpty())
                // Matn maydonlari reyestrga umuman chiqmaydi
                .andExpect(jsonPath("$.content[0].title").doesNotExist())
                .andExpect(jsonPath("$.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.content[0].officialResponse").doesNotExist());
    }

    @Test
    @DisplayName("Reyestr autentifikatsiyasiz ochilmaydi")
    void registerRequiresStaffToken() throws Exception {
        mockMvc.perform(get("/api/v1/complaints/register"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Xodim yashirgan murojaat reyestrda ko'rinmaydi")
    void hiddenComplaintIsExcludedFromRegister() throws Exception {
        String trackingCode = submitComplaint(complaintJson("Reyestrdan yashiriladigan murojaat", true));
        String token = adminToken();
        int complaintId = findComplaintId(token, trackingCode);

        mockMvc.perform(authorized(
                        json(patch("/api/v1/admin/complaints/{id}/register-visibility", complaintId), """
                                {"hidden": true}
                                """),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiddenFromRegister").value(true));

        mockMvc.perform(authorized(
                        get("/api/v1/complaints/register").param("code", trackingCode), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Murojaatchining o'zi esa kuzatuv kodi orqali baribir ko'ra oladi
        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------- ruxsatlar

    @Test
    @DisplayName("Tokensiz admin yo'liga kirib bo'lmaydi")
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/complaints"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Oddiy foydalanuvchi admin yo'liga kira olmaydi")
    void citizenCannotAccessAdminEndpoints() throws Exception {
        String token = registerAndLogin("Dilnoza Rahimova", "talaba2@test.uz", "Talaba12345!");

        mockMvc.perform(authorized(get("/api/v1/admin/complaints"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Yaroqsiz token bilan kirish 401 qaytaradi")
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/me"), "yaroqsiz.token.qiymati"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- ma'lumotnomalar

    @Test
    @DisplayName("Ma'lumotnomalar va statistika ochiq")
    void referenceAndStatsArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/reference/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").isNotEmpty())
                .andExpect(jsonPath("$[0].label").isNotEmpty());

        mockMvc.perform(get("/api/v1/reference/reporter-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.value == 'STUDENT')].label").value("Talaba"));

        mockMvc.perform(get("/api/v1/reference/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.value == 'MODERATOR')].label").value("Moderator"));

        mockMvc.perform(get("/api/v1/reference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.faculties").isArray())
                .andExpect(jsonPath("$.positions").isArray())
                .andExpect(jsonPath("$.roles").isArray());

        mockMvc.perform(get("/api/v1/stats/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.length()").value(5))
                .andExpect(jsonPath("$.byReporterType").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    // ------------------------------------------------------------- yordamchilar

    private String complaintJson(String title, boolean anonymous) {
        return """
                {
                  "title": "%s",
                  "description": "Bu sinov murojaati matni. Kamida o'ttiz belgidan iborat bo'lishi kerak, shuning uchun matn uzunroq yozilgan.",
                  "category": "EXAM_BRIBERY",
                  "reporterType": "STUDENT",
                  "subjectName": "Oliy matematika",
                  "accusedPosition": "TEACHER",
                  "incidentPlace": "2-bino, 305-xona",
                  "courseYear": 2,
                  "groupName": "KI-24-01",
                  "studyForm": "FULL_TIME",
                  "anonymous": %s
                }
                """.formatted(title, anonymous);
    }

    /** Murojaat yuboradi va kuzatuv kodini qaytaradi. */
    private String submitComplaint(String json) throws Exception {
        String response = mockMvc.perform(json(post("/api/v1/complaints"), json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.trackingCode");
    }

    /** Kuzatuv kodi bo'yicha admin ro'yxatidan murojaat id sini topadi. */
    private int findComplaintId(String token, String trackingCode) throws Exception {
        String response = mockMvc.perform(authorized(
                        get("/api/v1/admin/complaints").param("query", trackingCode), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.content[0].id");
    }
}
