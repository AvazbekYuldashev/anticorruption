import { api } from './client';
import type { PollGroupResponse, PollType } from './types';

/**
 * Guruh yaratish yoki tahrirlash.
 *
 * <p>`type` faqat yaratishda ma'noga ega: guruh turini keyin o'zgartirish
 * ichidagi so'rovnomalarni boshqa sahifaga ko'chirib yuborardi, shuning
 * uchun backend tahrirlashda uni e'tiborsiz qoldiradi.
 */
export interface SavePollGroupPayload {
  name: string;
  description?: string;
  type?: PollType;
  displayOrder?: number;
  /**
   * Faqat test guruhida: guruhdagi har bir testda foydalanuvchiga savollardan
   * nechtasi tasodifiy tanlab berilsin. null bo'lsa barcha savollar beriladi.
   */
  questionsPerAttempt?: number | null;
}

export const pollGroupsApi = {
  list: (type?: PollType) =>
    api.get<PollGroupResponse[]>('/admin/poll-groups', { query: { type } }),

  create: (payload: SavePollGroupPayload) =>
    api.post<PollGroupResponse>('/admin/poll-groups', payload),

  update: (id: number, payload: SavePollGroupPayload) =>
    api.put<PollGroupResponse>(`/admin/poll-groups/${id}`, payload),

  /** Guruh o'chadi, ichidagi so'rovnomalar "Guruhsiz" bo'limiga qaytadi. */
  remove: (id: number) => api.delete<void>(`/admin/poll-groups/${id}`),
};
