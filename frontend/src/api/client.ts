import i18next from 'i18next';
import { getToken, notifyUnauthorized } from '../lib/session';
import type { ApiErrorBody } from './types';

const BASE_URL = '/api/v1';

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

function authHeaders(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
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

  // 401 - token eskirgan yoki bekor qilingan: seansni tozalaymiz.
  if (response.status === 401) {
    notifyUnauthorized();
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

async function send<T>(
  method: string,
  path: string,
  body?: unknown,
  options?: RequestOptions,
): Promise<T> {
  const response = await fetch(buildUrl(path, options?.query), {
    method,
    headers: {
      ...authHeaders(),
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  return handleResponse<T>(response);
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => send<T>('GET', path, undefined, options),

  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    send<T>('POST', path, body, options),

  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    send<T>('PUT', path, body, options),

  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    send<T>('PATCH', path, body, options),

  delete: <T>(path: string, options?: RequestOptions) =>
    send<T>('DELETE', path, undefined, options),

  /**
   * Fayl yuklash. `Content-Type` ataylab qo'yilmaydi: uni brauzer
   * FormData uchun `boundary` bilan birga o'zi to'g'ri yozadi.
   */
  upload: async <T>(path: string, file: File, fieldName = 'file'): Promise<T> => {
    const form = new FormData();
    form.append(fieldName, file);

    const response = await fetch(buildUrl(path), {
      method: 'POST',
      headers: authHeaders(),
      body: form,
    });

    return handleResponse<T>(response);
  },

  /**
   * Bir nechta faylni bitta so'rovda yuboradi.
   *
   * <p>Har bir fayl bir xil nom bilan qo'shiladi - backend uni
   * {@code List<MultipartFile>} sifatida qabul qiladi. Bitta so'rov
   * tanlangani bejiz emas: shunda serverda ham hammasi bir tranzaksiyada
   * saqlanadi va chegara tekshiruvi butun to'plamga nisbatan bajariladi.
   */
  uploadMany: async <T>(path: string, files: File[], fieldName = 'files'): Promise<T> => {
    const form = new FormData();
    for (const file of files) {
      form.append(fieldName, file);
    }

    const response = await fetch(buildUrl(path), {
      method: 'POST',
      headers: authHeaders(),
      body: form,
    });

    return handleResponse<T>(response);
  },
};
