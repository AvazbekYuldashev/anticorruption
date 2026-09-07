import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi, type LoginPayload } from '../api/auth';
import type { UserResponse } from '../api/types';
import { forgetSession, hasSessionHint, onUnauthorized, rememberSession } from '../lib/session';

interface AuthState {
  user: UserResponse | null;
  /** Boshlang'ich tekshiruv tugamaguncha true - shu paytda yo'nalish qarori qabul qilinmaydi. */
  initializing: boolean;
  login: (payload: LoginPayload) => Promise<UserResponse>;
  logout: () => Promise<void>;
  /** Profil o'zgargandan keyin joriy foydalanuvchini qaytadan o'qiydi. */
  refresh: () => Promise<void>;
  isStaff: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [initializing, setInitializing] = useState(true);

  /*
   * Chiqish serverda ham bajarilishi kerak: yangilash tokeni bazada
   * qoladi va faqat shu chaqiruv uni bekor qiladi. Tarmoq uzilgan bo'lsa
   * ham interfeys chiqib ketadi - keyingi so'rovda cookie baribir
   * yaroqsiz bo'ladi.
   */
  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      /* server javob bermasa ham mahalliy holatni tozalaymiz */
    }
    forgetSession();
    setUser(null);
  }, []);

  /*
   * Seans tugaganda API mijozi shu funksiyani chaqiradi. Shu tufayli
   * foydalanuvchi 401 olgan zahoti interfeys ham "chiqib ketgan"
   * holatiga o'tadi - eski ma'lumot ekranda qolib qolmaydi.
   */
  useEffect(() => {
    onUnauthorized(() => setUser(null));
  }, []);

  /*
   * Sahifa yangilanganda seansni tiklaymiz. Tokenga bu yerda qaray
   * olmaymiz - u HttpOnly cookie'da. Shuning uchun oddiy belgiga
   * qaraymiz: u bo'lmasa foydalanuvchi mehmon va API ga umuman
   * murojaat qilinmaydi.
   *
   * Kirish tokeni eskirgan bo'lsa `/me` 401 qaytaradi va API mijozi
   * seansni o'zi yangilab, so'rovni qaytaradi - bu yerda alohida
   * ish qilish shart emas.
   */
  useEffect(() => {
    if (!hasSessionHint()) {
      setInitializing(false);
      return;
    }

    let cancelled = false;
    authApi
      .me()
      .then((loaded) => {
        if (!cancelled) setUser(loaded);
      })
      .catch(() => {
        forgetSession();
      })
      .finally(() => {
        if (!cancelled) setInitializing(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (payload: LoginPayload) => {
    const response = await authApi.login(payload);
    rememberSession();
    setUser(response.user);
    return response.user;
  }, []);

  const refresh = useCallback(async () => {
    if (!hasSessionHint()) return;
    setUser(await authApi.me());
  }, []);

  const value = useMemo<AuthState>(
    () => ({
      user,
      initializing,
      login,
      logout,
      refresh,
      isStaff: user?.role === 'MODERATOR' || user?.role === 'ADMIN',
      isAdmin: user?.role === 'ADMIN',
    }),
    [user, initializing, login, logout, refresh],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth faqat AuthProvider ichida ishlatiladi');
  }
  return context;
}
