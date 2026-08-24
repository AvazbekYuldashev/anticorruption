import { api } from './client';
import type { FacultyRatingResponse, StatsResponse } from './types';

export const statsApi = {
  /** Ommaviy statistika - autentifikatsiyasiz ochiq. */
  publicStats: () => api.get<StatsResponse>('/stats/public'),

  facultyRating: () => api.get<FacultyRatingResponse[]>('/stats/faculty-rating'),
};
