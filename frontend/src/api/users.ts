import { api } from './client';
import type { PageResponse, Role, UserResponse } from './types';

export const usersApi = {
  list: (role?: Role | '', page = 0, size = 20) =>
    api.get<PageResponse<UserResponse>>('/admin/users', { query: { role, page, size } }),

  changeRole: (id: number, role: Role) =>
    api.patch<UserResponse>(`/admin/users/${id}/role`, { role }),

  setEnabled: (id: number, enabled: boolean) =>
    api.patch<UserResponse>(`/admin/users/${id}/status`, { enabled }),
};
