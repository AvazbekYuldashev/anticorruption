import { api } from './client';

/** Bosh banner fonidagi rasm. `originalName` faqat admin panelida keladi. */
export interface HomeBannerImage {
  id: number;
  url: string;
  originalName: string | null;
  displayOrder: number;
}

export const homeBannerApi = {
  /** Sayt uchun: albom tartibida. Bo'sh bo'lsa banner gradient fonda qoladi. */
  site: () => api.get<HomeBannerImage[]>('/home-banner'),

  admin: () => api.get<HomeBannerImage[]>('/admin/home-banner'),

  /** Rasmlar albom oxiriga qo'shiladi va darhol saqlanadi. */
  upload: (files: File[]) => api.uploadMany<HomeBannerImage[]>('/admin/home-banner', files),

  /** Albomdagi barcha rasmlarning id lari, yangi tartibda. */
  reorder: (ids: number[]) => api.put<HomeBannerImage[]>('/admin/home-banner/order', { ids }),

  remove: (id: number) => api.delete<HomeBannerImage[]>(`/admin/home-banner/${id}`),
};
