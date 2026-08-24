import { api } from './client';
import type { AuthResponse, UserResponse } from './types';

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
}

export const authApi = {
  login: (payload: LoginPayload) => api.post<AuthResponse>('/auth/login', payload),

  register: (payload: RegisterPayload) => api.post<AuthResponse>('/auth/register', payload),

  /** Token egasini qaytaradi - sahifa yangilanganda seansni tiklash uchun. */
  me: () => api.get<UserResponse>('/me'),
};
