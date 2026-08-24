import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi, type LoginPayload, type RegisterPayload } from '../api/auth';
import type { UserResponse } from '../api/types';
import { clearToken, getToken, onUnauthorized, setToken } from '../lib/session';

interface AuthState {
  user: UserResponse | null;
  /** Boshlang'ich tekshiruv tugamaguncha true - shu paytda yo'nalish qarori qabul qilinmaydi. */
  initializing: boolean;
  login: (payload: LoginPayload) => Promise<UserResponse>;
  register: (payload: RegisterPayload) => Promise<UserResponse>;
  logout: () => void;
  isStaff: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [initializing, setInitializing] = useState(true);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
  }, []);

  /*
   * Token eskirsa API mijozi shu funksiyani chaqiradi. Shu tufayli
   * foydalanuvchi 401 olgan zahoti interfeys ham "chiqib ketgan"
   * holatiga o'tadi - eski ma'lumot ekranda qolib qolmaydi.
   */
  useEffect(() => {
    onUnauthorized(() => setUser(null));
  }, []);

  /*
   * Sahifa yangilanganda seansni tiklaymiz: token bor bo'lsa, u kimga
   * tegishli ekanini backend'dan so'raymiz. Foydalanuvchi ma'lumotini
   * localStorage'da saqlash mumkin edi, lekin u eskirib qolardi
   * (masalan roli o'zgargan bo'lsa).
   */
  useEffect(() => {
    if (!getToken()) {
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
        clearToken();
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
    setToken(response.accessToken);
    setUser(response.user);
    return response.user;
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const response = await authApi.register(payload);
    setToken(response.accessToken);
    setUser(response.user);
    return response.user;
  }, []);

  const value = useMemo<AuthState>(
    () => ({
      user,
      initializing,
      login,
      register,
      logout,
      isStaff: user?.role === 'MODERATOR' || user?.role === 'ADMIN',
      isAdmin: user?.role === 'ADMIN',
    }),
    [user, initializing, login, register, logout],
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
