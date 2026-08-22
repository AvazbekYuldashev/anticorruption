package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sayt bo'limlari: yangiliklar, xodimlar, matnli sahifalar va havolalar. */
class SiteContentIntegrationTest extends AbstractIntegrationTest {

    // ------------------------------------------------------------- yangiliklar

    @Test
    @DisplayName("Qoralama yangilik ochiq ro'yxatda ko'rinmaydi, chop etilgach ko'rinadi")
    void draftBecomesVisibleAfterPublishing() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Kafedrada tushuntirish ishlari o'tkazildi",
                                  "summary": "Talabalar bilan uchrashuv bo'lib o'tdi",
                                  "body": "Uchrashuvda korrupsiyaga qarshi kurash masalalari muhokama qilindi."
                                }
                                """),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false))
                // Slug o'zbekcha sarlavhadan avtomatik yasaladi
                .andExpect(jsonPath("$.slug").value("kafedrada-tushuntirish-ishlari-otkazildi"))
                .andReturn().getResponse().getContentAsString();

        int newsId = JsonPath.read(created, "$.id");
        String slug = JsonPath.read(created, "$.slug");

        mockMvc.perform(get("/api/v1/news/{slug}", slug))
                .andExpect(status().isNotFound());

        mockMvc.perform(authorized(patch("/api/v1/admin/news/{id}/publish", newsId).param("published", "true"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/news/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Kafedrada tushuntirish ishlari o'tkazildi"))
                // O'qilishi bilan ko'rishlar soni oshadi
                .andExpect(jsonPath("$.viewCount").value(1));

        mockMvc.perform(get("/api/v1/news/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(2));
    }

    @Test
    @DisplayName("Bir xil sarlavhali ikkinchi yangilikka boshqa slug beriladi")
    void duplicateTitleGetsDistinctSlug() throws Exception {
        String token = adminToken();
        String body = """
                {"title": "Takrorlanuvchi sarlavha sinovi", "body": "Birinchi yangilik matni bu yerda."}
                """;

        mockMvc.perform(authorized(json(post("/api/v1/admin/news"), body), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("takrorlanuvchi-sarlavha-sinovi"));

        mockMvc.perform(authorized(json(post("/api/v1/admin/news"), body), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("takrorlanuvchi-sarlavha-sinovi-2"));
    }

    @Test
    @DisplayName("Yangilikka muqova rasmi yuklanadi va ochiq media orqali beriladi")
    void newsCoverImageIsServedPublicly() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {"title": "Muqovali yangilik sinovi", "body": "Bu yangilikka rasm biriktiriladi.", "published": true}
                                """),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int newsId = JsonPath.read(created, "$.id");

        MockMultipartFile image = new MockMultipartFile(
                "file", "muqova.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        String withCover = mockMvc.perform(authorized(
                        multipart("/api/v1/admin/news/{id}/cover", newsId).file(image), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String coverUrl = JsonPath.read(withCover, "$.coverImageUrl");

        // Rasm autentifikatsiyasiz olinadi
        mockMvc.perform(get(coverUrl))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Muqova sifatida rasm bo'lmagan fayl qabul qilinmaydi")
    void nonImageCoverIsRejected() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {"title": "Yaroqsiz muqova sinovi", "body": "Bu yangilikka pdf yuklashga urinamiz."}
                                """),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int newsId = JsonPath.read(created, "$.id");

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "hujjat.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(authorized(multipart("/api/v1/admin/news/{id}/cover", newsId).file(pdf), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Yangilik qidiruvi sarlavha bo'yicha ishlaydi")
    void publishedNewsCanBeSearched() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Stipendiya masalasi bo'yicha tushuntirish",
                                  "body": "Stipendiya taqsimoti qoidalari haqida batafsil ma'lumot.",
                                  "published": true
                                }
                                """),
                        token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/news").param("query", "stipendiya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ------------------------------------------------------------- xodimlar

    @Test
    @DisplayName("Xodim qo'shiladi va ochiq ro'yxatda ko'rinadi, nofaol qilinsa yo'qoladi")
    void staffDirectoryReflectsActiveFlag() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/staff"), """
                                {
                                  "fullName": "Nodira Yusupova",
                                  "position": "Bo'lim boshlig'i",
                                  "academicDegree": "PhD, dotsent",
                                  "phone": "+998901112233",
                                  "receptionHours": "Dushanba-juma, 14:00-17:00",
                                  "displayOrder": 1
                                }
                                """),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int staffId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/v1/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + staffId + ")].fullName").value("Nodira Yusupova"));

        mockMvc.perform(authorized(
                        json(put("/api/v1/admin/staff/{id}", staffId), """
                                {"fullName": "Nodira Yusupova", "position": "Bo'lim boshlig'i", "active": false}
                                """),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + staffId + ")]").isEmpty());
    }

    // ------------------------------------------------------------- sahifalar

    @Test
    @DisplayName("Bo'lim haqida sahifasi yaratiladi va slug orqali o'qiladi")
    void staticPageIsCreatedAndReadBySlug() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/pages"), """
                                {
                                  "slug": "bolim-haqida",
                                  "title": "Bo'lim haqida",
                                  "body": "Korrupsiyaga qarshi kurash bo'limi 2020-yilda tashkil etilgan.",
                                  "displayOrder": 1
                                }
                                """),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("bolim-haqida"));

        mockMvc.perform(get("/api/v1/pages/bolim-haqida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bo'lim haqida"))
                .andExpect(jsonPath("$.body").isNotEmpty());

        // Menyu ro'yxatida sahifa bor, lekin matni qaytarilmaydi
        mockMvc.perform(get("/api/v1/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'bolim-haqida')].title").value("Bo'lim haqida"))
                .andExpect(jsonPath("$[?(@.slug == 'bolim-haqida')].body")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    @DisplayName("Bir xil slug bilan ikkinchi sahifa yaratib bo'lmaydi")
    void duplicatePageSlugIsRejected() throws Exception {
        String token = adminToken();
        String body = """
                {"slug": "takroriy-sahifa", "title": "Takroriy sahifa", "body": "Sahifa matni."}
                """;

        mockMvc.perform(authorized(json(post("/api/v1/admin/pages"), body), token))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(json(post("/api/v1/admin/pages"), body), token))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------- havolalar

    @Test
    @DisplayName("Foydali havola qo'shiladi va ochiq ro'yxatda ko'rinadi")
    void usefulLinkIsPubliclyListed() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/links"), """
                                {
                                  "title": "Bosh prokuratura",
                                  "url": "https://prokuratura.uz",
                                  "groupName": "Davlat organlari",
                                  "displayOrder": 1
                                }
                                """),
                        token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Bosh prokuratura')].url").value("https://prokuratura.uz"));
    }

    @Test
    @DisplayName("Manzil http bilan boshlanmasa havola qabul qilinmaydi")
    void invalidLinkUrlIsRejected() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/links"), """
                                {"title": "Yaroqsiz havola", "url": "prokuratura.uz"}
                                """),
                        token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.url").exists());
    }

    @Test
    @DisplayName("Kontentni tahrirlash uchun tizimga kirish shart")
    void contentManagementRequiresAuthentication() throws Exception {
        mockMvc.perform(json(post("/api/v1/admin/news"), """
                        {"title": "Ruxsatsiz yangilik", "body": "Bu yaratilmasligi kerak."}
                        """))
                .andExpect(status().isUnauthorized());
    }
}
