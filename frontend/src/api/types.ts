/**
 * Backend DTO'lariga mos turlar.
 *
 * <p>Enum qiymatlari string literal union sifatida yozilgan: backend ularni
 * aynan shu nomlar bilan yuboradi va qabul qiladi. Ekranga chiqadigan nom
 * har doim alohida `*Label` maydonida keladi - u so'rov tiliga qarab
 * o'zgaradi, shuning uchun frontendda takroriy tarjima saqlanmaydi.
 */

export type ComplaintStatus = 'NEW' | 'IN_REVIEW' | 'NEED_INFO' | 'RESOLVED' | 'REJECTED';

export type ComplaintCategory =
  | 'EXAM_BRIBERY'
  | 'GRADE_SELLING'
  | 'RETAKE_PAYMENT'
  | 'ATTENDANCE_PAYMENT'
  | 'ACADEMIC_FRAUD'
  | 'THESIS_FRAUD'
  | 'ADMISSION_FRAUD'
  | 'TRANSFER_FRAUD'
  | 'SCHOLARSHIP_FRAUD'
  | 'CONTRACT_PAYMENT_FRAUD'
  | 'DORMITORY_CORRUPTION'
  | 'FORCED_COLLECTION'
  | 'PROCUREMENT_FRAUD'
  | 'HIRING_NEPOTISM'
  | 'ABUSE_OF_POWER'
  | 'PROPERTY_MISUSE'
  | 'OTHER';

export type ReporterType =
  | 'STUDENT'
  | 'MASTER_STUDENT'
  | 'TEACHER'
  | 'STAFF'
  | 'PARENT'
  | 'ALUMNI'
  | 'EXTERNAL';

export type StudyForm = 'FULL_TIME' | 'EVENING' | 'PART_TIME' | 'DISTANCE' | 'JOINT';

export type AccusedPosition =
  | 'TEACHER'
  | 'HEAD_OF_DEPARTMENT'
  | 'DEAN'
  | 'VICE_DEAN'
  | 'RECTORATE'
  | 'ADMISSION_COMMITTEE'
  | 'DORMITORY_STAFF'
  | 'ACCOUNTING'
  | 'REGISTRAR'
  | 'SECURITY'
  | 'OTHER_STAFF'
  | 'UNKNOWN';

export type Role = 'CITIZEN' | 'MODERATOR' | 'ADMIN';

/** Ochiluvchi ro'yxatlar uchun: qiymat + joriy tildagi nomi. */
export interface EnumOption {
  value: string;
  label: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/** Backend xatoligining yagona formati. */
export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  /** Tilga bog'liq bo'lmagan kalit, masalan "error.complaint.notFoundByCode". */
  code: string;
  message: string;
  path: string;
  fields?: Record<string, string> | null;
}

// ---------------------------------------------------------------- foydalanuvchi

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: Role;
  roleLabel: string;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

// ---------------------------------------------------------------- tuzilma

export interface DepartmentResponse {
  id: number;
  facultyId: number;
  name: string;
  code: string;
  active: boolean;
}

export interface FacultyResponse {
  id: number;
  name: string;
  code: string;
  active: boolean;
  departments: DepartmentResponse[];
}

// ---------------------------------------------------------------- murojaat

export interface AttachmentResponse {
  id: number;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  downloadUrl: string;
  createdAt: string;
}

export interface StatusHistoryResponse {
  id: number;
  oldStatus: ComplaintStatus | null;
  oldStatusLabel: string | null;
  newStatus: ComplaintStatus;
  newStatusLabel: string;
  note: string | null;
  changedBy: string | null;
  changedAt: string;
}

export interface ComplaintCreatedResponse {
  id: number;
  trackingCode: string;
  status: ComplaintStatus;
  statusLabel: string;
  message: string;
  createdAt: string;
}

export interface ComplaintTrackingResponse {
  trackingCode: string;
  title: string;
  category: ComplaintCategory;
  categoryLabel: string;
  facultyName: string | null;
  departmentName: string | null;
  subjectName: string | null;
  status: ComplaintStatus;
  statusLabel: string;
  officialResponse: string | null;
  attachmentCount: number;
  history: StatusHistoryResponse[];
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
}

export interface ComplaintSummaryResponse {
  id: number;
  trackingCode: string;
  title: string;
  category: ComplaintCategory;
  categoryLabel: string;
  facultyName: string | null;
  departmentName: string | null;
  subjectName: string | null;
  reporterType: ReporterType | null;
  reporterTypeLabel: string | null;
  status: ComplaintStatus;
  statusLabel: string;
  anonymous: boolean;
  assigneeName: string | null;
  attachmentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ComplaintResponse {
  id: number;
  trackingCode: string;
  title: string;
  description: string;
  category: ComplaintCategory;
  categoryLabel: string;
  facultyId: number | null;
  facultyName: string | null;
  departmentId: number | null;
  departmentName: string | null;
  subjectName: string | null;
  accusedPosition: AccusedPosition | null;
  accusedPositionLabel: string | null;
  incidentDate: string | null;
  incidentPlace: string | null;
  anonymous: boolean;
  reporterType: ReporterType | null;
  reporterTypeLabel: string | null;
  courseYear: number | null;
  groupName: string | null;
  studyForm: StudyForm | null;
  studyFormLabel: string | null;
  reporterName: string | null;
  reporterEmail: string | null;
  reporterPhone: string | null;
  author: UserResponse | null;
  assignee: UserResponse | null;
  status: ComplaintStatus;
  statusLabel: string;
  officialResponse: string | null;
  hiddenFromRegister: boolean;
  attachments: AttachmentResponse[];
  history: StatusHistoryResponse[];
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
}

/** Ochiq reyestr yozuvi - erkin matn maydonlari ataylab yo'q. */
export interface RegisterEntryResponse {
  trackingCode: string;
  category: ComplaintCategory;
  categoryLabel: string;
  facultyName: string;
  reporterType: ReporterType | null;
  reporterTypeLabel: string | null;
  status: ComplaintStatus;
  statusLabel: string;
  createdAt: string;
  closedAt: string | null;
}

export interface CreateComplaintRequest {
  title: string;
  description: string;
  category: ComplaintCategory | '';
  reporterType: ReporterType | '';
  facultyId?: number | null;
  departmentId?: number | null;
  subjectName?: string | null;
  accusedPosition?: AccusedPosition | null;
  incidentDate?: string | null;
  incidentPlace?: string | null;
  courseYear?: number | null;
  groupName?: string | null;
  studyForm?: StudyForm | null;
  anonymous?: boolean;
  reporterName?: string | null;
  reporterEmail?: string | null;
  reporterPhone?: string | null;
}

// ---------------------------------------------------------------- statistika

export interface StatItem {
  key: string;
  label: string;
  count: number;
}

export interface StatsResponse {
  total: number;
  last30Days: number;
  open: number;
  resolved: number;
  rejected: number;
  averageResolutionDays: number | null;
  byStatus: StatItem[];
  byCategory: StatItem[];
  byFaculty: StatItem[];
  byReporterType: StatItem[];
}

export interface FacultyRatingResponse {
  facultyId: number;
  facultyName: string;
  total: number;
  resolved: number;
  closed: number;
  open: number;
  resolutionRate: number;
  averageResolutionDays: number | null;
}

// ---------------------------------------------------------------- kontent

/** Yangilik albomidagi bitta rasm. */
export interface NewsImageResponse {
  id: number;
  url: string;
  originalName: string;
  caption: string | null;
  displayOrder: number;
}

export interface NewsSummaryResponse {
  id: number;
  slug: string;
  title: string;
  summary: string | null;
  coverImageUrl: string | null;
  published: boolean;
  publishedAt: string | null;
  viewCount: number;
  /** Albomdagi rasmlar soni - ro'yxatda "galereya bor" belgisi. */
  imageCount: number;
}

export interface NewsDetailResponse extends NewsSummaryResponse {
  body: string;
  images: NewsImageResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface StaffMemberResponse {
  id: number;
  fullName: string;
  position: string;
  academicDegree: string | null;
  phone: string | null;
  email: string | null;
  receptionHours: string | null;
  photoUrl: string | null;
  displayOrder: number;
  active: boolean;
}

export interface StaticPageResponse {
  id: number;
  slug: string;
  title: string;
  body: string | null;
  displayOrder: number;
  published: boolean;
  updatedAt: string;
}

export interface UsefulLinkResponse {
  id: number;
  title: string;
  url: string;
  description: string | null;
  groupName: string | null;
  displayOrder: number;
  active: boolean;
}

// ---------------------------------------------------------------- so'rovnoma

export interface PollOptionResponse {
  id: number;
  text: string;
  voteCount: number;
  percentage: number;
}

export interface PollResponse {
  id: number;
  question: string;
  description: string | null;
  active: boolean;
  multipleChoice: boolean;
  openForVoting: boolean;
  alreadyVoted: boolean;
  startsAt: string | null;
  endsAt: string | null;
  voterCount: number;
  options: PollOptionResponse[];
}
