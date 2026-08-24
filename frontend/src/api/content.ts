import { api } from './client';
import type {
  NewsDetailResponse,
  NewsImageResponse,
  NewsSummaryResponse,
  PageResponse,
  StaffMemberResponse,
  StaticPageResponse,
  UsefulLinkResponse,
} from './types';

export interface SaveNewsPayload {
  title: string;
  summary?: string;
  body: string;
  published?: boolean;
}

export interface SaveStaffPayload {
  fullName: string;
  position: string;
  academicDegree?: string;
  phone?: string;
  email?: string;
  receptionHours?: string;
  displayOrder?: number;
  active?: boolean;
}

export interface SavePagePayload {
  slug?: string;
  title: string;
  body: string;
  displayOrder?: number;
  published?: boolean;
}

export interface SaveLinkPayload {
  title: string;
  url: string;
  description?: string;
  groupName?: string;
  displayOrder?: number;
  active?: boolean;
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

  /** Albomga bir vaqtda bir nechta rasm qo'shadi. */
  uploadNewsImages: (id: number, files: File[]) =>
    api.uploadMany<NewsImageResponse[]>(`/admin/news/${id}/images`, files),

  deleteNewsImage: (id: number, imageId: number) =>
    api.delete<void>(`/admin/news/${id}/images/${imageId}`),

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

  // ------------------------------------------------------------- sahifalar
  pages: () => api.get<StaticPageResponse[]>('/pages'),

  pageBySlug: (slug: string) => api.get<StaticPageResponse>(`/pages/${encodeURIComponent(slug)}`),

  adminPages: () => api.get<StaticPageResponse[]>('/admin/pages'),

  createPage: (payload: SavePagePayload) => api.post<StaticPageResponse>('/admin/pages', payload),

  updatePage: (id: number, payload: SavePagePayload) =>
    api.put<StaticPageResponse>(`/admin/pages/${id}`, payload),

  deletePage: (id: number) => api.delete<void>(`/admin/pages/${id}`),

  // ------------------------------------------------------------- havolalar
  links: () => api.get<UsefulLinkResponse[]>('/links'),

  adminLinks: () => api.get<UsefulLinkResponse[]>('/admin/links'),

  createLink: (payload: SaveLinkPayload) => api.post<UsefulLinkResponse>('/admin/links', payload),

  updateLink: (id: number, payload: SaveLinkPayload) =>
    api.put<UsefulLinkResponse>(`/admin/links/${id}`, payload),

  deleteLink: (id: number) => api.delete<void>(`/admin/links/${id}`),
};
