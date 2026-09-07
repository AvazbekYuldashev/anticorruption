package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integratsion testlar uchun umumiy asos.
 *
 * <p>Barcha testlar bitta Spring konteksti va bitta H2 bazasini bo'lishadi
 * (tez ishlashi uchun), shuning uchun har bir test o'ziga xos email va
 * nomlardan foydalanishi kerak.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    /** application-test.properties da yaratiladigan administrator. */
    protected static final String ADMIN_EMAIL = "admin@test.uz";
    protected static final String ADMIN_PASSWORD = "TestAdmin12345!";

    @Autowired
    protected MockMvc mockMvc;

    /** Tizimga kiradi va JWT tokenni qaytaradi. */
    protected String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** Yangi fuqaro hisobini yaratadi va tokenini qaytaradi. */
    protected String registerAndLogin(String fullName, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "%s", "email": "%s", "password": "%s"}
                                """.formatted(fullName, email, password)))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    /**
     * Bearer tokenini qo'shadi.
     *
     * <p>Generic tur kerak: Spring 7 da oddiy va multipart so'rov quruvchilari
     * bir-birining merosxo'ri emas, ikkalasi ham
     * {@link AbstractMockHttpServletRequestBuilder} dan kelib chiqadi.
     */
    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B authorized(B builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    /** JSON tanasini qo'shadi. */
    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B json(B builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
