import { api } from './client';
import type { PollResponse } from './types';

export interface SavePollPayload {
  question: string;
  description?: string;
  multipleChoice?: boolean;
  active?: boolean;
  startsAt?: string | null;
  endsAt?: string | null;
  options: { id?: number | null; text: string }[];
}

export const pollsApi = {
  active: () => api.get<PollResponse[]>('/polls'),

  detail: (id: number) => api.get<PollResponse>(`/polls/${id}`),

  vote: (id: number, optionIds: number[]) =>
    api.post<PollResponse>(`/polls/${id}/vote`, { optionIds }),

  // ------------------------------------------------------------- admin
  all: () => api.get<PollResponse[]>('/admin/polls'),

  create: (payload: SavePollPayload) => api.post<PollResponse>('/admin/polls', payload),

  update: (id: number, payload: SavePollPayload) =>
    api.put<PollResponse>(`/admin/polls/${id}`, payload),

  setActive: (id: number, active: boolean) =>
    api.patch<PollResponse>(`/admin/polls/${id}/active`, undefined, { query: { active } }),

  remove: (id: number) => api.delete<void>(`/admin/polls/${id}`),
};
