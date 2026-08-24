import { api } from './client';
import type { DepartmentResponse, FacultyResponse } from './types';

export interface SaveFacultyPayload {
  name: string;
  code: string;
  active?: boolean;
}

export interface SaveDepartmentPayload {
  name: string;
  code: string;
  active?: boolean;
}

export const universityApi = {
  /** Admin ro'yxati: nofaol fakultetlar ham ko'rinadi. */
  faculties: () => api.get<FacultyResponse[]>('/admin/faculties'),

  createFaculty: (payload: SaveFacultyPayload) =>
    api.post<FacultyResponse>('/admin/faculties', payload),

  updateFaculty: (id: number, payload: SaveFacultyPayload) =>
    api.put<FacultyResponse>(`/admin/faculties/${id}`, payload),

  deleteFaculty: (id: number) => api.delete<void>(`/admin/faculties/${id}`),

  createDepartment: (facultyId: number, payload: SaveDepartmentPayload) =>
    api.post<DepartmentResponse>(`/admin/faculties/${facultyId}/departments`, payload),

  updateDepartment: (departmentId: number, payload: SaveDepartmentPayload) =>
    api.put<DepartmentResponse>(`/admin/faculties/departments/${departmentId}`, payload),

  deleteDepartment: (departmentId: number) =>
    api.delete<void>(`/admin/faculties/departments/${departmentId}`),
};
