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
├── backend/      Spring Boot 4 · Java 21 · PostgreSQL — REST API
└── frontend/     React · Vite · TypeScript — sayt va admin panel
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
| **Bo'lim haqida** | Admin panelidan tahrirlanadigan sarlavha, matn, vazifalar ro'yxati va maqsad |
| **Reytinglar** | Fakultetlar kesimi statistikadan avtomatik hisoblanadi |

Qo'shimcha: rollar (`CITIZEN` / `MODERATOR` / `ADMIN`), holat tarixi,
email xabarnomalar, Swagger hujjatlari.

> **Eslatma.** Statik sahifalar (`/pages`) va foydali havolalar (`/links`)
> loyihada yo'q: ular `9b97ffa` commitida entity, endpoint va jadval bilan
> birga olib tashlangan. O'sha commit xabari faqat papkalarni ajratish haqida
> gapiradi, shuning uchun bu ataylab qilinganmi yoki tasodifanmi — aniq emas.

## Ishga tushirish

### 1. Bazani yarating

```bash
"C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres -h localhost anticorruption
```

### 2. Ulanishni sozlang

Sozlamalar [`application.properties`](backend/src/main/resources/application.properties) da.
**Parolni bu faylga yozmang** — u repoga tushadi. Ikkita xavfsiz yo'l bor.

`backend/src/main/resources/application-local.properties` yarating (u `.gitignore` da,
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
cd backend
./mvnw spring-boot:run
```

Birinchi ishga tushishda jadvallar avtomatik yaratiladi va ikkita hisob
qo'shiladi (standart qiymatlar):

| Rol | Email | Parol |
|---|---|---|
| Administrator | `admin@anticorruption.uz` | `Admin12345!` |
| Moderator | `moderator@anticorruption.uz` | `Moderator12345!` |

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
| `POST` | `/api/v1/auth/login` | Tizimga kirish (ochiq ro'yxatdan o'tish yo'q) |
| `POST` | `/api/v1/auth/refresh` | Seansni yangilash (yangilash cookie'si bo'yicha) |
| `POST` | `/api/v1/auth/logout` | Chiqish |
| `GET` | `/api/v1/auth/csrf` | CSRF tokenini o'rnatish |
| `POST` | `/api/v1/complaints` | Murojaat yuborish |
| `GET` | `/api/v1/complaints/track/{kod}` | Holatni tekshirish |
| `POST` | `/api/v1/complaints/track/{kod}/attachments` | Dalil biriktirish |
| `GET` | `/api/v1/reference` | Kategoriya, fakultet, maqom ro'yxatlari |
| `GET` | `/api/v1/stats/public` · `/stats/faculty-rating` | Statistika va fakultetlar kesimi |
| `GET` | `/api/v1/news` · `/news/{slug}` | Yangiliklar |
| `GET` | `/api/v1/staff` · `/about` | Xodimlar va "Bo'lim haqida" |
| `GET` | `/api/v1/polls` · `/polls/{id}` | So'rovnomalar |
| `POST` | `/api/v1/polls/{id}/vote` | Butun so'rovnomaga bir marta javob berish |
| `GET` | `/api/v1/media/{fayl}` | Yangilik va xodim rasmlari |

### Foydalanuvchi uchun

`GET /api/v1/me` · `PUT /api/v1/me` (ism va login) · `POST /api/v1/me/password` ·
`GET /api/v1/complaints/my` · `GET /api/v1/complaints/my/{id}` ·
`GET /api/v1/attachments/{id}`

### Moderator va administrator

| Metod | Yo'l | Vazifasi |
|---|---|---|
| `GET` | `/api/v1/admin/complaints` | Filtrlangan ro'yxat |
| `PATCH` | `/api/v1/admin/complaints/{id}/status` | Holatni o'zgartirish |
| `PATCH` | `/api/v1/admin/complaints/{id}/assign` | Mas'ul xodimni belgilash |
| `PATCH` | `/api/v1/admin/complaints/{id}/register-visibility` | Reyestrdan yashirish |
| — | `/api/v1/admin/news` · `/staff` · `/about` · `/polls` | Kontent CRUD |
| `GET` | `/api/v1/admin/polls/{id}/statistics` | So'rovnoma hisoboti |
| `PATCH` | `/api/v1/admin/polls/{id}/stopped` | Qo'lda to'xtatish yoki davom ettirish |
| `POST` | `/api/v1/admin/polls/{id}/restart` | Qayta o'tkazish (yangi o'tkazish ochadi) |

### Faqat administrator

`/api/v1/admin/users` (yaratish, rollar, bloklash, o'chirish) ·
`/api/v1/admin/faculties` (tuzilma) ·
`GET /api/v1/complaints/register` (murojaatlar reyestri - xodimlar uchun)

### Hisoblar

Ochiq ro'yxatdan o'tish yo'q: barcha hisoblarni administrator ochadi va
dastlabki parolni o'zi beradi. Egasi tizimga kirgach login (email) va
parolini o'zi almashtiradi (`PUT /api/v1/me`, `POST /api/v1/me/password`).

Murojaat yuborish uchun hisob umuman kerak emas - u anonim ham yuboriladi.

Birinchi ishga tushishda bitta administrator va bitta moderator yaratiladi:
`admin@anticorruption.uz` / `Admin12345!` va
`moderator@anticorruption.uz` / `Moderator12345!` (standart) - parollarni
darhol almashtiring yoki `APP_ADMIN_PASSWORD` va `APP_MODERATOR_PASSWORD`
orqali bering. Moderator kerak bo'lmasa `app.moderator.email` ni bo'sh
qoldiring - u holda yaratilmaydi.

Bazada shu emailli hisob allaqachon bo'lsa, u qayta yozilmaydi: paroli ham,
roli ham o'zgarmaydi.

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

**Backend** — 121 ta integratsion test, 8 ta sinf: seans oqimi, murojaat
oqimi, universitet tuzilmasi, sayt bo'limlari, so'rovnomalar, foydalanuvchilar,
tarjimalar va kontekst yuklanishi.

```bash
cd backend
./mvnw test
```

Testlar xotiradagi H2 da ishlaydi — PostgreSQL kerak emas. Sxemani u yerda
Hibernate yaratadi, ya'ni **migratsiyalar testlarda sinalmaydi**: yangi
migratsiya yozilsa, uni haqiqiy PostgreSQL da bir marta tekshirib ko'rish
kerak (ishlab chiqarishda `ddl-auto=validate`).

**Frontend** — 21 ta test, 3 ta fayl (Vitest + jsdom): seans qatlami (CSRF
sarlavhasi, 401 dan keyin avtomatik yangilash, bir vaqtdagi so'rovlarning
bitta yangilashga ulanishi), API mijozi va matn formatlash.

```bash
cd frontend
npm test
```

> **Bo'shliq.** `@testing-library/react` o'rnatilgan, lekin hali birorta
> testda ishlatilmagan — komponent va foydalanuvchi oqimi testlari yozilmagan.
> Hozirgi qoplama funksiya darajasida.

## Ma'lumotlar bazasi migratsiyalari

Sxemani Hibernate emas, **Flyway** boshqaradi:
`backend/src/main/resources/db/migration/`. Ilova ishga tushganda migratsiyalar
avtomatik qo'llanadi, Hibernate esa faqat entity va jadval mosligini
tekshiradi (`ddl-auto=validate`).

Hozirgi migratsiyalar:

| Fayl | Nimani qo'shadi |
|---|---|
| `V1__baseline.sql` | Dastlabki 17 ta jadval |
| `V2__refresh_tokens.sql` | Yangilash tokenlari |
| `V3__news_language.sql` | Yangilikka til va `translation_group` |
| `V4__content_translations.sql` | "Bo'lim haqida" va xodim tarjimalari |
| `V5__refresh_token_revocation_reason.sql` | Token nega bekor qilingani |

**Entity o'zgartirilsa** yangi migratsiya fayli yozilishi shart:

```sql
-- backend/src/main/resources/db/migration/V6__xodimga_telegram_qoshildi.sql
alter table staff_members add column telegram varchar(120);
```

Qoidalar:

- chop etilgan migratsiyani tahrirlab bo'lmaydi — Flyway uning nazorat
  yig'indisini saqlaydi va o'zgargani darrov xatolik beradi; xatoni
  yangi migratsiya bilan tuzatiladi;
- mavjud (migratsiyasiz yaratilgan) baza birinchi ishga tushishda
  avtomatik "baseline" qilinadi — jadvallar qaytadan yaratilmaydi;
- sinovlar xotiradagi H2 da ishlaydi, u yerda sxemani Hibernate yaratadi.

Ishlab chiqish paytida tez tajriba qilish uchun (tavsiya etilmaydi):
`DDL_AUTO=update` muhit o'zgaruvchisi.

## Ishlab chiqarishga chiqarish

### 1. Profil va maxfiy qiymatlar

```bash
java -jar anticorruption.jar --spring.profiles.active=prod
```

`prod` profili ([application-prod.properties](backend/src/main/resources/application-prod.properties)):
sxema tekshiruvi, Swagger yopiq, loglar faylga yoziladi va aylanadi,
javoblar siqiladi, so'rov chegarasi yoqilgan.

Maxfiy qiymatlar faqat muhit o'zgaruvchilari orqali beriladi:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/anticorruption
export DB_USERNAME=anticorruption
export DB_PASSWORD='...'
export APP_JWT_SECRET="$(openssl rand -base64 48)"
export APP_POLL_SALT="$(openssl rand -base64 32)"
export APP_ADMIN_PASSWORD='...'
export APP_CORS_ORIGINS=https://korrupsiya.astiedu.uz
export APP_STORAGE_LOCATION=/var/lib/anticorruption/uploads
```

Biror qiymat ishlab chiqish holatida qolsa, ilova **ishga tushmaydi**:
[`ProductionSafetyCheck`](backend/src/main/java/api/anticorruption/config/ProductionSafetyCheck.java)
JWT kaliti, ovoz tuzi, administrator paroli, CORS manzillari va
`ddl-auto` ni tekshiradi.

### 2. Teskari proksi va HTTPS

Ilova HTTP da 8080 portda turadi, TLS ni nginx tugatadi. `prod` profilida
`server.forward-headers-strategy=framework` yoqilgan — mijozning haqiqiy
manzili `X-Forwarded-For` dan olinadi (so'rov chegarasi ham shunga tayanadi).

```nginx
server {
    listen 443 ssl http2;
    server_name korrupsiya.astiedu.uz;

    # Frontend: `npm run build` natijasi
    root /var/www/anticorruption;
    index index.html;

    location / {
        try_files $uri /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        # Tashqaridan kelgan qiymat almashtiriladi - aks holda chegarani aldash mumkin
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 60m;
    }
}
```

Frontendni yig'ish: `cd frontend && npm ci && npm run build` → `dist/`
papkasini `/var/www/anticorruption` ga qo'ying.

### 3. Xizmat sifatida ishga tushirish (systemd)

```ini
[Unit]
Description=Korrupsiyaga qarshi kurash portali
After=network.target postgresql.service

[Service]
User=anticorruption
EnvironmentFile=/etc/anticorruption/env
ExecStart=/usr/bin/java -jar /opt/anticorruption/anticorruption.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

`/etc/anticorruption/env` faylining huquqlari `600` bo'lsin — maxfiy
qiymatlar shu yerda.

### 4. Zaxira nusxa

```bash
pg_dump -U anticorruption anticorruption | gzip > /backup/db-$(date +%F).sql.gz
tar czf /backup/uploads-$(date +%F).tar.gz /var/lib/anticorruption/uploads
```

Bazadan tashqari **yuklangan fayllar** ham zaxiralanishi kerak: ular
bazada emas, diskda yotadi.

### 5. Chiqarishdan oldingi ro'yxat

- [x] Flyway migratsiyalari, `ddl-auto=validate`
- [x] Swagger ishlab chiqarishda yopiq
- [x] Xavfsizlik sarlavhalari: HSTS, nosniff, frame-deny, CSP, referrer-policy
- [x] So'rov chegarasi (kirish, murojaat yuborish, ovoz berish)
- [x] Fayl mazmuni turga mos kelishini tekshirish (magic bytes)
- [x] Maxfiy qiymatlar tekshiruvi (ishga tushishda)
- [x] Loglar faylga, `show-sql` o'chirilgan
- [ ] `APP_MAIL_ENABLED=true` va SMTP sozlamalari
- [ ] HTTPS sertifikati (certbot)
- [ ] Zaxira nusxa jadvali (cron)
- [ ] Monitoring: `/actuator/health` ni kuzatuvchi xizmatga ulash

## Seans va xavfsizlik

Tokenlar brauzerda `localStorage` da emas, **`HttpOnly` cookie** da saqlanadi:
sahifadagi JavaScript ularga umuman kira olmaydi, ya'ni XSS topilgan taqdirda
ham token o'g'irlanmaydi.

| Cookie | Muddat | Yo'l | Vazifasi |
|---|---|---|---|
| `ac_access` | 15 daqiqa | `/` | Har bir so'rovda foydalanuvchini aniqlaydi |
| `ac_refresh` | 14 kun | `/api/v1/auth` | Yangi kirish tokeni olish uchun |
| `XSRF-TOKEN` | seans | `/` | CSRF tokeni (bu bittasi `HttpOnly` emas — uni interfeys o'qiydi) |

- Kirish tokeni **qisqa muddatli**, chunki uni bekor qilib bo'lmaydi. Yangilash
  tokeni esa bazada turadi (faqat SHA-256 xeshi) va istalgan payt bekor qilinadi.
- Har bir yangilashda token **almashadi** (rotation). **Aylantirilgan** token
  qaytadan kelsa — bu o'g'irlanish alomati (mijozda uning o'rnida yangisi
  turishi kerak edi): o'sha foydalanuvchining barcha seanslari uziladi.
- Ataylab bekor qilingan token (chiqish, parol almashtirish) qaytib kelsa esa
  shunchaki rad etiladi — bu eskirgan cookie, boshqa seanslarga tegilmaydi.
  Shuning uchun token nega bekor qilingani bazada saqlanadi
  (`refresh_tokens.revoked_reason`, `V5` migratsiyasi). Aks holda parolini
  almashtirgan odam boshqa qurilmasining navbatdagi yangilashi tufayli o'zi
  ham tizimdan chiqib qolardi.
- Parol almashtirilganda boshqa qurilmalardagi seanslar uziladi, joriy qurilma
  esa yangi cookie'lar oladi.
- Cookie avtomatik yuborilgani uchun **CSRF himoyasi majburiy**: yozuv so'rovi
  `XSRF-TOKEN` cookie'sidagi qiymatni `X-XSRF-TOKEN` sarlavhasida qaytarishi
  kerak. Interfeys buni o'zi qiladi (`frontend/src/api/client.ts`).
  Anonim ochiq yozuvlar — murojaat yuborish, dalil biriktirish, ovoz berish —
  hech qanday seansga tayanmaydi, shuning uchun ular ro'yxatdan chiqarilgan.
- Ishlab chiqarishda `app.auth.secure=true` (faqat HTTPS). Frontend API dan
  boshqa domenda tursa `APP_COOKIE_SAME_SITE=None` kerak bo'ladi — u faqat
  `secure=true` bilan ishlaydi. `ProductionSafetyCheck` buni tekshiradi.

| Endpoint | Vazifasi |
|---|---|
| `POST /api/v1/auth/login` | Kirish; ikkala cookie'ni o'rnatadi |
| `POST /api/v1/auth/refresh` | Yangilash cookie'si bo'yicha yangi seans |
| `POST /api/v1/auth/logout` | Tokenni bekor qiladi, cookie'larni o'chiradi |
| `GET /api/v1/auth/csrf` | `XSRF-TOKEN` cookie'sini o'rnatadi |

Brauzerdan tashqari mijozlar uchun `Authorization: Bearer <token>` sarlavhasi
ham qabul qilinaveradi — cookie topilmasa o'sha ishlatiladi.

## So'rov chegarasi

Ochiq yozuv amallari (kirish, murojaat yuborish, ovoz berish) bitta IP dan
keladigan oqimdan himoyalangan. Hisob ikki joyda yuritilishi mumkin:

| `app.rate-limit.store` | Qachon |
|---|---|
| `memory` (standart) | Ilova bitta nusxada ishlaydi. Redis kerak emas. |
| `redis` | Bir nechta nusxa. Chegara barcha nusxalar uchun umumiy bo'ladi. |

```bash
APP_RATE_LIMIT_STORE=redis REDIS_HOST=127.0.0.1 REDIS_PORT=6379 java -jar app.jar
```

Redis javob bermay qolsa ilova to'xtamaydi: chegara vaqtincha xotirada
hisoblanadi va logga ogohlantirish yoziladi. Chegara — himoya qatlami,
ruxsat tekshiruvi emas, shuning uchun uning uzilishi butun saytni
to'xtatmasligi kerak.

## Kontent tillari

Tizim xabarlari to'rt tilda (`uz`, `uz-cyrl`, `ru`, `en`) — ular
`src/main/resources/i18n/messages*.properties` da. Yangiliklar esa boshqacha
ishlaydi: **har bir til uchun alohida maqola**.

Sabab amaliy — ruscha maqola ko'pincha o'zbekchasidan qisqaroq bo'ladi va
rasmlar ham boshqacha tanlanadi. "Bitta maqola — to'rtta tarjima maydoni"
modeli muharrir uchun noqulay bo'lardi.

Nusxalar `translation_group` ustuni orqali bog'lanadi:

- Yangi maqola o'ziga yangi guruh ochadi.
- Admin panelida "Tarjimalar" bo'limidan boshqa tilda nusxa yaratiladi —
  u o'sha guruhga qo'shiladi (`translationOf` maydoni).
- Bitta guruhda bir tildan faqat bitta nusxa bo'ladi.

Ro'yxat va qidiruv `?lang=` bo'yicha filtrlanadi, lekin **tarjimasi yo'q
maqola ro'yxatdan tushib qolmaydi**: so'ralgan tilda nusxasi bo'lmagan
guruhlar uchun o'zbekcha varianti ko'rsatiladi. Aks holda rus tiliga o'tgan
odam yarim bo'sh sayt ko'rardi.

Maqola sahifasida `translations` ro'yxati qaytadi — sayt shu orqali
"Boshqa tillarda" havolalarini chiqaradi va o'quvchini ro'yxatga emas,
aynan shu maqolaning tarjimasiga olib boradi.

### "Bo'lim haqida" va xodimlar — tarjima jadvali

Bu ikkisi boshqacha ishlaydi. Ular struktura sifatida bitta: sarlavha, matn,
vazifalar ro'yxati har tilda ham shu tartibda qoladi, faqat matn almashadi.
Shuning uchun bu yerda alohida yozuv emas, **tarjima jadvali** to'g'ri keladi
(`about_section_translations`, `staff_member_translations`).

- Asosiy til (o'zbekcha) matni asosiy jadvalda qoladi — tarjima umuman
  qo'shilmasa ham sayt ilgarigidek ishlaydi.
- **Fallback maydon darajasida**: tarjimada bo'sh qolgan sarlavha o'zbekchasini
  ko'rsatadi, tarjima qilingan qismi esa o'z tilida chiqadi. Butun sahifani
  "tarjima bor/yo'q" deb ikkiga bo'lish yarim tayyor tarjimani foydasiz
  qilib qo'yardi.
- Xodimlarda faqat matn tarjima qilinadi. Telefon, email, surat, tartib va
  faollik holati tilga bog'liq emas — ular bir joyda kiritiladi.
- Admin panelida til varaqalari bor; hammasi bitta "Saqlash" bilan yuboriladi.
  Bo'sh varaq saqlanmaydi.

Admin javobida (`GET /api/v1/admin/about`, `/admin/staff`) asosiy matn va
barcha tarjimalar birga keladi; saytga esa faqat so'ralgan tildagi natija
qaytadi.

## Fayllar va antivirus

Yuklangan fayl uch bosqichdan o'tadi:

1. **MIME turi** ruxsat etilganlar ro'yxatida bo'lishi kerak
   (`app.storage.allowed-content-types`).
2. **Fayl imzosi** ko'rsatilgan turga mos kelishi kerak — `.jpg` deb atalgan
   bajariladigan fayl shu yerda to'xtaydi (`FileSignatures`).
3. **Antivirus** — yoqilgan bo'lsa, fayl `clamd` ga yuboriladi (INSTREAM).

Antivirus standart holatda **o'chiq**: ishlab chiqish uchun ClamAV o'rnatish
shart emas. Serverda yoqish:

```bash
apt install clamav-daemon        # port 3310
APP_ANTIVIRUS_ENABLED=true java -jar app.jar --spring.profiles.active=prod
```

| Sozlama | Standart | Ma'nosi |
|---|---|---|
| `app.antivirus.enabled` | `false` | Tekshiruv yoqilganmi |
| `app.antivirus.host` / `.port` | `127.0.0.1` / `3310` | clamd manzili |
| `app.antivirus.fail-closed` | `true` | clamd javob bermasa fayl rad etiladi |

`fail-closed=true` ataylab standart: tekshirilmagan faylni o'tkazib yuborish
antivirusni yoqishning ma'nosini yo'qotardi. Agar murojaat yuborishning
uzilib qolgani xavfliroq deb hisoblasangiz, uni `false` qiling — shunda fayl
o'tadi va logga ogohlantirish yoziladi.

Zararli fayl topilsa u diskdan darhol o'chiriladi va bazaga umuman
yozilmaydi. Tekshiruv fayl diskka yozilgandan keyin, oqim orqali bajariladi —
katta fayl ham xotirani band qilmaydi.

Yangi kutubxona qo'shilmagan: clamd protokoli sodda (buyruq, bo'laklar,
bitta qatorli javob), shuning uchun u to'g'ridan-to'g'ri soket orqali
bajariladi (`AntivirusScanner`).

## Keyingi qadamlar

Ochiq qolgan ishlar:

- **Komponent testlari** — `@testing-library/react` o'rnatilgan, lekin
  ishlatilmagan. Asosiy oqimlar (kirish, murojaat yuborish, ovoz berish)
  hali test bilan qoplanmagan.
- **Statik sahifalar** — qaytarilsinmi yoki yo'qligicha qolsinmi, hal
  qilinmagan (yuqoridagi eslatmaga qarang).

Keyingi nomzodlar:

- **Zaxira nusxa** — baza va `uploads/` uchun jadval bo'yicha nusxa olish.
- **Monitoring** — `/actuator/health` ni kuzatuvchi xizmatga ulash.
- **Antivirusni yoqish** — kod tayyor, lekin `clamd` ko'tarilib,
  `APP_ANTIVIRUS_ENABLED=true` qilinmagan.
- **Redis** — chegara ombori kodda bor, standart holatda hamon `memory`.
