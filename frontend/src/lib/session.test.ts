import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  CSRF_HEADER,
  forgetSession,
  hasSessionHint,
  notifyUnauthorized,
  onUnauthorized,
  readCsrfToken,
  rememberSession,
} from './session';

/** Har bir test toza cookie va toza localStorage bilan boshlanadi. */
function clearCookies() {
  for (const entry of document.cookie.split(';')) {
    const name = entry.split('=')[0]?.trim();
    if (name) document.cookie = `${name}=; Max-Age=0; path=/`;
  }
}

describe('session', () => {
  beforeEach(() => {
    clearCookies();
    localStorage.clear();
  });

  it('CSRF sarlavhasining nomi backend kutgani bilan bir xil', () => {
    expect(CSRF_HEADER).toBe('X-XSRF-TOKEN');
  });

  it('CSRF tokenini cookie dan o\'qiydi', () => {
    document.cookie = 'XSRF-TOKEN=abc-123; path=/';
    expect(readCsrfToken()).toBe('abc-123');
  });

  it('cookie boshqa cookie\'lar orasida bo\'lsa ham topiladi', () => {
    document.cookie = 'other=1; path=/';
    document.cookie = 'XSRF-TOKEN=token-2; path=/';
    document.cookie = 'another=2; path=/';
    expect(readCsrfToken()).toBe('token-2');
  });

  it('cookie yo\'q bo\'lsa null qaytadi', () => {
    expect(readCsrfToken()).toBeNull();
  });

  it('kodlangan qiymat ochib beriladi', () => {
    document.cookie = `XSRF-TOKEN=${encodeURIComponent('a b+c')}; path=/`;
    expect(readCsrfToken()).toBe('a b+c');
  });

  it('seans belgisi qo\'yiladi va o\'chiriladi', () => {
    expect(hasSessionHint()).toBe(false);

    rememberSession();
    expect(hasSessionHint()).toBe(true);

    forgetSession();
    expect(hasSessionHint()).toBe(false);
  });

  it('seans tugaganda belgisi o\'chadi va tinglovchi chaqiriladi', () => {
    const handler = vi.fn();
    rememberSession();
    onUnauthorized(handler);

    notifyUnauthorized();

    expect(handler).toHaveBeenCalledTimes(1);
    expect(hasSessionHint()).toBe(false);
  });
});
