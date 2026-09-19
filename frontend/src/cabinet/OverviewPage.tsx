import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';

import { complaintsApi } from '../api/complaints';
import type { ComplaintSummaryResponse } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ErrorBox, Spinner, StatusBadge } from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatDate, formatNumber } from '../lib/format';

/**
 * Kabinetning bosh ekrani.
 *
 * <p>Bu yerda yangi ma'lumot yaratilmaydi: hammasi `/complaints/my` dan
 * keladi va shu sahifada jamlanadi. Maqsadi - fuqaro kirgan zahoti
 * "murojaatlarim qaysi bosqichda" degan savolga javob olsin.
 */

/*
 * Holatlar kesimini hisoblash uchun bir sahifada shuncha murojaat olinadi.
 * Bundan ko'p bo'lsa hisob to'liq chiqmaydi - shuning uchun bunday holatda
 * kesim umuman ko'rsatilmaydi (pastga qarang). Serverda "mening
 * statistikam" endpoint'i yo'q, mavjud bo'lmagan raqamni chizishdan ko'ra
 * ko'rsatmagan yaxshi.
 */
const COUNT_LIMIT = 100;

function StatTile({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: 'brand' | 'good' | 'warning' | 'slate';
}) {
  /*
   * Rang ma'no tashiydi: hal qilingani - yashil, jarayondagisi - sariq.
   * Shu tufayli yozuvni o'qimasdan ham holat ko'rinadi. Yozuv baribir
   * yozilgan: rang yolg'iz ma'no tashimasligi kerak.
   */
  const tones = {
    brand: 'from-brand-500 to-brand-700 shadow-brand-700/25',
    good: 'from-emerald-500 to-emerald-700 shadow-emerald-700/25',
    warning: 'from-amber-400 to-amber-600 shadow-amber-600/25',
    slate: 'from-slate-400 to-slate-600 shadow-slate-600/20',
  };

  return (
    <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm transition-all duration-150 hover:-translate-y-px hover:shadow-md">
      <span
        className={`flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br text-lg font-bold text-white shadow-md ${tones[tone]}`}
      >
        {formatNumber(value)}
      </span>
      <p className="mt-3 text-sm font-medium text-slate-600 dark:text-slate-400">{label}</p>
    </div>
  );
}

function QuickAction({ to, label, hint, icon }: { to: string; label: string; hint: string; icon: ReactNode }) {
  return (
    <Link
      to={to}
      className="flex items-center gap-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm transition-all duration-150 hover:-translate-y-px hover:border-brand-200 dark:hover:border-brand-400/30 hover:shadow-md"
    >
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-300">
        {icon}
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-semibold text-slate-800 dark:text-slate-200">{label}</span>
        <span className="block truncate text-xs text-slate-500 dark:text-slate-400">{hint}</span>
      </span>
    </Link>
  );
}

const iconProps = {
  width: 20,
  height: 20,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
};

function RecentRow({ complaint }: { complaint: ComplaintSummaryResponse }) {
  return (
    <Link
      to={`/my/complaints/${complaint.id}`}
      className="flex flex-wrap items-center justify-between gap-3 rounded-xl px-3 py-3 transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/60"
    >
      <span className="min-w-0">
        <span className="block font-mono text-xs text-slate-500 dark:text-slate-400">{complaint.trackingCode}</span>
        <span className="mt-0.5 block truncate text-sm font-medium text-slate-800 dark:text-slate-200">
          {complaint.title}
        </span>
      </span>
      <span className="flex items-center gap-3">
        <span className="text-xs text-slate-400 dark:text-slate-500">{formatDate(complaint.createdAt)}</span>
        <StatusBadge status={complaint.status} label={complaint.statusLabel} />
      </span>
    </Link>
  );
}

export function CabinetOverviewPage() {
  const { t } = useTranslation();
  const { user } = useAuth();

  const query = useQuery({
    queryKey: ['complaints', 'mine', 'overview'],
    queryFn: () => complaintsApi.mine(0, COUNT_LIMIT),
  });

  if (query.isPending) return <Spinner />;
  if (query.isError)
    return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  const { content, totalElements } = query.data;

  // Barcha murojaat yuklangan bo'lsagina kesim to'g'ri chiqadi.
  const countsAreComplete = totalElements <= COUNT_LIMIT;
  const countOf = (...statuses: string[]) =>
    content.filter((complaint) => statuses.includes(complaint.status)).length;

  return (
    <div className="space-y-6">
      {/* Salomlashuv */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-800 via-brand-700 to-brand-600 p-6 text-white shadow-md shadow-brand-900/20">
        <span
          aria-hidden
          className="pointer-events-none absolute -right-10 -bottom-14 h-44 w-44 rounded-full bg-white/10"
        />
        <div className="relative">
          <h1 className="text-xl font-semibold sm:text-2xl">
            {t('cabinet.greeting', { name: user?.fullName ?? '' })}
          </h1>
          <p className="mt-2 max-w-xl text-sm text-brand-100">{t('cabinet.overviewHint')}</p>
        </div>
      </div>

      {/* Raqamlar */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatTile label={t('cabinet.statTotal')} value={totalElements} tone="brand" />
        {countsAreComplete && (
          <>
            <StatTile label={t('cabinet.statResolved')} value={countOf('RESOLVED')} tone="good" />
            <StatTile
              label={t('cabinet.statInReview')}
              value={countOf('IN_REVIEW', 'NEED_INFO')}
              tone="warning"
            />
            <StatTile label={t('cabinet.statNew')} value={countOf('NEW')} tone="slate" />
          </>
        )}
      </div>

      {/* Oxirgi murojaatlar */}
      {/* Card emas: bu yerda ichki chekka qatorlarga beriladi, kartochkaga emas -
          shunda qator ustiga borilganda yoritish chekkagacha yetadi. */}
      <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm">
        <div className="mb-2 flex items-center justify-between px-3">
          <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{t('cabinet.recentTitle')}</h2>
          {content.length > 0 && (
            <Link
              to="/my/complaints"
              className="text-sm font-medium text-brand-600 dark:text-brand-300 transition-colors hover:text-brand-700 dark:hover:text-brand-300 hover:underline"
            >
              {t('cabinet.viewAll')} →
            </Link>
          )}
        </div>

        {content.length === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-slate-500 dark:text-slate-400">{t('my.empty')}</p>
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {content.slice(0, 4).map((complaint) => (
              <RecentRow key={complaint.id} complaint={complaint} />
            ))}
          </div>
        )}
      </div>

      {/* Tezkor harakatlar */}
      <div className="grid gap-4 sm:grid-cols-2">
        <QuickAction
          to="/submit"
          label={t('cabinet.quickSubmit')}
          hint={t('cabinet.quickSubmitHint')}
          icon={
            <svg {...iconProps} aria-hidden>
              <path d="M12 5v14M5 12h14" />
            </svg>
          }
        />
        <QuickAction
          to="/track"
          label={t('cabinet.quickTrack')}
          hint={t('cabinet.quickTrackHint')}
          icon={
            <svg {...iconProps} aria-hidden>
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
          }
        />
      </div>
    </div>
  );
}
