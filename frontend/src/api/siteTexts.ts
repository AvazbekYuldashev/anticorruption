import { api } from './client';
import type { SiteTexts } from '../lib/siteTexts';

/** Bitta o'zgartirilgan matn. Bo'sh qiymat yuborilmaydi - u asl matnga qaytishni bildiradi. */
export interface SiteTextItem {
  key: string;
  language: string;
  value: string;
}

export const siteTextsApi = {
  /** Sayt uchun: administrator o'zgartirgan matnlar, barcha tillarda. */
  all: () => api.get<SiteTexts>('/site-texts'),

  admin: () => api.get<SiteTexts>('/admin/site-texts'),

  /** To'liq holat: ro'yxatda yo'q matn asl holiga qaytadi. */
  save: (texts: SiteTextItem[]) => api.put<SiteTexts>('/admin/site-texts', { texts }),
};
