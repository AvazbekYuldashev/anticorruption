package api.anticorruption;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seans oqimi: kirish, yangilash, chiqish va CSRF himoyasi.
 *
 * <p>Bu testlar aynan xavfsizlik kelishuvini qo'riqlaydi - token javob
 * tanasiga qaytib qolsa yoki CSRF tekshiruvi tasodifan o'chib qolsa,
 * ilova ishlashda davom etardi va buni faqat shu testlar payqaydi.
 */
class AuthSessionIntegrationTest extends AbstractIntegrationTest {

    // ------------------------------------------------------------------ kirish

    @Test
    @DisplayName("Kirishda tokenlar javob tanasida emas, HttpOnly cookie'da beriladi")
    void loginPutsTokensIntoHttpOnlyCookies() throws Exception {
        MockHttpServletResponse response = loginResponse(ADMIN_EMAIL, ADMIN_PASSWORD);

        Cookie access = response.getCookie(ACCESS_COOKIE);
        Cookie refresh = response.getCookie(REFRESH_COOKIE);

        assertThat(access).isNotNull();
        assertThat(access.isHttpOnly()).isTrue();
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        // Yangilash cookie'si boshqa so'rovlarga umuman yuborilmaydi.
        assertThat(refresh.getPath()).isEqualTo("/api/v1/auth");

        assertThat(response.getContentAsString())
                .doesNotContain("accessToken")
                .contains("expiresIn");
    }

    @Test
    @DisplayName("Cookie bilan himoyalangan resursga kirish mumkin")
    void accessCookieAuthenticatesRequests() throws Exception {
        String access = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/me").cookie(new Cookie(ACCESS_COOKIE, access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
    }

    // --------------------------------------------------------------- yangilash

    @Test
    @DisplayName("Yangilash tokeni har safar almashadi va eskisi ishlamay qoladi")
    void refreshRotatesTokenAndInvalidatesTheOldOne() throws Exception {
        String first = requireCookie(loginResponse(ADMIN_EMAIL, ADMIN_PASSWORD), REFRESH_COOKIE);

        MockHttpServletResponse refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, first)))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        String second = requireCookie(refreshed, REFRESH_COOKIE);
        assertThat(second).isNotEqualTo(first);

        // Eski token qayta ishlatildi: bu o'g'irlanish alomati.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, first)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("error.auth.refreshInvalid"));

        // ...shuning uchun o'sha foydalanuvchining barcha seanslari uziladi.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, second)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cookie'siz yangilash 401 qaytaradi")
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("error.auth.refreshInvalid"));
    }

    // ------------------------------------------------------------------ chiqish

    @Test
    @DisplayName("Chiqish cookie'larni o'chiradi va yangilash tokenini bekor qiladi")
    void logoutClearsCookiesAndRevokesRefreshToken() throws Exception {
        String refresh = requireCookie(loginResponse(ADMIN_EMAIL, ADMIN_PASSWORD), REFRESH_COOKIE);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, refresh)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getCookie(ACCESS_COOKIE)).isNotNull();
        assertThat(response.getCookie(ACCESS_COOKIE).getMaxAge()).isZero();
        assertThat(response.getCookie(REFRESH_COOKIE).getMaxAge()).isZero();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Chiqqan seansning tokeni qaytib kelsa boshqa qurilmalar uzilmaydi")
    void replayingALoggedOutTokenDoesNotRevokeOtherSessions() throws Exception {
        String email = "ikki.qurilma@test.uz";
        String password = "IkkiQurilma12345!";
        createUserAndLogin("Ikki Qurilma", email, password);

        String phone = requireCookie(loginResponse(email, password), REFRESH_COOKIE);
        String laptop = requireCookie(loginResponse(email, password), REFRESH_COOKIE);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, phone)))
                .andExpect(status().isNoContent());

        /*
         * Chiqqan qurilmaning eskirgan cookie'si qaytib keldi. Bu
         * o'g'irlanish alomati emas - o'sha tokenni foydalanuvchining o'zi
         * bekor qilgan, uni rad etish kifoya.
         */
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, phone)))
                .andExpect(status().isUnauthorized());

        // Shuning uchun boshqa qurilma ishlashda davom etadi.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, laptop)))
                .andExpect(status().isOk());
    }

    // --------------------------------------------------------------------- CSRF

    @Test
    @DisplayName("Seans cookie'si bor, lekin CSRF tokeni yo'q yozuv so'rovi rad etiladi")
    void writeWithoutCsrfTokenIsRejected() throws Exception {
        String access = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/v1/admin/users")
                        .cookie(new Cookie(ACCESS_COOKIE, access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "CSRF sinovi", "email": "csrf-sinov@test.uz",
                                 "password": "CsrfSinov12345!", "role": "CITIZEN"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("error.csrf.invalid"));
    }

    @Test
    @DisplayName("Anonim murojaat CSRF tokenisiz ham yuboriladi")
    void anonymousComplaintDoesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "CSRF chegarasini tekshirish uchun murojaat",
                                  "description": "Bu sinov murojaati matni. Kamida o'ttiz belgidan iborat bo'lishi kerak, shuning uchun matn uzunroq yozilgan.",
                                  "category": "EXAM_BRIBERY",
                                  "reporterType": "STUDENT",
                                  "anonymous": true
                                }
                                """))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------- parol

    @Test
    @DisplayName("Parol almashtirilganda eski seanslar uziladi, joriy seans yangilanadi")
    void changingPasswordRevokesOtherSessionsButKeepsCurrentOne() throws Exception {
        String email = "seans@test.uz";
        String password = "SeansSinov12345!";
        createUserAndLogin("Seans Sinovi", email, password);

        // Boshqa qurilmadagi seans - u uzilishi kerak.
        String otherDevice = requireCookie(loginResponse(email, password), REFRESH_COOKIE);

        MockHttpServletResponse current = loginResponse(email, password);
        String access = requireCookie(current, ACCESS_COOKIE);

        MockHttpServletResponse changed = mockMvc.perform(
                        authorized(json(post("/api/v1/me/password"), """
                                {"currentPassword": "%s", "newPassword": "YangiParol12345!"}
                                """.formatted(password)), access))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, otherDevice)))
                .andExpect(status().isUnauthorized());

        // Joriy qurilma javob bilan birga yangi cookie'larni oldi va ishlashda davom etadi.
        String renewed = requireCookie(changed, REFRESH_COOKIE);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_COOKIE, renewed)))
                .andExpect(status().isOk());
    }
}
