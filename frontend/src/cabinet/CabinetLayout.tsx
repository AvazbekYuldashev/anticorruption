import type { ReactNode } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import { useAuth } from '../auth/AuthContext';

/**
 * Shaxsiy kabinet tartibi.
 *
 * <p>Kabinet ommaviy sayt ichida qoladi - yuqoridagi menyu va pastdagi
 * ma'lumotlar joyida turadi. Admin panelidek butun ekranni egallamaydi:
 * fuqaro bu yerga bir necha daqiqaga kiradi va yana saytga qaytadi,
 * shuning uchun uni saytdan uzib qo'yish noqulaylik tug'dirardi.
 *
 * <p>Tailwind ishlatiladi, MUI emas: admin panel MUI ga tayanadi va u
 * faqat xodimlarga yuklanadi. Oddiy tashrifchi uchun o'sha paketni
 * yuklash - bir necha yuz kilobayt ortiqcha kod.
 */

const iconProps = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
};

const icons = {
  overview: (
    <svg {...iconProps} aria-hidden>
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </svg>
  ),
  complaints: (
    <svg {...iconProps} aria-hidden>
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
      <path d="M14 3v5h5" />
      <path d="M9 13h6M9 17h4" />
    </svg>
  ),
  profile: (
    <svg {...iconProps} aria-hidden>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20c0-3.3 3.1-5.5 7-5.5s7 2.2 7 5.5" />
    </svg>
  ),
};

interface NavItem {
  to: string;
  label: string;
  icon: ReactNode;
  end?: boolean;
}

function CabinetNav({ items }: { items: NavItem[] }) {
  return (
    <nav className="no-scrollbar flex gap-1.5 overflow-x-auto lg:flex-col lg:overflow-visible">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) =>
            `relative flex shrink-0 items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-medium
             transition-all duration-150 lg:shrink ${
               isActive
                 ? 'bg-brand-50 dark:bg-brand-500/15 text-brand-700 dark:text-brand-300 shadow-sm'
                 : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800/60 hover:text-slate-900 dark:hover:text-slate-100'
             }`
          }
        >
          {({ isActive }) => (
            <>
              {/*
                Faol bo'limni rang bilan birga shakl ham ko'rsatadi: rang
                ko'rmaydigan odam ham qaysi bo'limda turganini biladi.
                Ustun ko'rinishida chapda, qator ko'rinishida pastda chiziladi.
              */}
              {isActive && (
                <span
                  aria-hidden
                  className="absolute inset-x-3 bottom-0 h-0.5 rounded-full bg-brand-500 lg:inset-x-auto lg:top-1/2 lg:bottom-auto lg:left-1 lg:h-5 lg:w-0.5 lg:-translate-y-1/2"
                />
              )}
              <span className={isActive ? 'text-brand-600 dark:text-brand-300' : 'text-slate-400 dark:text-slate-500'}>{item.icon}</span>
              <span className="whitespace-nowrap">{item.label}</span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

export function CabinetLayout() {
  const { t } = useTranslation();
  const { user } = useAuth();

  const items: NavItem[] = [
    { to: '/my', label: t('cabinet.navOverview'), icon: icons.overview, end: true },
    { to: '/my/complaints', label: t('cabinet.navComplaints'), icon: icons.complaints },
    { to: '/my/profile', label: t('cabinet.navProfile'), icon: icons.profile },
  ];

  return (
    <div className="lg:grid lg:grid-cols-[17rem_1fr] lg:gap-8">
      {/* Yon panel - katta ekranda aylantirilganda ham ko'rinib turadi. */}
      <aside className="mb-6 lg:mb-0">
        <div className="lg:sticky lg:top-28">
          <div className="overflow-hidden rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
            {/*
              Egasining kartochkasi to'q ko'k gradientda: kabinetga kirgan
              odam birinchi navbatda "bu mening joyim" degan belgini ko'radi.
            */}
            <div className="relative overflow-hidden bg-gradient-to-br from-brand-800 to-brand-600 px-4 py-5 text-white">
              <span
                aria-hidden
                className="pointer-events-none absolute -top-8 -right-6 h-24 w-24 rounded-full bg-white/10"
              />
              <div className="relative flex items-center gap-3">
                <span
                  aria-hidden
                  className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white/15 text-lg font-bold ring-1 ring-white/25"
                >
                  {(user?.fullName ?? '?').trim().charAt(0).toUpperCase()}
                </span>
                <span className="min-w-0">
                  <span className="block truncate text-sm font-semibold">{user?.fullName}</span>
                  <span className="block truncate text-xs text-brand-100">{user?.email}</span>
                </span>
              </div>
            </div>

            <div className="p-2">
              <CabinetNav items={items} />
            </div>
          </div>
        </div>
      </aside>

      <div className="min-w-0">
        <Outlet />
      </div>
    </div>
  );
}
