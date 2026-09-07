import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from './client';
import { CSRF_HEADER, hasSessionHint, rememberSession } from '../lib/session';

/**
 * Soxta javob.
 *
 * <p>Haqiqiy `Response` ishlatilmaydi: bizga faqat `status`, `ok`,
 * `content-type` va `json()` kerak, qolgani testni og'irlashtirardi.
 */
function fakeResponse(body: unknown, status = 200): Response {
  return {
    status,
    ok: status >= 200 && status < 300,
    headers: {
      get: (name: string) =>
        name.toLowerCase() === 'content-type' ? 'application/json' : null,
    },
    json: async () => body,
  } as unknown as Response;
}

function clearCookies() {
  for (const entry of document.cookie.split(';')) {
    const name = entry.split('=')[0]?.trim();
    if (name) document.cookie = `${name}=; Max-Age=0; path=/`;
  }
}

function setCsrfCookie(value = 'test-token') {
  document.cookie = `XSRF-TOKEN=${value}; path=/`;
}

function headersOf(call: unknown[]): Record<string, string> {
  return ((call[1] as RequestInit).headers ?? {}) as Record<string, string>;
}

describe('api mijozi', () => {
  beforeEach(() => {
    clearCookies();
    localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('so\'rov cookie bilan yuboriladi va tilni qo\'shadi', async () => {
    const fetchMock = vi.fn().mockResolvedValue(fakeResponse({ id: 1 }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(api.get('/me')).resolves.toEqual({ id: 1 });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/api/v1/me');
    expect(url).toContain('lang=');
    expect((init as RequestInit).credentials).toBe('include');
  });

  it('o\'qish so\'rovida CSRF sarlavhasi yuborilmaydi', async () => {
    setCsrfCookie();
    const fetchMock = vi.fn().mockResolvedValue(fakeResponse({}));
    vi.stubGlobal('fetch', fetchMock);

    await api.get('/news');

    expect(headersOf(fetchMock.mock.calls[0])[CSRF_HEADER]).toBeUndefined();
  });

  it('yozuv so\'rovidan oldin CSRF tokeni olinadi va sarlavhaga qo\'yiladi', async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/auth/csrf')) {
        setCsrfCookie('olingan-token');
        return Promise.resolve(fakeResponse(null, 204));
      }
      return Promise.resolve(fakeResponse({ ok: true }));
    });
    vi.stubGlobal('fetch', fetchMock);

    await api.post('/admin/news', { title: 'sinov' });

    expect(fetchMock.mock.calls[0][0]).toContain('/auth/csrf');
    expect(headersOf(fetchMock.mock.calls[1])[CSRF_HEADER]).toBe('olingan-token');
  });

  it('401 dan keyin seans yangilanadi va so\'rov qaytariladi', async () => {
    setCsrfCookie();
    let meCalls = 0;

    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/auth/refresh')) {
        return Promise.resolve(fakeResponse({ expiresIn: 900 }));
      }
      meCalls += 1;
      return Promise.resolve(
        meCalls === 1 ? fakeResponse({ code: 'error.auth.required' }, 401) : fakeResponse({ id: 7 }),
      );
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(api.get('/me')).resolves.toEqual({ id: 7 });

    expect(meCalls).toBe(2);
    expect(fetchMock.mock.calls.filter(([url]) => String(url).includes('/auth/refresh'))).toHaveLength(1);
  });

  it('yangilash ham 401 bersa seans tugaydi', async () => {
    setCsrfCookie();
    rememberSession();

    const fetchMock = vi.fn().mockResolvedValue(fakeResponse({ code: 'error.auth.required' }, 401));
    vi.stubGlobal('fetch', fetchMock);

    await expect(api.get('/me')).rejects.toBeInstanceOf(ApiError);
    expect(hasSessionHint()).toBe(false);
  });

  it('bir vaqtda kelgan ikki 401 bitta yangilash so\'rovini yuboradi', async () => {
    setCsrfCookie();
    const failedOnce = new Set<string>();

    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/auth/refresh')) {
        // Kechikish ataylab: ikkinchi so'rov ham shu va'daga ulgursin.
        return new Promise((resolve) =>
          setTimeout(() => resolve(fakeResponse({ expiresIn: 900 })), 10),
        );
      }

      const key = url.split('?')[0];
      if (!failedOnce.has(key)) {
        failedOnce.add(key);
        return Promise.resolve(fakeResponse({ code: 'error.auth.required' }, 401));
      }
      return Promise.resolve(fakeResponse({ path: key }));
    });
    vi.stubGlobal('fetch', fetchMock);

    await Promise.all([api.get('/me'), api.get('/complaints/my')]);

    const refreshCalls = fetchMock.mock.calls.filter(([url]) =>
      String(url).includes('/auth/refresh'),
    );
    expect(refreshCalls).toHaveLength(1);
  });

  it('kirish so\'rovi 401 bersa yangilashga urinilmaydi', async () => {
    setCsrfCookie();
    const fetchMock = vi
      .fn()
      .mockResolvedValue(fakeResponse({ code: 'error.auth.badCredentials' }, 401));
    vi.stubGlobal('fetch', fetchMock);

    await expect(api.post('/auth/login', { email: 'a@b.uz', password: 'x' })).rejects.toBeInstanceOf(
      ApiError,
    );

    expect(fetchMock.mock.calls.filter(([url]) => String(url).includes('/auth/refresh'))).toHaveLength(0);
  });

  it('xatolik kodi va maydonlari ApiError ga o\'tadi', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      fakeResponse(
        { code: 'error.validation.failed', message: 'Xato', fields: { title: 'Majburiy' } },
        400,
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(api.get('/news')).rejects.toMatchObject({
      status: 400,
      code: 'error.validation.failed',
    });

    try {
      await api.get('/news');
    } catch (error) {
      expect((error as ApiError).fieldError('title')).toBe('Majburiy');
    }
  });
});
