import { api } from './client';
import type { AuthResponse, UserResponse } from './types';

export interface LoginPayload {
  email: string;
  password: string;
}

export const authApi = {
  /** Tokenlarni HttpOnly cookie sifatida o'rnatadi; javob tanasida token yo'q. */
  login: (payload: LoginPayload) => api.post<AuthResponse>('/auth/login', payload),

  /** Yangilash cookie'si bo'yicha yangi kirish tokeni oladi. */
  refresh: () => api.post<AuthResponse>('/auth/refresh'),

  /** Yangilash tokenini serverda bekor qiladi va cookie'larni o'chiradi. */
  logout: () => api.post<void>('/auth/logout'),

  /** Seans egasini qaytaradi - sahifa yangilanganda seansni tiklash uchun. */
  me: () => api.get<UserResponse>('/me'),
};
