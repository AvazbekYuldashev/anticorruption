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

/**
 * Kirish javobi.
 *
 * <p>Token bu yerda yo'q: u HttpOnly cookie orqali keladi va JavaScript
 * unga kira olmaydi.
 */
export interface AuthResponse {
  /** Kirish tokeni necha sekunddan keyin eskiradi. */
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

export type NewsBlockType = 'HEADING' | 'TEXT' | 'IMAGE' | 'GALLERY';

/** Albom blokidagi bitta rasm. */
export interface NewsBlockImageResponse {
  id: number;
  url: string;
  originalName: string | null;
  caption: string | null;
  displayOrder: number;
}

/**
 * Yangilik mazmunining bir bo'lagi.
 *
 * <p>Matn bloklarida (`HEADING`, `TEXT`) `text`, yakka rasmda `url`,
 * albomda `images` to'ldiriladi. Matn ichida oddiy belgilar bilan
 * formatlash bo'lishi mumkin - `renderRichText` uni chizadi.
 *
 * <p>Bloklar `displayOrder` bo'yicha tartiblangan holda keladi.
 */
export interface NewsBlockResponse {
  id: number;
  type: NewsBlockType;
  text: string | null;
  url: string | null;
  originalName: string | null;
  caption: string | null;
  images: NewsBlockImageResponse[];
  displayOrder: number;
}

export interface NewsSummaryResponse {
  id: number;
  slug: string;
  /** Yangilik qaysi tilda yozilgan: "uz", "uz-cyrl", "ru" yoki "en". */
  languageCode: string;
  title: string;
  summary: string | null;
  coverImageUrl: string | null;
  published: boolean;
  publishedAt: string | null;
  viewCount: number;
  /** Albomdagi rasmlar soni - ro'yxatda "galereya bor" belgisi. */
  imageCount: number;
}

/** Shu maqolaning boshqa tildagi nusxasiga havola. */
export interface NewsTranslationResponse {
  id: number;
  languageCode: string;
  /** Tilning o'z tilidagi nomi: "Русский", "English". */
  languageName: string;
  slug: string;
  title: string;
  published: boolean;
}

export interface NewsDetailResponse extends NewsSummaryResponse {
  /** Matn bloklarining birlashtirilgan nusxasi - qidiruv natijalari uchun. */
  body: string;
  languageName: string;
  blocks: NewsBlockResponse[];
  /** O'zidan tashqari barcha til nusxalari. */
  translations: NewsTranslationResponse[];
  createdAt: string;
  updatedAt: string;
}

/** "Bo'lim haqida" sahifasining bir tildagi matni. */
export interface AboutTranslation {
  languageCode: string;
  /** Tilning o'z tilidagi nomi - faqat javobda keladi. */
  languageName?: string;
  title: string | null;
  body: string | null;
  tasksTitle: string | null;
  tasks: string[];
  goal: string | null;
}

/** "Bo'lim haqida" sahifasi - saytda bitta nusxada. */
export interface AboutSectionResponse {
  /** Javobdagi matn qaysi tilda. */
  languageCode: string;
  title: string | null;
  body: string | null;
  tasksTitle: string | null;
  tasks: string[];
  goal: string | null;
  /** Sahifa to'ldirilganmi: bo'sh bo'lsa menyuda ko'rsatilmaydi. */
  filled: boolean;
  /** Faqat admin javobida to'ldiriladi; saytda bo'sh. */
  translations: AboutTranslation[];
  updatedAt: string | null;
}

/** Xodim ma'lumotlarining bir tildagi varianti. */
export interface StaffTranslation {
  languageCode: string;
  languageName?: string;
  fullName: string | null;
  position: string | null;
  academicDegree: string | null;
  biography: string | null;
  receptionHours: string | null;
}

export interface StaffMemberResponse {
  id: number;
  /** Javobdagi matn qaysi tilda. */
  languageCode: string;
  fullName: string;
  position: string;
  academicDegree: string | null;
  biography: string | null;
  phone: string | null;
  email: string | null;
  receptionHours: string | null;
  photoUrl: string | null;
  displayOrder: number;
  active: boolean;
  /** Faqat admin javobida to'ldiriladi; saytda bo'sh. */
  translations: StaffTranslation[];
}

// ---------------------------------------------------------------- so'rovnoma

export interface PollOptionResponse {
  id: number;
  text: string;
  voteCount: number;
  /** Shu savolga javob berganlarga nisbatan foiz. */
  percentage: number;
  /**
   * Testda shu variant to'g'rimi.
   *
   * So'rovnomada va test ishlanmagunicha `null`: to'g'ri javoblar
   * shakl bilan birga ochilib qolmasligi kerak.
   */
  correct: boolean | null;
}

export interface PollQuestionResponse {
  id: number;
  text: string;
  multipleChoice: boolean;
  required: boolean;
  answeredCount: number;
  displayOrder: number;
  options: PollOptionResponse[];
}

/**
 * So'rovnoma holati.
 *
 * `SCHEDULED` va `STOPPED` faqat admin panelida uchraydi: boshlanmagan
 * va qo'lda to'xtatilgan so'rovnoma ochiq ro'yxatga tushmaydi.
 */
export type PollStatus = 'DRAFT' | 'SCHEDULED' | 'OPEN' | 'STOPPED' | 'CLOSED';

/** So'rovnoma (fikr so'rash) yoki test (to'g'ri javobli viktorina). */
export type PollType = 'SURVEY' | 'QUIZ';

/** Bitta savol bo'yicha test natijasi. */
export interface QuizQuestionResult {
  questionId: number;
  correct: boolean;
  chosenOptionIds: number[];
  correctOptionIds: number[];
}

/** Test bo'yicha hisobot - faqat admin panelida keladi. */
export interface QuizStatisticsResponse {
  participants: number;
  /** O'rtacha nechta savolga to'g'ri javob berilgan. */
  averageCorrect: number;
  /** O'rtacha natija, foizda. */
  averagePercentage: number;
  questions: {
    questionId: number;
    text: string;
    answeredCount: number;
    correctCount: number;
    /** Javob berganlarga nisbatan to'g'ri javob ulushi, foizda. */
    correctRate: number;
  }[];
}

/** Test yakunidagi natija - faqat javob yuborilgandan keyin keladi. */
export interface QuizResultResponse {
  questionCount: number;
  correctCount: number;
  /** To'g'ri javoblar ulushi, foizda. */
  percentage: number;
  questions: QuizQuestionResult[];
}

export interface PollResponse {
  id: number;
  title: string;
  description: string | null;
  type: PollType;
  typeLabel: string;
  active: boolean;
  status: PollStatus;
  statusLabel: string;
  openForVoting: boolean;
  alreadyVoted: boolean;
  startsAt: string | null;
  endsAt: string | null;
  /** Qo'lda to'xtatilgan vaqt; to'xtatilmagan bo'lsa null. */
  stoppedAt: string | null;
  /** Nechanchi marta o'tkazilayotgani. */
  runNumber: number;
  /** Oldingi o'tkazish; birinchisida null. */
  previousPollId: number | null;
  voterCount: number;
  questionCount: number;
  questions: PollQuestionResponse[];
  /** Test yakunlangandagi natija; qolgan hollarda null. */
  quizResult: QuizResultResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface PollStatisticsResponse {
  pollId: number;
  title: string;
  status: PollStatus;
  statusLabel: string;
  startsAt: string | null;
  endsAt: string | null;
  stoppedAt: string | null;
  runNumber: number;
  previousPollId: number | null;
  voterCount: number;
  questionCount: number;
  /** Savollarning o'rtacha javoblanish darajasi, foizda. */
  completionRate: number;
  firstVoteAt: string | null;
  lastVoteAt: string | null;
  questions: PollQuestionResponse[];
  /** Test bo'yicha ball hisoboti; so'rovnomada null. */
  quiz: QuizStatisticsResponse | null;
}
