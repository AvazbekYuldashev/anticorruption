import { useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import { LanguageSwitcher } from './LanguageSwitcher';

/**
 * Asosiy menyu.
 *
 * <p>Murojaatlar reyestri ochiq saytda ko'rsatilmaydi: u xizmat ro'yxati
 * bo'lib qoldi va faqat xodimlarga ochiladi.
 */
function useNavItems() {
  const { t } = useTranslation();
  const { isStaff } = useAuth();

  return [
    { to: '/', label: t('nav.home') },
    { to: '/submit', label: t('nav.submit') },
    { to: '/track', label: t('nav.track') },
    ...(isStaff ? [{ to: '/register', label: t('nav.register') }] : []),
    { to: '/news', label: t('nav.news') },
    { to: '/staff', label: t('nav.staff') },
    { to: '/about', label: t('nav.about') },
    { to: '/polls', label: t('nav.polls') },
    { to: '/tests', label: t('nav.tests') },
    { to: '/stats', label: t('nav.stats') },
  ];
}

function Header() {
  const { t } = useTranslation();
  const { user, isStaff, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const items = useNavItems();
  const location = useLocation();

  return (
    <header className="sticky top-0 z-40 shadow-sm">
      {/* Yuqori qator: nom, til, hisob */}
      <div className="bg-brand-800 text-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-2.5">
          <Link to="/" className="flex min-w-0 items-center gap-3">
            {/*
              Logotip loyihaning ichida saqlanadi (public/brand), tashqi saytdan
              yuklanmaydi: institut sayti o'zgarsa ham portal ko'rinishi buzilmaydi.
            */}
            <img
              src="/brand/asti-logo-64.png"
              srcSet="/brand/asti-logo-64.png 1x, /brand/asti-logo-256.png 4x"
              alt={t('site.institute')}
              width={40}
              height={40}
              className="h-10 w-10 shrink-0 rounded bg-white/95 p-0.5"
            />
            <span className="min-w-0">
              <span className="block truncate text-[11px] leading-tight text-brand-100 sm:text-xs">
                {t('site.institute')}
              </span>
              <span className="block truncate text-sm leading-tight font-semibold sm:text-base">
                {t('site.name')}
              </span>
            </span>
          </Link>

          <div className="flex items-center gap-3">
            <div className="hidden sm:block">
              <LanguageSwitcher />
            </div>
            <div className="sm:hidden">
              <LanguageSwitcher compact />
            </div>

            <div className="hidden items-center gap-2 border-l border-white/20 pl-3 md:flex">
              {user ? (
                <>
                  {isStaff && (
                    <Link to="/admin" className="text-xs font-medium text-white/90 hover:text-white">
                      {t('nav.admin')}
                    </Link>
                  )}
                  <Link
                    to="/my"
                    className="text-xs font-medium text-white/90 hover:text-white"
                  >
                    {t('nav.myComplaints')}
                  </Link>
                  <button
                    type="button"
                    onClick={logout}
                    className="text-xs font-medium text-white/70 hover:text-white"
                  >
                    {t('nav.logout')}
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login" className="text-xs font-medium text-white/90 hover:text-white">
                    {t('nav.login')}
                  </Link>
                  <Link
                    to="/signup"
                    className="rounded bg-white/15 px-2.5 py-1 text-xs font-medium hover:bg-white/25"
                  >
                    {t('nav.signUp')}
                  </Link>
                </>
              )}
            </div>

            <button
              type="button"
              onClick={() => setMobileOpen((open) => !open)}
              aria-expanded={mobileOpen}
              className="rounded p-1.5 hover:bg-white/10 lg:hidden"
              aria-label={t('nav.menu')}
            >
              <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor" aria-hidden>
                <path
                  fillRule="evenodd"
                  d="M3 5.5h14a.75.75 0 000-1.5H3a.75.75 0 000 1.5zm0 5h14a.75.75 0 000-1.5H3a.75.75 0 000 1.5zm0 5h14a.75.75 0 000-1.5H3a.75.75 0 000 1.5z"
                />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Pastki qator: asosiy menyu */}
      <nav className="hidden bg-white lg:block">
        <div className="mx-auto flex max-w-7xl flex-wrap gap-x-1 px-4">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `border-b-2 px-3 py-3 text-sm font-medium transition-colors ${
                  isActive
                    ? 'border-brand-600 text-brand-700'
                    : 'border-transparent text-slate-600 hover:border-slate-300 hover:text-slate-900'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      </nav>

      {/* Mobil menyu */}
      {mobileOpen && (
        <nav className="border-t border-slate-200 bg-white lg:hidden">
          <div className="mx-auto max-w-7xl px-4 py-2">
            {items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                onClick={() => setMobileOpen(false)}
                className={({ isActive }) =>
                  `block rounded px-3 py-2.5 text-sm font-medium ${
                    isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-700 hover:bg-slate-50'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}

            <div className="mt-2 flex flex-col gap-1 border-t border-slate-200 pt-2">
              {user ? (
                <>
                  {isStaff && (
                    <Link
                      to="/admin"
                      onClick={() => setMobileOpen(false)}
                      className="rounded px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                    >
                      {t('nav.admin')}
                    </Link>
                  )}
                  <Link
                    to="/my"
                    onClick={() => setMobileOpen(false)}
                    className="rounded px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                  >
                    {t('nav.myComplaints')}
                  </Link>
                  <button
                    type="button"
                    onClick={() => {
                      logout();
                      setMobileOpen(false);
                    }}
                    className="rounded px-3 py-2.5 text-left text-sm font-medium text-slate-500 hover:bg-slate-50"
                  >
                    {t('nav.logout')}
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    state={{ from: location.pathname }}
                    onClick={() => setMobileOpen(false)}
                    className="rounded px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                  >
                    {t('nav.login')}
                  </Link>
                  <Link
                    to="/signup"
                    onClick={() => setMobileOpen(false)}
                    className="rounded px-3 py-2.5 text-sm font-medium text-brand-700 hover:bg-brand-50"
                  >
                    {t('nav.signUp')}
                  </Link>
                </>
              )}
            </div>
          </div>
        </nav>
      )}
    </header>
  );
}

function Footer() {
  const { t } = useTranslation();

  return (
    <footer className="mt-16 bg-brand-900 text-brand-100">
      <div className="mx-auto max-w-7xl px-4 py-10">
        <div className="grid gap-8 md:grid-cols-3">
          {/* Kim */}
          <div className="flex items-start gap-3">
            <img
              src="/brand/asti-logo-64.png"
              alt=""
              width={44}
              height={44}
              className="h-11 w-11 shrink-0 rounded-full bg-white/95 p-1"
            />
            <div>
              <p className="text-sm font-semibold tracking-wide text-white uppercase">
                {t('site.institute')}
              </p>
              <p className="mt-0.5 text-sm text-brand-200">{t('site.name')}</p>
              <p className="mt-2 text-xs text-brand-300">{t('site.tagline')}</p>
            </div>
          </div>

          {/* Bo'limlar */}
          <nav className="text-sm">
            <p className="mb-3 font-semibold text-white">{t('nav.menu')}</p>
            <ul className="space-y-2">
              {[
                { to: '/submit', label: t('nav.submit') },
                { to: '/track', label: t('nav.track') },
                { to: '/news', label: t('nav.news') },
                { to: '/about', label: t('nav.about') },
                { to: '/stats', label: t('nav.stats') },
              ].map((item) => (
                <li key={item.to}>
                  <Link to={item.to} className="text-brand-200 transition-colors hover:text-white">
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          {/* Aloqa */}
          <div className="text-sm">
            <p className="mb-3 font-semibold text-white">{t('site.instituteSite')}</p>
            <a
              href="https://astiedu.uz"
              target="_blank"
              rel="noopener noreferrer"
              className="text-brand-200 transition-colors hover:text-white"
            >
              astiedu.uz
            </a>
          </div>
        </div>

        <p className="mt-8 border-t border-white/10 pt-5 text-xs text-brand-300">
          © {new Date().getFullYear()} {t('site.institute')} · {t('site.official')}
        </p>
      </div>
    </footer>
  );
}


export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
