import { api } from './client';
import type { PollResponse, PollStatisticsResponse, PollType } from './types';

/** Saqlashda yuboriladigan variant. `id` bo'lsa mavjud variant ovozlari bilan saqlanadi. */
export interface SavePollOption {
  id?: number | null;
  text: string;
  /** Faqat testda: shu variant to'g'ri javobmi. */
  correct?: boolean;
}

export interface SavePollQuestion {
  id?: number | null;
  text: string;
  multipleChoice?: boolean;
  required?: boolean;
  options: SavePollOption[];
}

export interface SavePollPayload {
  title: string;
  description?: string;
  /** So'rovnomami yoki test; ko'rsatilmasa so'rovnoma. */
  type?: PollType;
  active?: boolean;
  /** Qaysi guruhga tushsin. Majburiy: guruhsiz so'rovnoma bo'lmaydi. */
  groupId: number | null;
  /** null bo'lsa muddat cheklovi yo'q. */
  startsAt?: string | null;
  endsAt?: string | null;
  questions: SavePollQuestion[];
}

/** Bitta savolga javob. Majburiy bo'lmagan savol ro'yxatga qo'shilmasligi mumkin. */
export interface PollAnswer {
  questionId: number;
  optionIds: number[];
}

export const pollsApi = {
  active: (type?: PollType) => api.get<PollResponse[]>('/polls', { query: { type } }),

  detail: (id: number) => api.get<PollResponse>(`/polls/${id}`),

  /**
   * Testni boshlaydi: ishtirokchiga tushgan savollar va `attemptToken` keladi.
   * Qayta chaqirilsa o'sha savollar qaytadi - yangilab boshqa to'plam olib bo'lmaydi.
   */
  start: (id: number) => api.post<PollResponse>(`/polls/${id}/start`),

  /** `attemptToken` - savollari tasodifiy beriladigan testda, boshlanganda berilgan belgi. */
  vote: (id: number, answers: PollAnswer[], attemptToken?: string | null) =>
    api.post<PollResponse>(`/polls/${id}/vote`, { attemptToken, answers }),

  // ------------------------------------------------------------- admin
  all: (type?: PollType) => api.get<PollResponse[]>('/admin/polls', { query: { type } }),

  statistics: (id: number) => api.get<PollStatisticsResponse>(`/admin/polls/${id}/statistics`),

  create: (payload: SavePollPayload) => api.post<PollResponse>('/admin/polls', payload),

  update: (id: number, payload: SavePollPayload) =>
    api.put<PollResponse>(`/admin/polls/${id}`, payload),

  setActive: (id: number, active: boolean) =>
    api.patch<PollResponse>(`/admin/polls/${id}/active`, undefined, { query: { active } }),

  /** Qo'lda to'xtatadi yoki to'xtatishni bekor qiladi. */
  setStopped: (id: number, stopped: boolean) =>
    api.patch<PollResponse>(`/admin/polls/${id}/stopped`, undefined, { query: { stopped } }),

  /**
   * Boshqa guruhga ko'chiradi.
   *
   * <p>Alohida amal - ro'yxatdan turib ko'chirish uchun to'liq tahrirlash
   * shaklini (savollar, muddat bilan) ochish ortiqcha bo'lardi.
   */
  setGroup: (id: number, groupId: number) =>
    api.patch<PollResponse>(`/admin/polls/${id}/group`, undefined, { query: { groupId } }),

  /**
   * Savollar nusxasi bilan yangi o'tkazish ochadi va eskisini to'xtatadi.
   * Eski hisobot tegilmaydi.
   */
  restart: (id: number, period?: { startsAt: string | null; endsAt: string | null }) =>
    api.post<PollResponse>(`/admin/polls/${id}/restart`, period ?? {}),

  remove: (id: number) => api.delete<void>(`/admin/polls/${id}`),
};
