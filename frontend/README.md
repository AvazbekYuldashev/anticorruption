# Frontend — React + Vite + TypeScript

Andijon davlat texnika instituti korrupsiyaga qarshi kurash markazi portalining
frontend qismi. Backend uchun asosiy [README](../README.md) ga qarang.

## Ishga tushirish

Backend `http://localhost:8080` da ishlab turishi kerak.

```bash
npm install --prefix frontend
npm run dev --prefix frontend
```

Sayt: http://localhost:5173

Dev serverda barcha `/api` so'rovlari backend'ga uzatiladi
([`vite.config.ts`](vite.config.ts)), shuning uchun brauzer uchun frontend va API
bitta manzilda bo'ladi — CORS umuman ishtirok etmaydi.

Boshqa manzildagi backend uchun:

```bash
VITE_API_TARGET=http://192.168.1.10:8080 npm run dev --prefix frontend
```

## Buyruqlar

| Buyruq | Vazifasi |
|---|---|
| `npm run dev` | Dev server (hot reload) |
| `npm run build` | `tsc -b` + ishlab chiqarish uchun yig'ish |
| `npm run preview` | Yig'ilgan versiyani ko'rish |
| `npm run lint` | oxlint |

## Tuzilma

```
src/
├── api/          Backend bilan aloqa: turlar va endpointlar
│   ├── client.ts     fetch o'rami — JWT, ?lang=, xatolik formati
│   └── types.ts      backend DTO'lariga mos turlar
├── i18n/         4 til (uz, uz-cyrl, ru, en)
├── auth/         AuthContext va marshrut himoyasi
├── components/   Ommaviy sayt UI qismlari (Tailwind)
├── pages/        Ommaviy sahifalar (Tailwind)
├── admin/        Admin panel (MUI) — alohida bo'lakka ajratilgan
└── lib/          Sana/raqam formati, xatolik matni, token saqlash
```

## Muhim qarorlar

**Ikkita uslub tizimi ataylab.** Ommaviy sayt Tailwind bilan yozilgan —
ko'rinish o'ziga xos va bundle kichik. Admin panel MUI bilan — jadval, dialog,
sahifalash tayyor keladi va boshqaruv ekranlari tezroq yig'iladi. MUI global
uslublarni buzmasligi uchun `ScopedCssBaseline` ishlatilgan: uning ta'siri
faqat `/admin` ostidagi daraxt bilan cheklanadi.

**Admin panel lazy yuklanadi.** MUI ~98 KB (gzip) — oddiy tashrifchiga u
kerak emas. `React.lazy` tufayli bu kod faqat xodim `/admin` ga kirganda
yuklab olinadi.

**Validatsiya qoidalari faqat backendda.** Frontend ularni takrorlamaydi:
so'rov 400 qaytarsa, `fields` obyektidagi xabarlar shakl maydonlariga
joylashtiriladi. Xabarlar backenddan joriy tilda keladi, shuning uchun
qoida o'zgarsa bitta joyni tuzatish kifoya. Foydalanuvchi tezkor javob olsin
deb, matn uzunligi hisoblagichi kabi vositalar qo'shilgan.

**Enum nomlari frontendda saqlanmaydi.** Holat, kategoriya, maqom va lavozim
nomlari backenddan `label` maydonida keladi. Til almashganda barcha so'rovlar
bekor qilinadi va qaytadan yuboriladi — shu tufayli tarjimalar ikki joyda
saqlanmaydi.

**Til `?lang=` orqali.** `i18next` tilini o'zgartirish API mijoziga ham
ta'sir qiladi: u har bir so'rovga joriy tilni qo'shadi. Tanlangan til
`localStorage` da saqlanadi.

**Rang mashina qiymatiga bog'lanadi.** Holat nishonining rangi `status`
(masalan `RESOLVED`) bo'yicha tanlanadi, matn bo'yicha emas — til
o'zgarganda ranglar joyida qoladi.

**Logotip loyiha ichida.** `public/brand/` da saqlanadi va tashqi saytdan
yuklanmaydi: institut sayti o'zgarsa ham portal ko'rinishi buzilmaydi.
Asl 2095×2095 (594 KB) tasvir 256 va 64 px o'lchamlarga keltirilgan.

## Seans

Token brauzerda saqlanmaydi. Backend uni `HttpOnly` cookie'da beradi, ya'ni
sahifadagi JavaScript unga umuman kira olmaydi — `localStorage` bilan XSS
holatida token o'g'irlanishi mumkin edi.

- `src/lib/session.ts` da token yo'q: u yerda faqat CSRF tokenini cookie'dan
  o'qish va "seans bor" belgisi. Belgi token emas — usiz ilova har ochilishida
  anonim mehmon uchun ham `/me` va `/auth/refresh` so'rovlarini yuborardi.
- `src/api/client.ts` yozuv so'rovlariga `X-XSRF-TOKEN` sarlavhasini qo'yadi
  (cookie bo'lmasa avval `/auth/csrf` ni chaqiradi) va har so'rovni
  `credentials: 'include'` bilan yuboradi.
- Kirish tokeni 15 daqiqalik, shuning uchun **401 odatiy hol** — u "chiqib
  ketdingiz" degani emas. Mijoz seansni yangilab, so'rovni bir marta
  qaytaradi. Bir vaqtda kelgan bir nechta 401 bitta yangilash so'rovini
  yuboradi (aks holda token aylanishi bir-birini bekor qilardi).

## Matn formatlash

Yangilik bloklari, "Bo'lim haqida" matni va xodim biografiyasi formatlash
paneli bilan tahrirlanadi (`RichTextField`): qalin, kursiv, tagli, chizilgan
va havola.

Matn **HTML sifatida saqlanmaydi** — faqat `**qalin**` kabi belgilar.
Shuning uchun tozalash (sanitizatsiya) umuman kerak emas: `renderRichText`
faqat o'zi biladigan elementlarni yasaydi, qolgan hamma narsa oddiy matn
bo'lib qoladi. Havola manzillari ham tekshiriladi — `javascript:` va `data:`
kabi sxemalar havola bo'lmaydi.

## Testlar

```bash
npm test          # bir marta
npm run test:watch
```

Vitest + Testing Library, muhit — jsdom. Sozlama `vitest.config.ts` da
(`vite.config.ts` dan alohida: testlarga dev-proksi va Tailwind kerak emas).

Testlar `tsconfig.app.json` dan chiqarilgan — shu tufayli `npm run build`
vitest o'rnatilmagan mashinada ham ishlaydi.

Hozir qoplangan: seans qatlami (`session.ts`, `client.ts`) — CSRF sarlavhasi,
401 dan keyin avtomatik yangilash, bir vaqtdagi so'rovlarning bitta yangilashga
ulanishi, kirish so'rovi uchun istisno, xatolik kodlarining `ApiError` ga
o'tishi.

## Keyingi qadamlar

- **Komponent testlari.** Murojaat formasi va admin paneli oqimlari hali
  qoplanmagan.

