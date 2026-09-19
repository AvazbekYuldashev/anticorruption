import { useSyncExternalStore } from 'react';

/*
 * Sayt rejimi: yorug', qorong'i yoki avtomatik (qurilma sozlamasiga ergashadi).
 *
 * Qorong'i rejim <html> dagi `dark` sinfi bilan yoqiladi - Tailwind `dark:`
 * variantlari shunga qaraydi (index.css). Birinchi chizishdan oldin sinfni
 * public/theme-init.js qo'yadi, bu modul esa keyin tanlovni boshqaradi -
 * ikkalasi bir xil kalit va bir xil qoida bilan ishlaydi.
 *
 * Admin panel hozircha faqat yorug' rejimda: u MUI da yozilgan va o'z
 * ranglariga ega. U ochiq turganda qorong'i sinf olib tashlanadi.
 */

export type ThemeMode = 'light' | 'dark' | 'auto';

export const THEME_MODES: ThemeMode[] = ['light', 'dark', 'auto'];

/** public/theme-init.js dagi kalit bilan bir xil. */
const STORAGE_KEY = 'anticorruption.theme';

/*
 * So'rov obyekti modulda saqlanadi: havolasiz qolgan MediaQueryList brauzer
 * tomonidan tozalab yuborilishi va unga ulangan tinglovchi jim bo'lib qolishi mumkin.
 */
const systemQuery: MediaQueryList | undefined = window.matchMedia?.('(prefers-color-scheme: dark)');

const systemDark = () => systemQuery?.matches === true;

function readStored(): ThemeMode {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value === 'light' || value === 'dark' ? value : 'auto';
  } catch {
    return 'auto';
  }
}

let mode: ThemeMode = readStored();
let lightOnly = false;
const listeners = new Set<() => void>();

function apply(): void {
  const dark = !lightOnly && (mode === 'dark' || (mode === 'auto' && systemDark()));
  const root = document.documentElement;
  root.classList.toggle('dark', dark);
  // Brauzerning o'z elementlari (aylantirish chizig'i, sana tanlagich) ham mos tusda bo'lsin.
  root.style.colorScheme = dark ? 'dark' : 'light';
}

function notify(): void {
  apply();
  listeners.forEach((listener) => listener());
}

// Avtomatik rejimda qurilma sozlamasi o'zgarsa sayt ham darhol almashadi.
systemQuery?.addEventListener?.('change', () => {
  if (mode === 'auto') notify();
});

// Boshqa varaqda tanlov o'zgarsa bu varaq ham ergashsin.
window.addEventListener('storage', (event) => {
  if (event.key === STORAGE_KEY) {
    mode = readStored();
    notify();
  }
});

export function setThemeMode(next: ThemeMode): void {
  mode = next;
  try {
    if (next === 'auto') localStorage.removeItem(STORAGE_KEY);
    else localStorage.setItem(STORAGE_KEY, next);
  } catch {
    /* saqlanmasa ham joriy sahifada ishlaydi */
  }
  notify();
}

/** Admin panel ochiq turganda sayt yorug' rejimda qoladi. */
export function setLightOnly(value: boolean): void {
  lightOnly = value;
  notify();
}

export function useThemeMode(): ThemeMode {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => mode,
  );
}
