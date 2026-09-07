/**
 * Brauzer tomonidagi seans holati.
 *
 * <p>Token bu yerda YO'Q va endi umuman saqlanmaydi. Backend uni
 * `HttpOnly` cookie'da beradi: brauzer uni har bir so'rovga o'zi qo'shadi,
 * lekin sahifadagi JavaScript uni o'qiy olmaydi. Shu tufayli XSS topilgan
 * taqdirda ham token o'g'irlanmaydi - `localStorage` bilan bu mumkin edi.
 *
 * <p>Buning evaziga cookie har bir so'rovga avtomatik qo'shiladi, ya'ni
 * begona sayt ham bizning nomimizdan so'rov yuborishi mumkin bo'lardi.
 * Bunga qarshi CSRF tokeni ishlaydi: u oddiy (HttpOnly bo'lmagan)
 * cookie'da keladi, biz uni o'qib sarlavhaga qo'yamiz. Begona sayt
 * cookie'ni o'qiy olmaydi, demak sarlavhani ham to'ldira olmaydi.
 */

const CSRF_COOKIE = 'XSRF-TOKEN';

/** Backend shu sarlavhada CSRF tokenini kutadi. */
export const CSRF_HEADER = 'X-XSRF-TOKEN';

/**
 * "Seans bor" belgisi.
 *
 * <p>Bu token emas va uning o'g'irlanishi hech narsa bermaydi - shunchaki
 * eslatma. Usiz ilova har bir ochilishida anonim mehmon uchun ham
 * `/me` va `/auth/refresh` so'rovlarini yuborardi.
 */
const SESSION_HINT = 'anticorruption.session';

let unauthorizedHandler: (() => void) | null = null;

/** CSRF tokenini cookie'dan o'qiydi. */
export function readCsrfToken(): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${CSRF_COOKIE}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

export function hasSessionHint(): boolean {
  try {
    return localStorage.getItem(SESSION_HINT) === '1';
  } catch {
    // Maxfiylik rejimida localStorage taqiqlangan bo'lishi mumkin.
    return false;
  }
}

export function rememberSession(): void {
  try {
    localStorage.setItem(SESSION_HINT, '1');
  } catch {
    /* saqlanmasa ham ilova ishlashda davom etadi */
  }
}

export function forgetSession(): void {
  try {
    localStorage.removeItem(SESSION_HINT);
  } catch {
    /* e'tiborsiz */
  }
}

/**
 * Seans tugaganda chaqiriladigan funksiyani ro'yxatdan o'tkazadi.
 * AuthContext buni o'z holatini tozalash uchun beradi.
 */
export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler;
}

export function notifyUnauthorized(): void {
  forgetSession();
  unauthorizedHandler?.();
}
