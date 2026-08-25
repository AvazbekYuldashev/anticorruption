# Korrupsiyaga qarshi kurash markazi portali

**Andijon davlat texnika instituti** korrupsiyaga qarshi kurash markazining
portali: backend (Spring Boot 4, Java 21, PostgreSQL, JWT) va frontend
(React + Vite + TypeScript).

Loyiha [korrupsiya.astiedu.uz](https://korrupsiya.astiedu.uz/asti/) saytining
kengaytirilgan muqobili sifatida yozilgan: o'sha bo'limlarning hammasi bor,
ustiga murojaatni kuzatish, dalil biriktirish, holat tarixi, rollar va
ochiq reyestr qo'shilgan.

```
anticorruption/
├── src/          Backend (Spring Boot) — REST API
└── frontend/     Frontend (React + Vite + TypeScript)
```

Frontend haqida batafsil: [frontend/README.md](frontend/README.md)

## Bo'limlar

| Bo'lim | Nimasi bilan kengaytirilgan |
|---|---|
| **Murojaat qoldirish** | Noyob kuzatuv kodi, dalil fayllari, fakultet/kafedra/fan konteksti, 17 ta aniq kategoriya |
| **Murojaatlar ro'yxati** | Ochiq reyestr: kategoriya, fakultet, maqom va sanalar bilan; qidiruv va filtrlar |
| **Yangiliklar** | Blokli muharrir (sarlavha, formatlangan matn, yakka rasm, albom), qoralama/chop etish, muqova, qidiruv, ko'rishlar hisobi |
| **Xodimlar** | Lavozim, ilmiy daraja, qabul vaqti, surat, tartib raqami |
| **So'rovnomalar** | Cheklanmagan sondagi savol, mavsumiy muddat, qo'lda to'xtatish, qayta o'tkazish, takroriy ovozdan himoya, savollar kesimidagi statistika |
| **Bo'lim haqida** | Admin panelidan tahrirlanadigan istalgan sondagi matnli sahifa |
| **Foydali linklar** | Guruhlangan, tartiblangan havolalar |
| **Reytinglar** | Fakultetlar kesimi statistikadan avtomatik hisoblanadi |

Qo'shimcha: ro'yxatdan o'tish va rollar (`CITIZEN` / `MODERATOR` / `ADMIN`),
holat tarixi, email xabarnomalar, Swagger hujjatlari.

## Ishga tushirish

### 1. Bazani yarating

```bash
"C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres -h localhost anticorruption
```

### 2. Ulanishni sozlang

Sozlamalar [`application.properties`](src/main/resources/application.properties) da.
**Parolni bu faylga yozmang** — u repoga tushadi. Ikkita xavfsiz yo'l bor.

`src/main/resources/application-local.properties` yarating (u `.gitignore` da,
ilova uni avtomatik yuklaydi va asosiy fayldagi qiymatlarni almashtiradi):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/anticorruption
spring.datasource.username=postgres
spring.datasource.password=sizning_parolingiz
```

Yoki muhit o'zgaruvchilari orqali:

```powershell
$env:DB_PASSWORD = "..."; $env:APP_JWT_SECRET = "kamida-32-baytlik-tasodifiy-kalit"; $env:APP_ADMIN_PASSWORD = "..."
```

### 3. Ishga tushiring

```bash
./mvnw spring-boot:run
```

Birinchi ishga tushishda jadvallar avtomatik yaratiladi va bitta administrator
qo'shiladi: `admin@anticorruption.uz` / `Admin12345!` (standart).

**Hujjatlar:** http://localhost:8080/swagger-ui.html

### 4. Birinchi qadam — tuzilmani kiriting

Murojaat shakli fakultet ro'yxatidan to'ldiriladi, shuning uchun avval
fakultetlarni qo'shish kerak:

```bash
curl -X POST http://localhost:8080/api/v1/admin/faculties -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"name\":\"Kompyuter injiniringi fakulteti\",\"code\":\"KIF\"}"
```

## API yo'llari

### Ochiq (token kerak emas)

| Metod | Yo'l | Vazifasi |
|---|---|---|
| `POST` | `/api/v1/auth/register` · `/login` | Ro'yxatdan o'tish, kirish |
| `POST` | `/api/v1/complaints` | Murojaat yuborish |
| `GET` | `/api/v1/complaints/track/{kod}` | Holatni tekshirish |
| `POST` | `/api/v1/complaints/track/{kod}/attachments` | Dalil biriktirish |
| `GET` | `/api/v1/complaints/register` | Ochiq reyestr |
| `GET` | `/api/v1/reference` | Kategoriya, fakultet, maqom ro'yxatlari |
| `GET` | `/api/v1/stats/public` · `/stats/faculty-rating` | Statistika va fakultetlar kesimi |
| `GET` | `/api/v1/news` · `/news/{slug}` | Yangiliklar |
| `GET` | `/api/v1/staff` · `/pages` · `/pages/{slug}` · `/links` | Xodimlar, sahifalar, havolalar |
| `GET` | `/api/v1/polls` · `/polls/{id}` | So'rovnomalar |
| `POST` | `/api/v1/polls/{id}/vote` | Butun so'rovnomaga bir marta javob berish |
| `GET` | `/api/v1/media/{fayl}` | Yangilik va xodim rasmlari |

### Foydalanuvchi uchun

`GET /api/v1/me` · `GET /api/v1/complaints/my` · `GET /api/v1/complaints/my/{id}` ·
`GET /api/v1/attachments/{id}`

### Moderator va administrator

| Metod | Yo'l | Vazifasi |
|---|---|---|
| `GET` | `/api/v1/admin/complaints` | Filtrlangan ro'yxat |
| `PATCH` | `/api/v1/admin/complaints/{id}/status` | Holatni o'zgartirish |
| `PATCH` | `/api/v1/admin/complaints/{id}/assign` | Mas'ul xodimni belgilash |
| `PATCH` | `/api/v1/admin/complaints/{id}/register-visibility` | Reyestrdan yashirish |
| — | `/api/v1/admin/news` · `/staff` · `/pages` · `/links` · `/polls` | Kontent CRUD |
| `GET` | `/api/v1/admin/polls/{id}/statistics` | So'rovnoma hisoboti |
| `PATCH` | `/api/v1/admin/polls/{id}/stopped` | Qo'lda to'xtatish yoki davom ettirish |
| `POST` | `/api/v1/admin/polls/{id}/restart` | Qayta o'tkazish (yangi o'tkazish ochadi) |

### Faqat administrator

`/api/v1/admin/users` (rollar, bloklash) · `/api/v1/admin/faculties` (tuzilma)

### Filtrlash namunasi

```
GET /api/v1/admin/complaints?status=NEW&category=EXAM_BRIBERY&facultyId=1
    &reporterType=STUDENT&unassigned=true&query=matematika&page=0&size=20&sort=createdAt,desc
```

## Tillar (i18n)

Tizim xabarlari to'rtta tilda: **o'zbek (lotin)**, **o'zbek (kirill)**,
**rus**, **ingliz**. Til `?lang=` parametri orqali tanlanadi:

```
GET /api/v1/reference/statuses?lang=ru
GET /api/v1/complaints/track/AC-2026-K7M2Q4?lang=uz-cyrl
```

| Kod | Til | Tarjima fayli |
|---|---|---|
| `uz` (standart) | O'zbekcha, lotin | `i18n/messages.properties` |
| `uz-cyrl` | Ўзбекча, кирилл | `i18n/messages_uz_Cyrl.properties` |
| `ru` | Русский | `i18n/messages_ru.properties` |
| `en` | English | `i18n/messages_en.properties` |

Qo'llab-quvvatlanadigan tillar ro'yxati: `GET /api/v1/reference/languages`.

**Nima tarjima qilinadi:** xatolik xabarlari, validatsiya xabarlari, enum
nomlari (holat, kategoriya, maqom, lavozim, ta'lim shakli, rol) va email
xabarnomalar.

**Nima tarjima qilinmaydi:** foydalanuvchi kiritgan kontent — yangilik matni,
sahifa matni, fakultet nomi, murojaat matni. Ular qaysi tilda yozilgan bo'lsa,
shundayligicha qoladi.

Bir nechta amaliy tafsilot:

- **Accept-Language ataylab e'tiborga olinmaydi.** Faqat `?lang=` ishlaydi,
  shuning uchun bir xil URL har doim bir xil natija beradi — bu keshlash va
  nosozlikni aniqlashni osonlashtiradi.
- **Xatolik javobida `code` maydoni bor** (`error.complaint.notFoundByCode`).
  U tilga bog'liq emas, shuning uchun frontend xabar matniga emas, shu kodga
  qarab qaror qabul qiladi.
- **Email murojaat yuborilgan tilda ketadi.** Til murojaat bilan birga
  saqlanadi: xat asinxron yuborilgani uchun so'rov konteksti u yerda yo'q.
- **Noma'lum til xatolik bermaydi** — standart tilga (o'zbekcha) qaytadi.

Yangi til qo'shish uchun Java kodini o'zgartirish shart emas: `AppLanguage`
enumiga qator qo'shiladi va yangi `messages_XX.properties` yoziladi.

## Muhim qarorlar

**Anonimlik haqiqiy.** `anonymous: true` bo'lsa muallif bog'lanmaydi — hatto
foydalanuvchi tizimga kirgan bo'lsa ham. Ism va telefon saqlanmaydi; faqat
ixtiyoriy email qoladi va u ham faqat xabarnoma uchun.

**Ochiq reyestrda birorta erkin matn maydoni yo'q.** Sarlavha ham, matn ham,
rasmiy javob ham chiqmaydi — faqat oldindan belgilangan qiymatlar (kategoriya,
holat, fakultet) va sanalar. Sabab: erkin matnda murojaatchini oshkor qiladigan
tafsilot bo'lishi mumkin va uni har safar qo'lda tekshirib bo'lmaydi. Bunday
tuzilishda reyestr **konstruksiyasi bo'yicha** xavfsiz. Qo'shimcha ehtiyot
chorasi sifatida xodim alohida murojaatni reyestrdan yashira oladi.

**Ayblanuvchi ism bilan saqlanmaydi.** Faqat lavozim darajasi ko'rsatiladi
(o'qituvchi, kafedra mudiri, dekan). Tekshiruv tugamaguncha aniq shaxsni bazada
ayblab qo'yish noto'g'ri bo'lardi; aniq ism kerak bo'lsa murojaat matnida
qoladi va faqat xodimlarga ko'rinadi.

**Fakultet reytingida yagona ball yo'q.** Murojaat ko'pligi ikki xil narsani
anglatishi mumkin: muammo ko'p, yoki odamlar tizimga ishonadi. Bir raqamga
siqilsa, ochiq fakultet nohaq yomon ko'rinardi. Shuning uchun xom
ko'rsatkichlar beriladi; faqat "yakunlanganlar ulushi" aniq ma'noga ega.

**Fakultet va kafedra — jadval, enum emas.** Ular universitetdan universitetga
farq qiladi va vaqt o'tishi bilan o'zgaradi. Murojaati bor fakultet
o'chirilmaydi, nofaol qilinadi — eski yozuvlar kimga tegishli ekani yo'qolmasin.

**Ochiq va yopiq fayllar alohida papkalarda.** Yangilik rasmi hech qanday
tekshiruvsiz beriladi, dalil fayli esa faqat huquq tekshirilgandan keyin.
Ikkalasi bitta papkada yotsa, ochiq endpoint tasodifan dalillarni ham bera
boshlashi mumkin edi. Diskdagi nom har doim tasodifiy UUID.

**Holat o'tishlari cheklangan.** Yakunlangan murojaat (`RESOLVED`, `REJECTED`)
qayta ochilmaydi — tarix izchil qoladi.

**Ovoz beruvchining IP si saqlanmaydi.** IP va brauzer satri maxfiy tuz bilan
xeshlanadi: bazadan hech kimning IP sini tiklab bo'lmaydi, lekin takroriy ovoz
aniqlanadi.

**Admin o'zini bloklay olmaydi** va o'z rolini pasaytira olmaydi.

**Yangilik mazmuni bloklardan iborat.** Bitta katta matn o'rniga tartiblangan
bloklar (`news_blocks`): `HEADING`, `TEXT`, `IMAGE` va `GALLERY` — shu tufayli
rasm matnning istalgan joyiga qo'yiladi. Qidiruv uchun matn bloklari
`news.body` ga birlashtirib qo'yiladi: SQL da bloklar bo'ylab izlash har safar
birlashtirishni talab qilardi. Bu ustun har saqlashda qayta hosil qilinadi va
qo'lda tahrirlanmaydi.

**Yakka rasm blokning xossasi, albom esa to'plam.** `IMAGE` blokda fayl nomi
blokning o'zida turadi, `GALLERY` da esa rasmlar alohida jadvalda
(`news_block_images`) va o'z tartibiga ega. Ikkalasini bitta to'plamga
birlashtirish ham mumkin edi, lekin unda "yakka" va "albom" farqi faqat
rasmlar soniga qarab taxmin qilinardi — muallif tanlovi esa saqlanishi kerak:
u sahifadagi ko'rinishni belgilaydi.

**Matn formati HTML emas, belgilar bilan yoziladi.** `**qalin**`, `*kursiv*`,
`__tagi chizilgan__`, `~~chizilgan~~`, `[matn](havola)`. HTML saqlansa uni
sahifaga qo'yishdan oldin tozalash kerak bo'lardi va bitta e'tibordan chetda
qolgan teg saytga begona skript kiritish yo'lini ochib berardi. Belgilar esa
hech qachon HTML ga aylanmaydi: `frontend/src/lib/richText.tsx` faqat sanab
o'tilgan elementlarni yasaydi, havolalarda esa `http`, `https` va `mailto` dan
boshqa sxemalar oddiy matn bo'lib qoladi.

**So'rovnoma qayta o'tkazilganda yangi yozuv ochiladi.** Eski o'tkazishning
hisobotini tozalab, hisoblagichlarni noldan boshlash ham mumkin edi, lekin unda
o'tgan mavsum natijalari yo'qolardi. Shu sababli "qayta o'tkazish" savollar
nusxasi bilan yangi so'rovnoma yaratadi va eskisini to'xtatadi: ikkala hisobot
ham to'liq qoladi, `previousPollId` esa ularni bir-biriga bog'lab turadi. Yangi
so'rovnomaning id si boshqa bo'lgani uchun ilgari ovoz berganlar yana ovoz bera
oladi.

**To'xtatilgan va muddati tugagan so'rovnoma bir xil emas.** Muddati tugagani
saytda natijalari bilan qoladi - odamlar nima bilan yakunlanganini ko'rishi
kerak. Qo'lda to'xtatilgani esa saytdan olib tashlanadi, lekin admin panelida
statistikasi joyida turadi. Holat saqlanmaydi: u `active`, `stoppedAt` va
sanalardan hisoblanadi, shuning uchun muddati tugaganda uni kimdir yangilab
turishi shart emas.

**Enum cheklovi ishga tushishda tekislanadi.** `ddl-auto=update` mavjud
`CHECK` cheklovini yangilamaydi, shuning uchun enumga yangi blok turi
qo'shilganda eski bazada yozuv rad etilardi. `NewsBlockTypeConstraintMigration`
cheklovni enumning o'zidan qayta yasaydi — ro'yxat kod bilan ajralib qolmaydi.

## Frontend

```bash
npm install --prefix frontend
npm run dev --prefix frontend
```

Sayt http://localhost:5173 da ochiladi; `/api` so'rovlari backend'ga uzatiladi.
Ommaviy sayt Tailwind bilan, admin panel MUI bilan yozilgan va alohida
bo'lakka ajratilgan (lazy yuklanadi). Batafsil: [frontend/README.md](frontend/README.md)

## Testlar

```bash
./mvnw test
```

77 ta integratsion test, 6 ta sinf: murojaat oqimi, universitet tuzilmasi,
sayt bo'limlari, so'rovnomalar, tarjimalar va kontekst yuklanishi. Testlar
xotiradagi H2 da ishlaydi — PostgreSQL kerak emas.

## Keyingi qadamlar

- **Flyway migratsiyalari.** Hozir `spring.jpa.hibernate.ddl-auto=update`.
  Ishlab chiqarishga chiqishdan oldin migratsiyalarga o'tish kerak.
- **Frontend.** API tayyor, CORS sozlangan (`app.cors.allowed-origins`).
- **Rate limiting** ochiq `POST /api/v1/complaints` va ovoz berish yo'llariga.
- **Refresh token** — hozir faqat 12 soatlik access token bor.
- **Fayllarni antivirus tekshiruvi.**
- **Kontent tarjimalari** — hozir tizim xabarlari to'rt tilda, lekin yangilik
  va sahifa matnlari bitta tilda. Kerak bo'lsa tarjima jadvallari qo'shiladi.

## Ishlab chiqarishga chiqarishdan oldin

- [ ] `APP_JWT_SECRET` — tasodifiy, kamida 32 bayt
- [ ] `APP_POLL_SALT` — tasodifiy
- [ ] `APP_ADMIN_PASSWORD` — standart paroldan voz keching
- [ ] `DB_PASSWORD` — muhit o'zgaruvchisi orqali, faylda emas
- [ ] `APP_CORS_ORIGINS` — faqat haqiqiy frontend manzillari
- [ ] `APP_MAIL_ENABLED=true` va SMTP sozlamalari
- [ ] HTTPS (reverse proxy orqali)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` + Flyway
- [ ] `spring.jpa.show-sql=false`
