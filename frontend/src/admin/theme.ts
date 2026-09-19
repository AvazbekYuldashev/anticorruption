/*
 * Admin panelining ko'rinish qiymatlari bir joyda.
 *
 * Ranglar `index.css` dagi `--color-brand-*` shkalasidan olingan: MUI CSS
 * o'zgaruvchilarini o'qimaydi, shuning uchun qiymatlar shu yerda takrorlanadi.
 * Ikkalasi bir vaqtda o'zgarishi kerak.
 */

export const brand = {
  50: '#eff6ff',
  100: '#dbeafe',
  200: '#bfdbfe',
  300: '#93c5fd',
  400: '#60a5fa',
  500: '#2563eb',
  600: '#1d4ed8',
  700: '#1e40af',
  800: '#1e3a8a',
  900: '#172554',
} as const;

/**
 * Harakat.
 *
 * <p>Bosiladigan har bir element javob qaytaradi: ustiga borilganda ozgina
 * ko'tariladi, bosilganda joyiga qaytadi. Bu "tugma ishladi" degan his
 * beradi - rangning o'zgarishi yolg'iz buni yetarlicha ko'rsatmaydi.
 *
 * <p>Masofalar ataylab kichik (1-2 px): katta sakrash boshqaruv panelida
 * o'nlab tugma bo'lganda bezovta qiladi.
 */
export const motion = {
  /** Tez, lekin sezilarli. */
  quick: 'transform .16s cubic-bezier(.2, .8, .3, 1)',
  soft: 'box-shadow .22s ease, background-color .18s ease, border-color .18s ease',
} as const;

/**
 * Harakatni kamaytirishni so'raganlar uchun.
 *
 * <p>Vestibulyar buzilishlarda siljish va sakrash noqulaylik tug'diradi,
 * shuning uchun tizim sozlamasi hurmat qilinadi: rang o'zgarishi qoladi,
 * harakat esa o'chadi.
 */
export const noMotion = {
  '@media (prefers-reduced-motion: reduce)': {
    transition: 'none',
    animation: 'none',
    '&:hover': { transform: 'none' },
    '&:active': { transform: 'none' },
  },
} as const;

/** Kartochka soyasi: sezilarli, lekin ekranda o'nlab kartochka bo'lsa ham shovqin qilmaydi. */
export const softShadow =
  '0 1px 2px rgba(15, 23, 42, 0.04), 0 12px 28px -18px rgba(15, 23, 42, 0.28)';

export const hoverShadow =
  '0 2px 4px rgba(15, 23, 42, 0.05), 0 18px 36px -20px rgba(15, 23, 42, 0.38)';

/** Yon menyu foni - ommaviy saytning bosh ekrani bilan bir xil gradient. */
export const sidebarBackground = `linear-gradient(168deg, ${brand[900]} 0%, ${brand[800]} 55%, ${brand[700]} 100%)`;

/**
 * Holat ranglari - `index.css` dagi `--color-status-*` bilan bir xil.
 *
 * <p>MUI ning o'z "success/warning/error" ranglari boshqacha (masalan yashili
 * #2e7d32). Ular almashtirilmasa admin panelida ommaviy saytdagidan boshqa
 * yashil va boshqa qizil paydo bo'lardi.
 */
export const status = {
  good: '#0ca30c',
  warning: '#fab219',
  serious: '#ec835a',
  critical: '#d03b3b',
} as const;

/**
 * Nishonlar (Chip) uchun yumshoq ohanglar: to'ldirilgan quyuq rang o'rniga
 * shaffof fon + quyuq yozuv.
 *
 * <p>Yozuv ranglari fon ustida 4.5:1 dan yuqori bo'lishi uchun asosiy
 * tokendan quyuqroq olingan - masalan #0ca30c yumshoq yashil fonda 2.9:1
 * chiqadi, #0a7a0a esa 4.8:1.
 */
export const chipTones = [
  { color: 'default', bg: 'rgba(100, 116, 139, 0.14)', fg: '#334155' },
  { color: 'primary', bg: 'rgba(29, 78, 216, 0.12)', fg: brand[600] },
  { color: 'success', bg: 'rgba(12, 163, 12, 0.14)', fg: '#0a7a0a' },
  { color: 'warning', bg: 'rgba(250, 178, 25, 0.22)', fg: '#92400e' },
  { color: 'secondary', bg: 'rgba(236, 131, 90, 0.20)', fg: '#9a3412' },
  { color: 'error', bg: 'rgba(208, 59, 59, 0.14)', fg: '#b91c1c' },
] as const;

/** Jadval sarlavhasi tasmasi - qatorlardan ajralib turishi uchun. */
export const tableHeadBackground = 'linear-gradient(180deg, #f2f6fc 0%, #e9eff9 100%)';

/**
 * Ro'yxatdagi asosiy yozuv: murojaat sarlavhasi, yangilik nomi, xodim ismi.
 *
 * <p>Standart jadvalda hamma katak bir xil og'irlikda edi va ko'z qaysi
 * ustun muhimligini topa olmasdi. Asosiy ustun quyuqroq va yo'g'onroq
 * bo'lsa, qatorni bir qarashda o'qish mumkin.
 */
export const listPrimaryText = {
  fontSize: 14.5,
  fontWeight: 600,
  color: '#0f172a',
  lineHeight: 1.4,
} as const;

/** Yordamchi yozuv: sana, email, manzil - asosiysidan bir pog'ona pastda. */
export const listSecondaryText = {
  fontSize: 12.5,
  color: '#64748b',
  lineHeight: 1.4,
} as const;

/**
 * Mashina qiymati (kuzatuv kodi, slug) uchun kichik yorliq.
 *
 * <p>Yalang'och monospace matn qator ichida yo'qolib ketardi; yengil fon
 * uni "bu nusxa olinadigan qiymat" deb ajratib turadi.
 */
export const codePill = {
  display: 'inline-block',
  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
  fontSize: 12,
  fontWeight: 600,
  letterSpacing: '0.01em',
  px: 0.85,
  py: 0.35,
  borderRadius: 1.5,
  bgcolor: 'rgba(148, 163, 184, 0.16)',
  color: '#475569',
  whiteSpace: 'nowrap',
} as const;

/**
 * Sahifa sarlavhasi tasmasi.
 *
 * <p>Yon menyu bilan bir xil gradient: ekranning chap va yuqori chekkasi
 * bitta to'q ramka hosil qiladi va oq ish maydoni uning ichida turadi.
 */
export const heroBackground = `linear-gradient(115deg, ${brand[900]} 0%, ${brand[700]} 52%, ${brand[500]} 100%)`;

export const heroShadow = '0 18px 38px -22px rgba(23, 37, 84, 0.85)';

/** Boshqaruv elementlari (filtr, qidiruv) uchun yengil ko'kimtir fon. */
export const panelWash = 'linear-gradient(135deg, #ffffff 0%, #f2f7ff 100%)';

/**
 * Oq kartochka ichidagi blok foni (masalan so'rovnomaning bitta savoli).
 *
 * <p>`panelWash` dan quyuqroq: u kanvas ustida turadi, bu esa oq kartochka
 * ustida - susroq bo'lsa umuman ko'rinmasdi.
 */
export const nestedWash = 'linear-gradient(135deg, #f8fafc 0%, #eaf1fd 100%)';

/** Ish maydonining foni: tekis kulrang emas, ikki tomondan yumshoq ko'k yorug'lik. */
export const canvasBackground =
  'radial-gradient(1100px 420px at 82% -12%, rgba(37, 99, 235, 0.13), transparent 62%), ' +
  'radial-gradient(820px 380px at -8% 4%, rgba(30, 58, 138, 0.10), transparent 58%)';

/**
 * Kartochka ohanglari.
 *
 * <p>`value` - katta raqamning rangi. Bu yerda sariq va to'q sariq YO'Q:
 * ular oq fonda 3:1 dan past chiqadi (qarang `index.css`). Ogohlantirish
 * ohangi nishon va yuqoridagi chiziq orqali beriladi, raqam esa neytral
 * qoladi - ma'no baribir yozuvda turadi, rang yolg'iz tashimaydi.
 */
export type Tone = {
  /** Kartochkaning yumshoq fon gradienti. */
  wash: string;
  /** Yuqoridagi ingichka rangli chiziq va nishon foni. */
  accent: string;
  /** Katta raqam rangi. */
  value: string;
  /** Nishon (ikonka doirasi) matn rangi. */
  icon: string;
};

export const tones = {
  /** Umumiy son - asosiy brand ohangi. */
  brand: {
    wash: `linear-gradient(135deg, ${brand[50]} 0%, ${brand[100]} 100%)`,
    accent: `linear-gradient(135deg, ${brand[600]}, ${brand[500]})`,
    value: brand[600],
    icon: '#ffffff',
  },
  /*
   * Ko'rib chiqilmoqda - kutish ohangi (--color-status-warning).
   *
   * Foni qasddan boshqalardan susroq: bir xil to'yinganlikda sariq qatordagi
   * eng baland rang bo'lib chiqadi va ko'z avval unga tushadi. Holbuki bu
   * kartochka boshqalaridan muhimroq emas.
   */
  warning: {
    wash: 'linear-gradient(135deg, #fffdf5 0%, #fdf3d7 100%)',
    accent: 'linear-gradient(135deg, #f59e0b, #fab219)',
    value: '#1e293b',
    icon: '#ffffff',
  },
  /** Hal qilingan - ijobiy ohang (--color-status-good). */
  good: {
    wash: 'linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%)',
    accent: 'linear-gradient(135deg, #0ca30c, #22c55e)',
    value: '#0ca30c',
    icon: '#ffffff',
  },
  /** Davr kesimi - bezak ohangi, holat rangi emas. */
  indigo: {
    wash: 'linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%)',
    accent: 'linear-gradient(135deg, #4f46e5, #6366f1)',
    value: '#4338ca',
    icon: '#ffffff',
  },
  /*
   * Muddat - neytral grafit. Bu kartochkadagi son boshqa turdagi o'lchov
   * (kun, dona emas), shuning uchun rangi ham qolganlaridan ajralib turadi.
   */
  slate: {
    wash: 'linear-gradient(135deg, #f1f5f9 0%, #dfe7f0 100%)',
    accent: 'linear-gradient(135deg, #475569, #94a3b8)',
    value: '#334155',
    icon: '#ffffff',
  },
} satisfies Record<string, Tone>;

export type ToneName = keyof typeof tones;

/**
 * Taqsimot kartochkalarining bezak ranglari.
 *
 * <p>Faqat ko'k-binafsha oila: holat ranglari (yashil, sariq, qizil) bu
 * yerda ishlatilmaydi. Aks holda "Kompyuter injiniringi fakulteti" yashil
 * chiziq bilan chiqib, hech qanday ma'no tashimaydigan rang xabar
 * bergandek ko'rinardi.
 *
 * <p>Rang bitta kartochka ichidagi barcha qatorlar uchun bir xil - u
 * qiymatni emas, kartochkani ajratadi.
 */
export const breakdownAccents = [
  { bar: `linear-gradient(90deg, ${brand[700]}, ${brand[500]})`, chip: brand[600], tint: brand[50] },
  { bar: 'linear-gradient(90deg, #4338ca, #6366f1)', chip: '#4338ca', tint: '#eef2ff' },
  { bar: 'linear-gradient(90deg, #0369a1, #0ea5e9)', chip: '#0369a1', tint: '#f0f9ff' },
  { bar: 'linear-gradient(90deg, #6d28d9, #8b5cf6)', chip: '#6d28d9', tint: '#f5f3ff' },
] as const;
