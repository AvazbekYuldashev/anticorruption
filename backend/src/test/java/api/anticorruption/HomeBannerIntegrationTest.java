package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bosh banner foni: administrator bitta rasm yoki albom yuklaydi, tartibini
 * o'zgartiradi va o'chiradi; sayt rasmlarni kirishsiz oladi.
 *
 * <p>Sinovlar bitta bazani bo'lishadi, shuning uchun har biridan keyin albom
 * tozalanadi.
 */
class HomeBannerIntegrationTest extends AbstractIntegrationTest {

    /** Haqiqiy PNG imzosi: server fayl mazmunini e'lon qilingan turga solishtiradi. */
    private static final byte[] PNG_BYTES =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @AfterEach
    void clearAlbum() throws Exception {
        String token = adminToken();
        String album = mockMvc.perform(authorized(get("/api/v1/admin/home-banner"), token))
                .andReturn().getResponse().getContentAsString();

        for (Number id : JsonPath.<List<Number>>read(album, "$[*].id")) {
            mockMvc.perform(authorized(delete("/api/v1/admin/home-banner/{id}", id), token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Albom yuklanadi, tartibi o'zgaradi, rasm o'chadi - sayt har doim joriy holatni ko'radi")
    void albumIsUploadedReorderedAndDeleted() throws Exception {
        String token = adminToken();

        String uploaded = mockMvc.perform(authorized(upload(png("birinchi.png"), png("ikkinchi.png")), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].originalName").value("birinchi.png"))
                .andExpect(jsonPath("$[1].originalName").value("ikkinchi.png"))
                .andReturn().getResponse().getContentAsString();

        List<Number> ids = JsonPath.read(uploaded, "$[*].id");
        String firstUrl = JsonPath.read(uploaded, "$[0].url");
        assertThat(firstUrl).startsWith("/api/v1/media/");

        // Sayt kirishsiz oladi, yuklangan asl nomlar esa unga berilmaydi
        mockMvc.perform(get("/api/v1/home-banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].url").value(firstUrl))
                .andExpect(jsonPath("$[0].originalName").doesNotExist());

        // Rasm faylining o'zi ham ochiq
        mockMvc.perform(get(firstUrl)).andExpect(status().isOk());

        // Tartib almashadi
        mockMvc.perform(authorized(json(put("/api/v1/admin/home-banner/order"), """
                        {"ids": [%d, %d]}
                        """.formatted(ids.get(1).longValue(), ids.get(0).longValue())), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalName").value("ikkinchi.png"));

        // Albomdagi hamma rasm ko'rsatilmasa tartib qabul qilinmaydi
        mockMvc.perform(authorized(json(put("/api/v1/admin/home-banner/order"), """
                        {"ids": [%d]}
                        """.formatted(ids.get(0).longValue())), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.homeBanner.orderMismatch"));

        mockMvc.perform(authorized(delete("/api/v1/admin/home-banner/{id}", ids.get(1).longValue()), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/home-banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].url").value(firstUrl));
    }

    @Test
    @DisplayName("Albomda 10 tadan ortiq rasm bo'lmaydi va rasm bo'lmagan fayl qabul qilinmaydi")
    void albumIsLimitedAndAcceptsOnlyImages() throws Exception {
        String token = adminToken();

        MockMultipartFile[] eleven = new MockMultipartFile[11];
        for (int i = 0; i < eleven.length; i++) {
            eleven[i] = png("rasm-" + i + ".png");
        }
        mockMvc.perform(authorized(upload(eleven), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.homeBanner.tooMany"));

        mockMvc.perform(authorized(upload(new MockMultipartFile(
                        "files", "hujjat.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'})), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.file.imageTypeNotAllowed"));

        // Rad etilgan yuklashlardan keyin albom bo'sh qoladi
        mockMvc.perform(get("/api/v1/home-banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Banner rasmini oddiy foydalanuvchi yuklay olmaydi")
    void citizenCannotUpload() throws Exception {
        String citizen = createUserAndLogin("Banner Fuqarosi", "banner.fuqaro@test.uz", "Banner12345!");

        mockMvc.perform(authorized(upload(png("begona.png")), citizen))
                .andExpect(status().isForbidden());
    }

    private MockMultipartHttpServletRequestBuilder upload(MockMultipartFile... files) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/v1/admin/home-banner");
        for (MockMultipartFile file : files) {
            builder.file(file);
        }
        return builder;
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("files", name, "image/png", PNG_BYTES);
    }
}
