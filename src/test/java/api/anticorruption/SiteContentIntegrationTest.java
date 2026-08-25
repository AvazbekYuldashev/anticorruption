package api.anticorruption;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                                  "blocks": [{"type": "TEXT", "text": "Uchrashuvda korrupsiyaga qarshi kurash masalalari muhokama qilindi."}]
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
                {"title": "Takrorlanuvchi sarlavha sinovi", "blocks": [{"type": "TEXT", "text": "Birinchi yangilik matni bu yerda."}]}
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
                                {"title": "Muqovali yangilik sinovi", "blocks": [{"type": "TEXT", "text": "Bu yangilikka rasm biriktiriladi."}], "published": true}
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
                                {"title": "Yaroqsiz muqova sinovi", "blocks": [{"type": "TEXT", "text": "Bu yangilikka pdf yuklashga urinamiz."}]}
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

    // ------------------------------------------------------------- bloklar

    @Test
    @DisplayName("Matn va rasm bloklari kiritilgan tartibda saqlanadi")
    void blocksKeepTheOrderTheyWereAddedIn() throws Exception {
        String token = adminToken();
        String first = uploadMedia(token, "birinchi.png");
        String second = uploadMedia(token, "ikkinchi.png");

        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Blokli yangilik sinovi uchun sarlavha",
                                  "published": true,
                                  "blocks": [
                                    {"type": "TEXT", "text": "Birinchi xatboshi."},
                                    {"type": "IMAGE", "storedName": "%s", "originalName": "birinchi.png"},
                                    {"type": "TEXT", "text": "Ikkinchi xatboshi."},
                                    {"type": "IMAGE", "storedName": "%s", "originalName": "ikkinchi.png"}
                                  ]
                                }
                                """.formatted(first, second)),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blocks.length()").value(4))
                .andExpect(jsonPath("$.blocks[0].type").value("TEXT"))
                .andExpect(jsonPath("$.blocks[1].type").value("IMAGE"))
                .andExpect(jsonPath("$.blocks[2].type").value("TEXT"))
                .andExpect(jsonPath("$.blocks[3].type").value("IMAGE"))
                // Matn bloklari qidiruv uchun birlashtiriladi
                .andExpect(jsonPath("$.body").value("Birinchi xatboshi.\n\nIkkinchi xatboshi."))
                .andReturn().getResponse().getContentAsString();

        String slug = JsonPath.read(created, "$.slug");

        mockMvc.perform(get("/api/v1/news/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks.length()").value(4))
                .andExpect(jsonPath("$.blocks[1].url").isNotEmpty())
                .andExpect(jsonPath("$.blocks[1].displayOrder").value(1));

        // Ro'yxatda faqat rasm bloklari sanaladi
        mockMvc.perform(get("/api/v1/news").param("query", "Blokli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].imageCount").value(2));
    }

    @Test
    @DisplayName("Saqlashda blok ro'yxati to'liq almashtiriladi")
    void savingReplacesTheWholeBlockList() throws Exception {
        String token = adminToken();
        int newsId = createNews(token, "Bloklar almashtirilishi sinovi", false);

        mockMvc.perform(authorized(
                        json(put("/api/v1/admin/news/{id}", newsId), """
                                {
                                  "title": "Bloklar almashtirilishi sinovi",
                                  "blocks": [
                                    {"type": "TEXT", "text": "Yangi yagona xatboshi."}
                                  ]
                                }
                                """),
                        token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks.length()").value(1))
                .andExpect(jsonPath("$.blocks[0].text").value("Yangi yagona xatboshi."));
    }

    @Test
    @DisplayName("Bo'sh matn bloki qabul qilinmaydi")
    void emptyTextBlockIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Bo'sh matn bloki sinovi uchun",
                                  "blocks": [{"type": "TEXT", "text": "   "}]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.news.textBlockEmpty"));
    }

    @Test
    @DisplayName("Faylsiz rasm bloki qabul qilinmaydi")
    void imageBlockWithoutFileIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Faylsiz rasm bloki sinovi uchun",
                                  "blocks": [{"type": "IMAGE"}]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.news.imageBlockMissing"));
    }

    @Test
    @DisplayName("Noma'lum blok turi qabul qilinmaydi")
    void unknownBlockTypeIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Notogri blok turi sinovi uchun",
                                  "blocks": [{"type": "VIDEO", "text": "x"}]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.news.blockTypeInvalid"));
    }

    @Test
    @DisplayName("Sarlavha bloki alohida tur sifatida saqlanadi va qidiruvga tushadi")
    void headingBlockIsStoredAsItsOwnType() throws Exception {
        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Sarlavhali yangilik sinovi uchun",
                                  "published": true,
                                  "blocks": [
                                    {"type": "HEADING", "text": "Tadbir yakunlari"},
                                    {"type": "TEXT", "text": "Yig'ilishda qaror qabul qilindi."}
                                  ]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blocks[0].type").value("HEADING"))
                .andExpect(jsonPath("$.blocks[0].text").value("Tadbir yakunlari"))
                .andExpect(jsonPath("$.blocks[1].type").value("TEXT"))
                // Sarlavha ham qidiruv matniga kiradi
                .andExpect(jsonPath("$.body").value("Tadbir yakunlari\n\nYig'ilishda qaror qabul qilindi."))
                .andReturn().getResponse().getContentAsString();

        String slug = JsonPath.read(created, "$.slug");

        mockMvc.perform(get("/api/v1/news").param("query", "Tadbir yakunlari"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value(slug));
    }

    @Test
    @DisplayName("Albom bloki rasmlarni o'z tartibida saqlaydi va ular ham sanaladi")
    void galleryBlockKeepsItsOwnImageOrder() throws Exception {
        String token = adminToken();
        String single = uploadMedia(token, "yakka.png");
        String first = uploadMedia(token, "albom-1.png");
        String second = uploadMedia(token, "albom-2.png");

        String created = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Albomli yangilik sinovi uchun",
                                  "published": true,
                                  "blocks": [
                                    {"type": "IMAGE", "storedName": "%s", "originalName": "yakka.png"},
                                    {"type": "GALLERY", "caption": "Tadbirdan lavhalar", "images": [
                                      {"storedName": "%s", "originalName": "albom-1.png"},
                                      {"storedName": "%s", "originalName": "albom-2.png", "caption": "Ikkinchi"}
                                    ]}
                                  ]
                                }
                                """.formatted(single, first, second)),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blocks[1].type").value("GALLERY"))
                .andExpect(jsonPath("$.blocks[1].caption").value("Tadbirdan lavhalar"))
                .andExpect(jsonPath("$.blocks[1].images.length()").value(2))
                .andExpect(jsonPath("$.blocks[1].images[0].displayOrder").value(0))
                .andExpect(jsonPath("$.blocks[1].images[1].caption").value("Ikkinchi"))
                .andExpect(jsonPath("$.blocks[1].images[1].url").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String slug = JsonPath.read(created, "$.slug");

        mockMvc.perform(get("/api/v1/news/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[1].images.length()").value(2));

        // Ro'yxatdagi hisob yakka rasmni ham, albom ichidagilarni ham qamrab oladi
        mockMvc.perform(get("/api/v1/news").param("query", "Albomli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].imageCount").value(3));
    }

    @Test
    @DisplayName("Rasmsiz albom bloki qabul qilinmaydi")
    void emptyGalleryBlockIsRejected() throws Exception {
        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Bo'sh albom bloki sinovi uchun",
                                  "blocks": [{"type": "GALLERY", "images": []}]
                                }
                                """),
                        adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.news.galleryBlockEmpty"));
    }

    @Test
    @DisplayName("Bitta yangilikdagi rasmlar soni chegaradan oshmaydi")
    void tooManyImagesAreRejected() throws Exception {
        String token = adminToken();
        // Sinov sozlamasida chegara 3 ta (application-test.properties)
        String a = uploadMedia(token, "a.png");
        String b = uploadMedia(token, "b.png");
        String c = uploadMedia(token, "c.png");
        String d = uploadMedia(token, "d.png");

        mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {
                                  "title": "Rasm chegarasi sinovi uchun sarlavha",
                                  "blocks": [
                                    {"type": "IMAGE", "storedName": "%s"},
                                    {"type": "GALLERY", "images": [
                                      {"storedName": "%s"}, {"storedName": "%s"}, {"storedName": "%s"}
                                    ]}
                                  ]
                                }
                                """.formatted(a, b, c, d)),
                        token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.news.tooManyImages"));
    }

    @Test
    @DisplayName("Muharrir uchun rasm alohida yuklanadi va nomi qaytadi")
    void mediaUploadReturnsStoredName() throws Exception {
        String token = adminToken();

        mockMvc.perform(authorized(
                        multipart("/api/v1/admin/media").file(
                                new MockMultipartFile("file", "muharrir.png", "image/png",
                                        new byte[]{(byte) 0x89, 'P', 'N', 'G'})),
                        token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storedName").isNotEmpty())
                .andExpect(jsonPath("$.originalName").value("muharrir.png"))
                .andExpect(jsonPath("$.url").isNotEmpty());
    }

    @Test
    @DisplayName("Muharrirga rasm bo'lmagan fayl yuklanmaydi")
    void mediaUploadRejectsNonImage() throws Exception {
        mockMvc.perform(authorized(
                        multipart("/api/v1/admin/media").file(
                                new MockMultipartFile("file", "hujjat.pdf", "application/pdf",
                                        new byte[]{'%', 'P', 'D', 'F'})),
                        adminToken()))
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
                                  "blocks": [{"type": "TEXT", "text": "Stipendiya taqsimoti qoidalari haqida batafsil ma'lumot."}],
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
                        {"title": "Ruxsatsiz yangilik", "blocks": [{"type": "TEXT", "text": "Bu yaratilmasligi kerak."}]}
                        """))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- yordamchilar

    /** Muharrir uchun rasm yuklaydi va uning saqlangan nomini qaytaradi. */
    private String uploadMedia(String token, String name) throws Exception {
        String response = mockMvc.perform(authorized(
                        multipart("/api/v1/admin/media").file(
                                new MockMultipartFile("file", name, "image/png",
                                        new byte[]{(byte) 0x89, 'P', 'N', 'G'})),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.storedName");
    }

    private int createNews(String token, String title, boolean published) throws Exception {
        String response = mockMvc.perform(authorized(
                        json(post("/api/v1/admin/news"), """
                                {"title": "%s", "blocks": [{"type": "TEXT", "text": "Sinov uchun yangilik matni."}], "published": %s}
                                """.formatted(title, published)),
                        token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.id");
    }
}
