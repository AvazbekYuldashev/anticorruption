import { useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import { useAuth } from '../auth/AuthContext';
import { LanguageSwitcher } from './LanguageSwitcher';

/** Asosiy menyu. Matnli sahifalar bunga bazadan qo'shiladi. */
function useNavItems() {
  const { t } = useTranslation();

  // Sahifalar admin panelidan qo'shiladi, shuning uchun menyu ham
  // dinamik: "Bo'lim haqida" kabi bo'limlar kod o'zgarmasdan paydo bo'ladi.
  const { data: pages } = useQuery({
    queryKey: ['pages', 'menu'],
    queryFn: contentApi.pages,
    staleTime: 5 * 60 * 1000,
  });

  const fixed = [
    { to: '/', label: t('nav.home') },
    { to: '/submit', label: t('nav.submit') },
    { to: '/track', label: t('nav.track') },
    { to: '/register', label: t('nav.register') },
    { to: '/news', label: t('nav.news') },
    { to: '/staff', label: t('nav.staff') },
    { to: '/polls', label: t('nav.polls') },
    { to: '/stats', label: t('nav.stats') },
  ];

  const dynamic = (pages ?? []).map((page) => ({
    to: `/pages/${page.slug}`,
    label: page.title,
  }));

  return [...fixed, ...dynamic, { to: '/links', label: t('nav.links') }];
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
    <footer className="mt-16 border-t border-slate-200 bg-white">
      <div className="mx-auto max-w-7xl px-4 py-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-start gap-3">
            <img
              src="/brand/asti-logo-64.png"
              alt=""
              width={40}
              height={40}
              className="h-10 w-10 shrink-0"
            />
            <div>
              <p className="text-sm font-semibold text-slate-800">{t('site.institute')}</p>
              <p className="text-sm text-slate-700">{t('site.name')}</p>
              <p className="mt-1 text-xs text-slate-500">{t('site.tagline')}</p>
              <a
                href="https://astiedu.uz"
                target="_blank"
                rel="noopener noreferrer"
                className="mt-2 inline-block text-xs text-brand-600 hover:underline"
              >
                {t('site.instituteSite')} · astiedu.uz
              </a>
            </div>
          </div>
          <div className="flex flex-wrap gap-4 text-xs text-slate-500">
            <Link to="/submit" className="hover:text-brand-700">
              {t('nav.submit')}
            </Link>
            <Link to="/track" className="hover:text-brand-700">
              {t('nav.track')}
            </Link>
            <Link to="/register" className="hover:text-brand-700">
              {t('nav.register')}
            </Link>
            <Link to="/stats" className="hover:text-brand-700">
              {t('nav.stats')}
            </Link>
          </div>
        </div>
        <p className="mt-6 border-t border-slate-100 pt-4 text-xs text-slate-400">
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
