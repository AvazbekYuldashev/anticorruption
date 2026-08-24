import { api } from './client';
import type { DepartmentResponse, EnumOption, FacultyResponse } from './types';

/**
 * Murojaat shakli uchun barcha ma'lumotnomalar.
 *
 * <p>Bitta so'rovda olinadi: shakl ochilishida beshta alohida so'rov
 * yuborishdan ko'ra tezroq va til o'zgarganda hammasi birgalikda yangilanadi.
 */
export interface ReferenceBundle {
  language: string;
  languages: EnumOption[];
  categories: EnumOption[];
  statuses: EnumOption[];
  reporterTypes: EnumOption[];
  studyForms: EnumOption[];
  positions: EnumOption[];
  roles: EnumOption[];
  faculties: FacultyResponse[];
}

export const referenceApi = {
  all: () => api.get<ReferenceBundle>('/reference'),

  languages: () => api.get<EnumOption[]>('/reference/languages'),

  departments: (facultyId: number) =>
    api.get<DepartmentResponse[]>(`/reference/faculties/${facultyId}/departments`),
};
