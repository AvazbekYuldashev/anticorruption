import i18next from 'i18next';

import en from '../i18n/locales/en.json';
import ru from '../i18n/locales/ru.json';
import uz from '../i18n/locales/uz.json';
import uzCyrl from '../i18n/locales/uz-cyrl.json';

/*
 * Bosh sahifa matnlari admin panelidan o'zgartiriladi.
 *
 * Sahifa matnlari odatdagidek tarjima kalitlari orqali chiqadi
 * (`t('home.heroTitle')`). Administrator yozgan matn serverdan keladi va shu
 * kalitlarning ustidan yoziladi - sahifa kodi hech narsa bilmaydi. Yozilmagan
 * matn tarjima faylidagi asl holicha qoladi.
 */

/** Til kodi -> (kalit -> matn). Faqat o'zgartirilgan matnlar. */
export type SiteTexts = Record<string, Record<string, string>>;

export interface SiteTextField {
  /** Tarjima kaliti - backenddagi `SiteTextKey` bilan bir xil. */
  key: string;
  /** `admin.siteTextLabels.*` dagi nom. */
  label: string;
  /** Bir necha jumlali matn - maydon kengroq bo'ladi. */
  multiline?: boolean;
}

export const SITE_TEXT_GROUPS: { title: string; fields: SiteTextField[] }[] = [
  {
    title: 'homeTextsGroupHero',
    fields: [
      { key: 'site.institute', label: 'institute' },
      { key: 'site.name', label: 'siteName' },
      { key: 'home.heroTitle', label: 'heroTitle' },
      { key: 'home.heroText', label: 'heroText', multiline: true },
      { key: 'home.ctaSubmit', label: 'ctaSubmit' },
      { key: 'home.ctaTrack', label: 'ctaTrack' },
    ],
  },
  {
    title: 'homeTextsGroupStats',
    fields: [
      { key: 'home.statsTitle', label: 'statsTitle' },
      { key: 'home.statTotal', label: 'statTotal' },
      { key: 'home.statResolved', label: 'statResolved' },
      { key: 'home.statOpen', label: 'statOpen' },
      { key: 'home.statLast30', label: 'statLast30' },
    ],
  },
  {
    title: 'homeTextsGroupHow',
    fields: [
      { key: 'home.howTitle', label: 'howTitle' },
      { key: 'home.step1Title', label: 'step1Title' },
      { key: 'home.step1Text', label: 'step1Text', multiline: true },
      { key: 'home.step2Title', label: 'step2Title' },
      { key: 'home.step2Text', label: 'step2Text', multiline: true },
      { key: 'home.step3Title', label: 'step3Title' },
      { key: 'home.step3Text', label: 'step3Text', multiline: true },
    ],
  },
  {
    title: 'homeTextsGroupSections',
    fields: [
      { key: 'home.newsTitle', label: 'newsTitle' },
      { key: 'home.newsMore', label: 'newsMore' },
      { key: 'home.pollTitle', label: 'pollTitle' },
    ],
  },
];

/** Kalit bo'yicha maydon ta'rifi - admin sahifasida maydonlarni saytdagi joyiga qarab terish uchun. */
export const SITE_TEXT_FIELDS: Record<string, SiteTextField> = Object.fromEntries(
  SITE_TEXT_GROUPS.flatMap((group) => group.fields.map((field) => [field.key, field])),
);

export const SITE_TEXT_KEYS = SITE_TEXT_GROUPS.flatMap((group) =>
  group.fields.map((field) => field.key),
);

const LANGUAGE_CODES = ['uz', 'uz-cyrl', 'ru', 'en'] as const;

/*
 * Asl matnlarning nusxasi. i18next tarjima fayllarini nusxa olmasdan saqlaydi
 * va matn qo'yilganda aynan shu obyektlarni o'zgartiradi - nusxasiz birinchi
 * o'zgartirilgan matn "asl matn"ning o'rnini egallab, uni qaytarib bo'lmasdi.
 * Nusxa modul yuklanganda olinadi, hali hech narsa qo'yilmagan paytda.
 */
const DEFAULTS: Record<string, unknown> = JSON.parse(
  JSON.stringify({ uz, 'uz-cyrl': uzCyrl, ru, en }),
);

/** Tarjima faylidagi asl matn. */
export function defaultSiteText(language: string, key: string): string {
  let node: unknown = DEFAULTS[language] ?? DEFAULTS.uz;
  for (const part of key.split('.')) {
    node = (node as Record<string, unknown> | undefined)?.[part];
  }
  return typeof node === 'string' ? node : '';
}

/**
 * Serverdan kelgan matnlarni tarjimalar ustidan qo'yadi.
 *
 * <p>Har safar barcha tahrirlanadigan kalitlar yoziladi, faqat o'zgarganlari
 * emas: administrator matnni asl holiga qaytarsa, avval qo'yilgan matn
 * tarjimalarda qolib ketmasin.
 */
export function applySiteTexts(texts: SiteTexts): void {
  for (const language of LANGUAGE_CODES) {
    const values: Record<string, string> = {};
    for (const key of SITE_TEXT_KEYS) {
      values[key] = texts[language]?.[key] ?? defaultSiteText(language, key);
    }
    i18next.addResources(language, 'translation', values);
  }
  remember(texts);
}

const CACHE_KEY = 'anticorruption.siteTexts';

/**
 * Oxirgi marta kelgan matnlarni darhol qo'yadi.
 *
 * <p>Server javobini kutib turilsa, qayta kirgan tashrifchi bir lahza asl
 * sarlavhani ko'rib, keyin u almashganini sezardi.
 */
export function applyCachedSiteTexts(): void {
  try {
    const cached = localStorage.getItem(CACHE_KEY);
    if (cached) applySiteTexts(JSON.parse(cached) as SiteTexts);
  } catch {
    /* localStorage taqiqlangan yoki yozuv buzilgan - asl matnlar qoladi */
  }
}

function remember(texts: SiteTexts): void {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(texts));
  } catch {
    /* e'tiborsiz */
  }
}
