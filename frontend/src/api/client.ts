import i18next from 'i18next';
import { CSRF_HEADER, notifyUnauthorized, readCsrfToken } from '../lib/session';
import type { ApiErrorBody } from './types';

const BASE_URL = '/api/v1';

/**
 * Seansning o'zi bilan bog'liq yo'llar.
 *
 * <p>Ular 401 qaytarganda seansni yangilashga urinish ma'nosiz: aynan
 * yangilashning o'zi muvaffaqiyatsiz bo'lgan. Bu ro'yxatsiz cheksiz
 * halqa hosil bo'lardi.
 */
const SESSION_PATHS = ['/auth/login', '/auth/refresh', '/auth/logout', '/auth/csrf'];

/**
 * Backend qaytargan xatolik.
 *
 * <p>`code` - tilga bog'liq bo'lmagan kalit. Interfeys aynan shu bo'yicha
 * qaror qabul qilishi kerak (masalan "allaqachon ovoz bergansiz" holatini
 * alohida ko'rsatish), xabar matni esa foydalanuvchiga ko'rsatish uchun.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fields: Record<string, string> | null;

  constructor(
    status: number,
    code: string,
    message: string,
    fields: Record<string, string> | null = null,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fields = fields;
  }

  /** Maydon bo'yicha validatsiya xatoligi. */
  fieldError(field: string): string | undefined {
    return this.fields?.[field];
  }
}

type QueryValue = string | number | boolean | null | undefined;

/**
 * So'rov manzilini yasaydi va unga joriy tilni qo'shadi.
 *
 * <p>Backend tilni faqat `?lang=` dan oladi, shuning uchun uni har bir
 * so'rovga qo'shish shart - aks holda javob standart tilda kelardi.
 */
function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const params = new URLSearchParams();

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== null && value !== undefined && value !== '') {
        params.append(key, String(value));
      }
    }
  }
  params.set('lang', i18next.language || 'uz');

  return `${BASE_URL}${path}?${params.toString()}`;
}

/**
 * Yozuv so'rovlari uchun CSRF sarlavhasi.
 *
 * <p>Cookie hali o'rnatilmagan bo'lishi mumkin - masalan foydalanuvchi
 * to'g'ridan-to'g'ri kirish sahifasini ochgan va hech qanday so'rov
 * bo'lmagan. Shunda avval uni so'rab olamiz.
 */
async function csrfHeader(method: string): Promise<Record<string, string>> {
  if (method === 'GET' || method === 'HEAD') {
    return {};
  }

  let token = readCsrfToken();
  if (!token) {
    await fetch(buildUrl('/auth/csrf'), { credentials: 'include' }).catch(() => undefined);
    token = readCsrfToken();
  }
  return token ? { [CSRF_HEADER]: token } : {};
}

/**
 * Seansni yangilash.
 *
 * <p>Bir vaqtda bir nechta so'rov 401 olishi mumkin (sahifada bir nechta
 * so'rov parallel ketadi). Ularning har biri alohida yangilashga urinsa,
 * tokenlar aylanishi bir-birini bekor qilib qo'yardi - shuning uchun
 * hammasi bitta va'daga (promise) ulanadi.
 */
let refreshing: Promise<boolean> | null = null;

function refreshSession(): Promise<boolean> {
  if (!refreshing) {
    refreshing = runRefresh().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

async function runRefresh(): Promise<boolean> {
  try {
    const response = await fetch(buildUrl('/auth/refresh'), {
      method: 'POST',
      credentials: 'include',
      headers: await csrfHeader('POST'),
    });
    return response.ok;
  } catch {
    // Tarmoq uzilgan bo'lsa ham seansni o'chirilgan deb hisoblamaymiz.
    return false;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }

  if (response.ok) {
    const contentType = response.headers.get('content-type') ?? '';
    return contentType.includes('application/json')
      ? ((await response.json()) as T)
      : (undefined as T);
  }

  let body: ApiErrorBody | null = null;
  try {
    body = (await response.json()) as ApiErrorBody;
  } catch {
    /* javob JSON bo'lmasa quyida umumiy xabar ishlatiladi */
  }

  throw new ApiError(
    response.status,
    body?.code ?? 'error.unknown',
    body?.message ?? `HTTP ${response.status}`,
    body?.fields ?? null,
  );
}

interface RequestOptions {
  query?: Record<string, QueryValue>;
}

/**
 * So'rovni yuboradi va kerak bo'lsa seansni yangilab, bir marta qaytadan urinadi.
 *
 * <p>Kirish tokeni qisqa muddatli (15 daqiqa), shuning uchun 401 odatiy
 * hol - u "chiqib ketdingiz" degani emas. Yangilash tokeni hali amal
 * qilsa, foydalanuvchi buni umuman sezmaydi.
 */
async function request<T>(
  path: string,
  build: () => Promise<RequestInit>,
  options?: RequestOptions,
): Promise<T> {
  const url = buildUrl(path, options?.query);

  let response = await fetch(url, await build());

  if (response.status === 401 && !SESSION_PATHS.includes(path)) {
    if (await refreshSession()) {
      response = await fetch(url, await build());
    }
    if (response.status === 401) {
      notifyUnauthorized();
    }
  }

  return handleResponse<T>(response);
}

function jsonInit(method: string, body?: unknown): () => Promise<RequestInit> {
  return async () => ({
    method,
    credentials: 'include',
    headers: {
      ...(await csrfHeader(method)),
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

/**
 * Fayl yuborish uchun so'rov.
 *
 * <p>`Content-Type` ataylab qo'yilmaydi: uni brauzer FormData uchun
 * `boundary` bilan birga o'zi to'g'ri yozadi.
 */
function formInit(form: FormData): () => Promise<RequestInit> {
  return async () => ({
    method: 'POST',
    credentials: 'include',
    headers: await csrfHeader('POST'),
    body: form,
  });
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, jsonInit('GET'), options),

  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, jsonInit('POST', body), options),

  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, jsonInit('PUT', body), options),

  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, jsonInit('PATCH', body), options),

  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, jsonInit('DELETE'), options),

  upload: <T>(path: string, file: File, fieldName = 'file'): Promise<T> => {
    const form = new FormData();
    form.append(fieldName, file);
    return request<T>(path, formInit(form));
  },

  /**
   * Bir nechta faylni bitta so'rovda yuboradi.
   *
   * <p>Har bir fayl bir xil nom bilan qo'shiladi - backend uni
   * {@code List<MultipartFile>} sifatida qabul qiladi. Bitta so'rov
   * tanlangani bejiz emas: shunda serverda ham hammasi bir tranzaksiyada
   * saqlanadi va chegara tekshiruvi butun to'plamga nisbatan bajariladi.
   */
  uploadMany: <T>(path: string, files: File[], fieldName = 'files'): Promise<T> => {
    const form = new FormData();
    for (const file of files) {
      form.append(fieldName, file);
    }
    return request<T>(path, formInit(form));
  },
};
