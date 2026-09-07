import { api } from './client';
import type {
  AboutSectionResponse,
  NewsBlockType,
  NewsDetailResponse,
  NewsSummaryResponse,
  PageResponse,
  StaffMemberResponse,
} from './types';

/** Albomga qo'shiladigan rasm - avval yuklangan faylga havola. */
export interface SaveNewsBlockImage {
  storedName: string;
  originalName?: string;
  caption?: string;
}

/** Saqlashda yuboriladigan blok. Rasm bloklari avval yuklangan fayllarga havola qiladi. */
export interface SaveNewsBlock {
  type: NewsBlockType;
  text?: string;
  storedName?: string;
  originalName?: string;
  caption?: string;
  images?: SaveNewsBlockImage[];
}

export interface SaveNewsPayload {
  title: string;
  summary?: string;
  published?: boolean;
  /** To'liq ro'yxat: unda yo'q blok o'chiriladi. */
  blocks: SaveNewsBlock[];
}

/** Muharrir uchun yuklangan rasm. */
export interface UploadedMedia {
  storedName: string;
  originalName: string;
  url: string;
}

export interface SaveStaffPayload {
  fullName: string;
  position: string;
  academicDegree?: string;
  biography?: string;
  phone?: string;
  email?: string;
  receptionHours?: string;
  displayOrder?: number;
  active?: boolean;
}

/** "Bo'lim haqida" sahifasini saqlash. Bo'sh bandlar server tomonida tashlanadi. */
export interface SaveAboutPayload {
  title?: string;
  body?: string;
  tasksTitle?: string;
  tasks: string[];
  goal?: string;
}

export const contentApi = {
  // ------------------------------------------------------------- yangiliklar
  news: (query?: string, page = 0, size = 9) =>
    api.get<PageResponse<NewsSummaryResponse>>('/news', { query: { query, page, size } }),

  newsBySlug: (slug: string) => api.get<NewsDetailResponse>(`/news/${encodeURIComponent(slug)}`),

  adminNews: (page = 0, size = 20) =>
    api.get<PageResponse<NewsSummaryResponse>>('/admin/news', { query: { page, size } }),

  adminNewsDetail: (id: number) => api.get<NewsDetailResponse>(`/admin/news/${id}`),

  createNews: (payload: SaveNewsPayload) => api.post<NewsDetailResponse>('/admin/news', payload),

  updateNews: (id: number, payload: SaveNewsPayload) =>
    api.put<NewsDetailResponse>(`/admin/news/${id}`, payload),

  publishNews: (id: number, published: boolean) =>
    api.patch<NewsDetailResponse>(`/admin/news/${id}/publish`, undefined, { query: { published } }),

  uploadNewsCover: (id: number, file: File) =>
    api.upload<NewsDetailResponse>(`/admin/news/${id}/cover`, file),

  /**
     * Muharrir uchun rasm yuklaydi va uning nomini qaytaradi.
     * Rasm hali hech qanday yangilikka bog'lanmagan - blok saqlanganda bog'lanadi.
     */
  uploadMedia: (file: File) => api.upload<UploadedMedia>('/admin/media', file),

  deleteNews: (id: number) => api.delete<void>(`/admin/news/${id}`),

  // ------------------------------------------------------------- xodimlar
  staff: () => api.get<StaffMemberResponse[]>('/staff'),

  adminStaff: () => api.get<StaffMemberResponse[]>('/admin/staff'),

  createStaff: (payload: SaveStaffPayload) =>
    api.post<StaffMemberResponse>('/admin/staff', payload),

  updateStaff: (id: number, payload: SaveStaffPayload) =>
    api.put<StaffMemberResponse>(`/admin/staff/${id}`, payload),

  uploadStaffPhoto: (id: number, file: File) =>
    api.upload<StaffMemberResponse>(`/admin/staff/${id}/photo`, file),

  deleteStaff: (id: number) => api.delete<void>(`/admin/staff/${id}`),

  // ------------------------------------------------------------- bo'lim haqida
  about: () => api.get<AboutSectionResponse>('/about'),

  adminAbout: () => api.get<AboutSectionResponse>('/admin/about'),

  saveAbout: (payload: SaveAboutPayload) =>
    api.put<AboutSectionResponse>('/admin/about', payload),
};
