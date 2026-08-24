import type { TFunction } from 'i18next';
import { ApiError } from '../api/client';

/**
 * Xatolikni foydalanuvchiga ko'rsatiladigan matnga aylantiradi.
 *
 * <p>Backend xabari allaqachon joriy tilda keladi, shuning uchun uni
 * qayta tarjima qilish shart emas - shundayligicha ko'rsatiladi.
 * Faqat tarmoq uzilishi kabi holatlarda frontend o'z matnini beradi,
 * chunki bunda serverdan hech qanday javob kelmagan.
 */
export function errorMessage(error: unknown, t: TFunction): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  // fetch tarmoq uzilishida TypeError tashlaydi.
  if (error instanceof TypeError) {
    return t('errors.network');
  }
  return t('errors.unknown');
}

/** Maydonlar bo'yicha validatsiya xatoliklari (backend qaytargan). */
export function fieldErrors(error: unknown): Record<string, string> {
  return error instanceof ApiError && error.fields ? error.fields : {};
}

export function errorCode(error: unknown): string | null {
  return error instanceof ApiError ? error.code : null;
}
