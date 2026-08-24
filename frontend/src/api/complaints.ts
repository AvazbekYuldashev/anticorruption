import { api } from './client';
import type {
  AttachmentResponse,
  ComplaintCreatedResponse,
  ComplaintResponse,
  ComplaintSummaryResponse,
  ComplaintTrackingResponse,
  CreateComplaintRequest,
  PageResponse,
  RegisterEntryResponse,
} from './types';

export interface RegisterFilters {
  code?: string;
  category?: string;
  status?: string;
  facultyId?: number | null;
  page?: number;
  size?: number;
}

export interface AdminComplaintFilters {
  query?: string;
  status?: string;
  category?: string;
  facultyId?: number | null;
  departmentId?: number | null;
  reporterType?: string;
  accusedPosition?: string;
  assigneeId?: number | null;
  unassigned?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export const complaintsApi = {
  // ------------------------------------------------------------- ochiq
  create: (payload: CreateComplaintRequest) =>
    api.post<ComplaintCreatedResponse>('/complaints', payload),

  track: (trackingCode: string) =>
    api.get<ComplaintTrackingResponse>(`/complaints/track/${encodeURIComponent(trackingCode)}`),

  attach: (trackingCode: string, file: File) =>
    api.upload<AttachmentResponse>(
      `/complaints/track/${encodeURIComponent(trackingCode)}/attachments`,
      file,
    ),

  register: (filters: RegisterFilters) =>
    api.get<PageResponse<RegisterEntryResponse>>('/complaints/register', {
      query: { ...filters, size: filters.size ?? 20 },
    }),

  // ------------------------------------------------------------- foydalanuvchi
  mine: (page = 0, size = 10) =>
    api.get<PageResponse<ComplaintSummaryResponse>>('/complaints/my', { query: { page, size } }),

  mineDetail: (id: number) => api.get<ComplaintResponse>(`/complaints/my/${id}`),

  // ------------------------------------------------------------- xodimlar
  search: (filters: AdminComplaintFilters) =>
    api.get<PageResponse<ComplaintSummaryResponse>>('/admin/complaints', {
      query: { ...filters, size: filters.size ?? 20 },
    }),

  detail: (id: number) => api.get<ComplaintResponse>(`/admin/complaints/${id}`),

  updateStatus: (id: number, body: { status: string; note?: string; officialResponse?: string }) =>
    api.patch<ComplaintResponse>(`/admin/complaints/${id}/status`, body),

  assign: (id: number, assigneeId: number | null) =>
    api.patch<ComplaintResponse>(`/admin/complaints/${id}/assign`, { assigneeId }),

  setRegisterVisibility: (id: number, hidden: boolean) =>
    api.patch<ComplaintResponse>(`/admin/complaints/${id}/register-visibility`, { hidden }),
};
