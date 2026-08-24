/**
 * JWT tokenni brauzerda saqlash.
 *
 * <p>React'dan tashqarida turadi, chunki API mijozi tokenga har bir so'rovda
 * muhtoj bo'ladi va u komponent daraxtining bir qismi emas.
 *
 * <p>`localStorage` tanlandi: sahifa yangilanganda ham foydalanuvchi
 * tizimda qolishi kerak. Bu XSS holatida token o'g'irlanishi mumkinligini
 * anglatadi - haqiqiy himoya HttpOnly cookie bo'lardi, lekin u backend
 * tomonidan CSRF himoyasi bilan birga qo'shilishi kerak. Hozircha
 * kelishuv shu, va bu README'da keyingi qadam sifatida yozilgan.
 */

const TOKEN_KEY = 'anticorruption.token';

let unauthorizedHandler: (() => void) | null = null;

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    // Maxfiylik rejimida localStorage taqiqlangan bo'lishi mumkin.
    return null;
  }
}

export function setToken(token: string): void {
  try {
    localStorage.setItem(TOKEN_KEY, token);
  } catch {
    /* saqlanmasa ham ilova ishlashda davom etadi */
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* e'tiborsiz */
  }
}

/**
 * Token eskirganda yoki bekor qilinganda chaqiriladigan funksiyani
 * ro'yxatdan o'tkazadi. AuthContext buni o'z holatini tozalash uchun beradi.
 */
export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler;
}

export function notifyUnauthorized(): void {
  clearToken();
  unauthorizedHandler?.();
}
