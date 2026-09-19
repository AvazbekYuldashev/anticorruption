import { api } from './client';
import type { PageResponse, Role, UserResponse } from './types';

/** Administrator ochadigan yangi hisob. Ochiq ro'yxatdan o'tish yo'q. */
export interface CreateUserPayload {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
  role: Role;
}

/**
 * Administrator hisobni tahrirlaydi.
 *
 * `password` berilsa parol tiklanadi va foydalanuvchining barcha seanslari
 * uziladi; null bo'lsa parol o'zgarmaydi.
 */
export interface UpdateUserPayload {
  fullName: string;
  email: string;
  phone?: string;
  password?: string | null;
}

/** Foydalanuvchining o'z ma'lumotlari. */
export interface UpdateProfilePayload {
  fullName: string;
  email: string;
  phone?: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export const usersApi = {
  list: (role?: Role | '', page = 0, size = 20) =>
    api.get<PageResponse<UserResponse>>('/admin/users', { query: { role, page, size } }),

  create: (payload: CreateUserPayload) => api.post<UserResponse>('/admin/users', payload),

  update: (id: number, payload: UpdateUserPayload) =>
    api.put<UserResponse>(`/admin/users/${id}`, payload),

  changeRole: (id: number, role: Role) =>
    api.patch<UserResponse>(`/admin/users/${id}/role`, { role }),

  setEnabled: (id: number, enabled: boolean) =>
    api.patch<UserResponse>(`/admin/users/${id}/status`, { enabled }),

  remove: (id: number) => api.delete<void>(`/admin/users/${id}`),

  // ------------------------------------------------------------- o'z profili
  updateProfile: (payload: UpdateProfilePayload) => api.put<UserResponse>('/me', payload),

  changePassword: (payload: ChangePasswordPayload) => api.post<void>('/me/password', payload),
};
