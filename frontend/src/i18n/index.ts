import i18next from 'i18next';
import { initReactI18next } from 'react-i18next';

import en from './locales/en.json';
import ru from './locales/ru.json';
import uz from './locales/uz.json';
import uzCyrl from './locales/uz-cyrl.json';

/** Backend qabul qiladigan kodlar bilan aynan bir xil. */
export const LANGUAGES = [
  { code: 'uz', name: "O'zbekcha" },
  { code: 'uz-cyrl', name: 'Ўзбекча' },
  { code: 'ru', name: 'Русский' },
  { code: 'en', name: 'English' },
] as const;

export type LanguageCode = (typeof LANGUAGES)[number]['code'];

const STORAGE_KEY = 'anticorruption.lang';
const DEFAULT_LANGUAGE: LanguageCode = 'uz';

function storedLanguage(): LanguageCode {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved && LANGUAGES.some((language) => language.code === saved)) {
      return saved as LanguageCode;
    }
  } catch {
    /* localStorage taqiqlangan bo'lishi mumkin */
  }
  return DEFAULT_LANGUAGE;
}

void i18next.use(initReactI18next).init({
  resources: {
    uz: { translation: uz },
    'uz-cyrl': { translation: uzCyrl },
    ru: { translation: ru },
    en: { translation: en },
  },
  lng: storedLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  supportedLngs: LANGUAGES.map((language) => language.code),
  // 'uz-cyrl' kodini i18next 'uz-CYRL' ga aylantirib qo'ymasligi uchun.
  lowerCaseLng: true,
  interpolation: { escapeValue: false },
});

/**
 * Tilni almashtiradi va uni eslab qoladi.
 *
 * <p>Til API mijoziga ham ta'sir qiladi: u har bir so'rovga
 * `?lang=` ni `i18next.language` dan oladi, shuning uchun backend
 * xabarlari ham darhol yangi tilda kela boshlaydi.
 */
export async function changeLanguage(code: LanguageCode): Promise<void> {
  await i18next.changeLanguage(code);
  document.documentElement.lang = code;
  try {
    localStorage.setItem(STORAGE_KEY, code);
  } catch {
    /* e'tiborsiz */
  }
}

document.documentElement.lang = i18next.language;

export default i18next;
