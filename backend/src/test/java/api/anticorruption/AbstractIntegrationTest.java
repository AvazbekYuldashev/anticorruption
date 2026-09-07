package api.anticorruption;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integratsion testlar uchun umumiy asos.
 *
 * <p>Barcha testlar bitta Spring konteksti va bitta H2 bazasini bo'lishadi
 * (tez ishlashi uchun), shuning uchun har bir test o'ziga xos email va
 * nomlardan foydalanishi kerak.
 *
 * <p>Seans haqiqiy ilovadagidek ishlaydi: token javob tanasida emas,
 * {@code HttpOnly} cookie'da keladi va yozuv so'rovlari CSRF tokenini
 * talab qiladi. Shuning uchun {@link #authorized} ikkalasini ham qo'shadi -
 * test kodi bu tafsilotlar bilan shug'ullanmaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    /** application-test.properties da yaratiladigan administrator. */
    protected static final String ADMIN_EMAIL = "admin@test.uz";
    protected static final String ADMIN_PASSWORD = "TestAdmin12345!";

    /** application-test.properties dagi cookie nomlari. */
    protected static final String ACCESS_COOKIE = "ac_access";
    protected static final String REFRESH_COOKIE = "ac_refresh";

    @Autowired
    protected MockMvc mockMvc;

    /** Tizimga kiradi va kirish cookie'sining qiymatini qaytaradi. */
    protected String login(String email, String password) throws Exception {
        return requireCookie(loginResponse(email, password), ACCESS_COOKIE);
    }

    /** Tizimga kiradi va butun javobni qaytaradi - cookie'larni tekshirish uchun. */
    protected MockHttpServletResponse loginResponse(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    protected String requireCookie(MockHttpServletResponse response, String name) {
        Cookie cookie = response.getCookie(name);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            throw new IllegalStateException("Javobda '" + name + "' cookie'si yo'q");
        }
        return cookie.getValue();
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /**
     * Yangi fuqaro hisobini yaratadi va tokenini qaytaradi.
     *
     * <p>Ochiq ro'yxatdan o'tish yo'q, shuning uchun hisob administrator
     * nomidan ochiladi - saytda ham xuddi shunday bo'ladi.
     */
    protected String createUserAndLogin(String fullName, String email, String password)
            throws Exception {

        mockMvc.perform(authorized(json(post("/api/v1/admin/users"), """
                        {"fullName": "%s", "email": "%s", "password": "%s", "role": "CITIZEN"}
                        """.formatted(fullName, email, password)), adminToken()))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    /**
     * So'rovni seans cookie'si va CSRF tokeni bilan to'ldiradi.
     *
     * <p>Generic tur kerak: Spring 7 da oddiy va multipart so'rov quruvchilari
     * bir-birining merosxo'ri emas, ikkalasi ham
     * {@link AbstractMockHttpServletRequestBuilder} dan kelib chiqadi.
     */
    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B authorized(B builder, String token) {
        return builder.cookie(new Cookie(ACCESS_COOKIE, token)).with(csrf());
    }

    /**
     * Seansi yo'q, lekin brauzerdek CSRF tokeni bor so'rov.
     *
     * <p>CSRF tokenini kirmagan mijoz ham oladi ({@code GET /auth/csrf}),
     * shuning uchun "kirish shart" tekshiruvlari aynan shu holatni sinashi
     * kerak. Tokensiz so'rov CSRF filtrida 403 bo'lib to'xtaydi va
     * tekshiruv nazarda tutgan 401 gacha yetib bormaydi.
     */
    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B anonymous(B builder) {
        return builder.with(csrf());
    }

    /** JSON tanasini qo'shadi. */
    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B json(B builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
