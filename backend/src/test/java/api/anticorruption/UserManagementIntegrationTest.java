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

/**
 * Hisoblarni boshqarish: administrator yaratadi, bloklaydi va o'chiradi;
 * egasi esa o'z login va parolini almashtiradi.
 */
class UserManagementIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Administrator xodim hisobini yaratadi va u tizimga kiradi")
    void adminCreatesStaffAccount() throws Exception {
        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/users"), """
                                {
                                  "fullName": "Yangi Moderator",
                                  "email": "yangi.moderator@test.uz",
                                  "password": "Moder12345!",
                                  "role": "MODERATOR"
                                }
                                """),
                        adminToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MODERATOR"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn().getResponse().getContentAsString();

        // Yaratilgan hisob darrov ishlaydi
        String token = login("yangi.moderator@test.uz", "Moder12345!");
        mockMvc.perform(authorized(get("/api/v1/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("yangi.moderator@test.uz"));

        // Xodim boshqa hisob ocha olmaydi - bu faqat administrator ishi
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/users"), """
                                {"fullName": "Ruxsatsiz Hisob", "email": "ruxsatsiz2@test.uz",
                                 "password": "Parol12345!", "role": "CITIZEN"}
                                """),
                        token))
                .andExpect(status().isForbidden());

        int id = JsonPath.read(created, "$.id");
        org.junit.jupiter.api.Assertions.assertTrue(id > 0);
    }

    @Test
    @DisplayName("Hisob o'chiriladi, o'z hisobini esa o'chirib bo'lmaydi")
    void accountIsDeletedButNotOwn() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/users"), """
                                {"fullName": "O'chiriladigan Hisob", "email": "ochiriladi@test.uz",
                                 "password": "Ochir12345!", "role": "CITIZEN"}
                                """),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int id = JsonPath.read(created, "$.id");

        mockMvc.perform(authorized(delete("/api/v1/admin/users/{id}", id), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/api/v1/admin/users/{id}", id), token))
                .andExpect(status().isNotFound());

        // Administrator o'zini o'chira olmaydi - aks holda tizim boshqaruvsiz qoladi
        int adminId = JsonPath.read(
                mockMvc.perform(authorized(get("/api/v1/me"), token))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        mockMvc.perform(authorized(delete("/api/v1/admin/users/{id}", adminId), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.user.cannotDeleteSelf"));
    }

    @Test
    @DisplayName("Murojaati bor hisob o'chirilmaydi")
    void accountWithComplaintsIsNotDeleted() throws Exception {
        String userToken = createUserAndLogin("Murojaatchi Hisob", "murojaatchi@test.uz", "Muroj12345!");

        mockMvc.perform(authorized(
                        json(post("/api/v1/complaints"), """
                                {
                                  "title": "Hisobga bog'langan murojaat sinovi",
                                  "description": "Bu murojaat hisob o'chirilishiga to'sqinlik qilishi kerak.",
                                  "category": "OTHER",
                                  "reporterType": "STUDENT"
                                }
                                """),
                        userToken))
                .andExpect(status().isCreated());

        int id = JsonPath.read(
                mockMvc.perform(authorized(get("/api/v1/me"), userToken))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        mockMvc.perform(authorized(delete("/api/v1/admin/users/{id}", id), adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("error.user.hasComplaints"));
    }

    @Test
    @DisplayName("Foydalanuvchi login (email) va parolini o'zi almashtiradi")
    void userChangesOwnLoginAndPassword() throws Exception {
        String token = createUserAndLogin("Profil Egasi", "eski.login@test.uz", "Eski12345!");

        mockMvc.perform(authorized(
                        json(put("/api/v1/me"), """
                                {"fullName": "Profil Egasi", "email": "yangi.login@test.uz", "phone": "+998901234567"}
                                """),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("yangi.login@test.uz"));

        // Yangi email bilan kirish ishlaydi
        String newToken = login("yangi.login@test.uz", "Eski12345!");

        // Joriy parol noto'g'ri bo'lsa almashtirilmaydi
        mockMvc.perform(authorized(
                        json(post("/api/v1/me/password"), """
                                {"currentPassword": "notogri", "newPassword": "Yangi12345!"}
                                """),
                        newToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.user.currentPasswordWrong"));

        mockMvc.perform(authorized(
                        json(post("/api/v1/me/password"), """
                                {"currentPassword": "Eski12345!", "newPassword": "Yangi12345!"}
                                """),
                        newToken))
                .andExpect(status().isNoContent());

        // Endi faqat yangi parol ishlaydi
        login("yangi.login@test.uz", "Yangi12345!");
        mockMvc.perform(anonymous(json(post("/api/v1/auth/login"), """
                        {"email": "yangi.login@test.uz", "password": "Eski12345!"}
                        """)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Band emailga o'tib bo'lmaydi")
    void takenEmailIsRejected() throws Exception {
        String token = createUserAndLogin("Band Email", "band.email@test.uz", "Band12345!");

        mockMvc.perform(authorized(
                        json(put("/api/v1/me"), """
                                {"fullName": "Band Email", "email": "%s", "phone": ""}
                                """.formatted(ADMIN_EMAIL)),
                        token))
                .andExpect(status().isConflict());
    }
}
