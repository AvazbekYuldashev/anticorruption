package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fakultet va kafedralarni boshqarish hamda ularning murojaat bilan bog'lanishi. */
class UniversityStructureIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Admin fakultet va kafedra qo'shadi, ular ochiq ma'lumotnomada ko'rinadi")
    void adminCanManageFacultiesAndDepartments() throws Exception {
        String token = adminToken();

        int facultyId = createFaculty(token, "Kompyuter injiniringi fakulteti", "KIF1");

        String departmentResponse = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/faculties/{id}/departments", facultyId), """
                                {"name": "Dasturiy injiniring kafedrasi", "code": "DIK1"}
                                """),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dasturiy injiniring kafedrasi"))
                .andReturn().getResponse().getContentAsString();

        int departmentId = JsonPath.read(departmentResponse, "$.id");

        // Ochiq ma'lumotnomada fakultet kafedrasi bilan birga ko'rinadi
        mockMvc.perform(get("/api/v1/reference/faculties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'KIF1')].name").value("Kompyuter injiniringi fakulteti"));

        mockMvc.perform(get("/api/v1/reference/faculties/{id}/departments", facultyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + departmentId + ")].code").value("DIK1"));
    }

    @Test
    @DisplayName("Bir xil kod bilan ikkinchi fakultet yaratib bo'lmaydi")
    void duplicateFacultyCodeIsRejected() throws Exception {
        String token = adminToken();
        createFaculty(token, "Iqtisodiyot fakulteti", "IQT2");

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/faculties"), """
                                {"name": "Boshqa fakultet", "code": "IQT2"}
                                """),
                        token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Murojaatga bog'langan fakultet o'chirilmaydi, nofaol qilish taklif etiladi")
    void facultyWithComplaintsCannotBeDeleted() throws Exception {
        String token = adminToken();
        int facultyId = createFaculty(token, "Energetika fakulteti", "ENR3");

        mockMvc.perform(json(post("/api/v1/complaints"), """
                        {
                          "title": "Fakultetga bog'langan murojaat sarlavhasi",
                          "description": "Bu murojaat fakultetga bog'langani uchun uni o'chirishga to'sqinlik qilishi kerak.",
                          "category": "GRADE_SELLING",
                          "reporterType": "STUDENT",
                          "facultyId": %d,
                          "anonymous": true
                        }
                        """.formatted(facultyId)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(delete("/api/v1/admin/faculties/{id}", facultyId), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("nofaol")));
    }

    @Test
    @DisplayName("Nofaol fakultetni yangi murojaatda tanlab bo'lmaydi")
    void inactiveFacultyCannotBeSelected() throws Exception {
        String token = adminToken();
        int facultyId = createFaculty(token, "Yopilgan fakultet", "OLD4");

        mockMvc.perform(authorized(
                        json(put("/api/v1/admin/faculties/{id}", facultyId), """
                                {"name": "Yopilgan fakultet", "code": "OLD4", "active": false}
                                """),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(json(post("/api/v1/complaints"), """
                        {
                          "title": "Nofaol fakultetga yuborilgan murojaat",
                          "description": "Bu murojaat nofaol fakultetni ko'rsatgani uchun rad etilishi kerak.",
                          "category": "OTHER",
                          "reporterType": "STUDENT",
                          "facultyId": %d,
                          "anonymous": true
                        }
                        """.formatted(facultyId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Kafedra boshqa fakultetga tegishli bo'lsa murojaat rad etiladi")
    void departmentMustBelongToSelectedFaculty() throws Exception {
        String token = adminToken();
        int firstFaculty = createFaculty(token, "Birinchi fakultet", "FCA5");
        int secondFaculty = createFaculty(token, "Ikkinchi fakultet", "FCB5");

        String departmentResponse = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/faculties/{id}/departments", firstFaculty), """
                                {"name": "Birinchi fakultet kafedrasi", "code": "KAF5"}
                                """),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int departmentId = JsonPath.read(departmentResponse, "$.id");

        mockMvc.perform(json(post("/api/v1/complaints"), """
                        {
                          "title": "Mos kelmaydigan kafedra bilan murojaat",
                          "description": "Bu murojaatda kafedra boshqa fakultetga tegishli, shuning uchun rad etilishi kerak.",
                          "category": "OTHER",
                          "reporterType": "STUDENT",
                          "facultyId": %d,
                          "departmentId": %d,
                          "anonymous": true
                        }
                        """.formatted(secondFaculty, departmentId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Kafedra ko'rsatilsa, fakultet avtomatik aniqlanadi")
    void facultyIsDerivedFromDepartment() throws Exception {
        String token = adminToken();
        int facultyId = createFaculty(token, "Avtomatik aniqlanadigan fakultet", "AUT6");

        String departmentResponse = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/faculties/{id}/departments", facultyId), """
                                {"name": "Avtomatik kafedra", "code": "AKF6"}
                                """),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int departmentId = JsonPath.read(departmentResponse, "$.id");

        String created = mockMvc.perform(json(post("/api/v1/complaints"), """
                        {
                          "title": "Faqat kafedra ko'rsatilgan murojaat",
                          "description": "Bu murojaatda faqat kafedra ko'rsatilgan, fakultet undan kelib chiqishi kerak.",
                          "category": "RETAKE_PAYMENT",
                          "reporterType": "STUDENT",
                          "departmentId": %d,
                          "anonymous": true
                        }
                        """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String trackingCode = JsonPath.read(created, "$.trackingCode");

        mockMvc.perform(get("/api/v1/complaints/track/{code}", trackingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facultyName").value("Avtomatik aniqlanadigan fakultet"))
                .andExpect(jsonPath("$.departmentName").value("Avtomatik kafedra"));
    }

    @Test
    @DisplayName("Moderator fakultet qo'sha olmaydi - bu faqat administrator ishi")
    void onlyAdminCanManageStructure() throws Exception {
        String citizenToken = createUserAndLogin("Oddiy Foydalanuvchi", "tuzilma@test.uz", "Tuzilma12345!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/faculties"), """
                                {"name": "Ruxsatsiz fakultet", "code": "NOP7"}
                                """),
                        citizenToken))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------- yordamchilar

    private int createFaculty(String token, String name, String code) throws Exception {
        String response = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/faculties"), """
                                {"name": "%s", "code": "%s"}
                                """.formatted(name, code)),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.id");
    }
}
