import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import { pollsApi } from '../api/polls';
import { statsApi } from '../api/stats';
import { Card } from '../components/ui';
import { formatDate, formatNumber } from '../lib/format';
import { PollListItem } from './PollsPage';

/**
 * Raqamli kartochka.
 *
 * <p>Rang ma'no tashiydi: umumiy son - ko'k, hal qilingani - yashil,
 * jarayondagisi - sariq. Shu tufayli yozuvni o'qimasdan ham holat
 * ko'rinib turadi.
 */
function StatTile({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: 'blue' | 'green' | 'amber' | 'slate';
}) {
  const tones = {
    blue: 'bg-brand-50 text-brand-700',
    green: 'bg-emerald-50 text-emerald-700',
    amber: 'bg-amber-50 text-amber-700',
    slate: 'bg-slate-100 text-slate-700',
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm transition-shadow hover:shadow-md">
      <div
        className={`mx-auto flex h-14 w-14 items-center justify-center rounded-full text-xl font-bold ${tones[tone]}`}
      >
        {formatNumber(value)}
      </div>
      <div className="mt-3 text-sm font-medium text-slate-700">{label}</div>
    </div>
  );
}

/** Bo'lim sarlavhasi - markazda, ostida qisqa rangli chiziq bilan. */
function SectionTitle({ children }: { children: string }) {
  return (
    <div className="mb-6 text-center">
      <h2 className="text-xl font-semibold text-slate-900 sm:text-2xl">{children}</h2>
      <span className="mx-auto mt-2 block h-1 w-12 rounded-full bg-brand-500" />
    </div>
  );
}

/**
 * Bosh sahifadagi tezkor o'tish kartochkasi.
 *
 * <p>Rasmiy portallarda odat bo'lgan ko'rinish: belgi, ostida bo'lim nomi.
 * Tashrifchi matn o'qimasdan ham kerakli bo'limni topadi.
 */
function QuickTile({ to, label, icon }: { to: string; label: string; icon: ReactNode }) {
  return (
    <Link
      to={to}
      className="flex flex-col items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-6 text-center shadow-sm transition-all hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md"
    >
      <span className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-50 text-brand-600">
        {icon}
      </span>
      <span className="text-sm font-medium text-slate-700">{label}</span>
    </Link>
  );
}

/* Belgilar tashqi kutubxonadan emas - oltitasi uchun butun paket ortiqcha. */
const iconProps = {
  width: 22,
  height: 22,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
};

const icons = {
  submit: (
    <svg {...iconProps}>
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
    </svg>
  ),
  track: (
    <svg {...iconProps}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
  ),
  stats: (
    <svg {...iconProps}>
      <path d="M3 3v18h18" />
      <path d="m7 14 3-4 4 3 5-7" />
    </svg>
  ),
  about: (
    <svg {...iconProps}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 16v-5M12 8h.01" />
    </svg>
  ),
  staff: (
    <svg {...iconProps}>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20a6.5 6.5 0 0 1 13 0M17 11a3 3 0 1 0-2-5.2M18 20a5.6 5.6 0 0 0-2.2-4.4" />
    </svg>
  ),
  news: (
    <svg {...iconProps}>
      <path d="M4 5h11a1 1 0 0 1 1 1v13H5a1 1 0 0 1-1-1z" />
      <path d="M16 9h3a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1M7 9h6M7 13h6M7 16h4" />
    </svg>
  ),
};

export function HomePage() {
  const { t } = useTranslation();

  const stats = useQuery({ queryKey: ['stats', 'public'], queryFn: statsApi.publicStats });
  const news = useQuery({ queryKey: ['news', 'latest'], queryFn: () => contentApi.news(undefined, 0, 3) });
  // Bosh sahifada faqat so'rovnoma ko'rsatiladi - testlar o'z bo'limida.
  const polls = useQuery({
    queryKey: ['polls', 'active', 'SURVEY'],
    queryFn: () => pollsApi.active('SURVEY'),
  });

  // Bosh sahifada faqat bitta so'rovnoma ko'rsatiladi - eng yangisi.
  const featuredPoll = polls.data?.[0];

  return (
    <div className="space-y-12">
      {/*
        Bosh banner sahifaning chetigacha cho'ziladi: rasmiy portallarda
        birinchi ekran shunday to'liq kenglikda bo'ladi.
      */}
      <section className="relative -mx-4 -mt-8 overflow-hidden bg-gradient-to-br from-brand-900 via-brand-800 to-brand-700 px-4 py-14 text-white sm:py-20">
        {/* Fondagi yirik gerb - matnga xalaqit bermasligi uchun juda xira. */}
        <img
          src="/brand/asti-logo-256.png"
          alt=""
          aria-hidden
          className="pointer-events-none absolute -right-10 -bottom-16 w-72 opacity-10 sm:w-96"
        />

        <div className="relative mx-auto max-w-3xl text-center">
          <div className="flex flex-col items-center gap-3">
            <img
              src="/brand/asti-logo-256.png"
              alt=""
              width={64}
              height={64}
              className="h-16 w-16 rounded-full bg-white/95 p-1.5"
            />
            <p className="text-xs font-semibold tracking-[0.18em] text-brand-100 uppercase sm:text-sm">
              {t('site.institute')}
            </p>
            <p className="text-xs tracking-[0.14em] text-brand-200 uppercase">{t('site.name')}</p>
          </div>

          <h1 className="mt-8 text-2xl leading-tight font-bold sm:text-4xl">
            {t('home.heroTitle')}
          </h1>
          <p className="mx-auto mt-4 max-w-2xl text-sm text-brand-100 sm:text-base">
            {t('home.heroText')}
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to="/submit"
              className="rounded-lg bg-rose-600 px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-rose-900/20 transition-colors hover:bg-rose-700"
            >
              {t('home.ctaSubmit')}
            </Link>
            <Link
              to="/track"
              className="rounded-lg border border-white/40 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-white/10"
            >
              {t('home.ctaTrack')}
            </Link>
          </div>
        </div>
      </section>

      {/* Tezkor o'tish bo'limlari - banner ostida, alohida qator bo'lib turadi. */}
      <section>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
          <QuickTile to="/submit" label={t('nav.submit')} icon={icons.submit} />
          <QuickTile to="/track" label={t('nav.track')} icon={icons.track} />
          <QuickTile to="/stats" label={t('nav.stats')} icon={icons.stats} />
          <QuickTile to="/about" label={t('nav.about')} icon={icons.about} />
          <QuickTile to="/staff" label={t('nav.staff')} icon={icons.staff} />
          <QuickTile to="/news" label={t('nav.news')} icon={icons.news} />
        </div>
      </section>

      {/* Raqamlar */}
      {stats.data && (
        <section>
          <SectionTitle>{t('home.statsTitle')}</SectionTitle>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatTile label={t('home.statTotal')} value={stats.data.total} tone="blue" />
            <StatTile label={t('home.statResolved')} value={stats.data.resolved} tone="green" />
            <StatTile label={t('home.statOpen')} value={stats.data.open} tone="amber" />
            <StatTile label={t('home.statLast30')} value={stats.data.last30Days} tone="slate" />
          </div>
          <div className="mt-4 text-center">
            <Link
              to="/stats"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-brand-600 hover:underline"
            >
              {t('stats.title')} <span aria-hidden>→</span>
            </Link>
          </div>
        </section>
      )}

      {/* Qanday ishlaydi */}
      <section>
        <SectionTitle>{t('home.howTitle')}</SectionTitle>
        <div className="grid gap-5 md:grid-cols-3">
          {[1, 2, 3].map((step) => (
            <div
              key={step}
              className="relative rounded-2xl border border-slate-200 bg-white p-6 pt-8 shadow-sm transition-shadow hover:shadow-md"
            >
              {/* Tartib raqami kartochka chetiga chiqarilgan - qadamlar ketma-ketligi darrov o'qiladi. */}
              <span className="absolute -top-4 left-6 flex h-9 w-9 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white shadow-md shadow-brand-900/20">
                {step}
              </span>
              <h3 className="font-semibold text-slate-900">{t(`home.step${step}Title`)}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">{t(`home.step${step}Text`)}</p>
            </div>
          ))}
        </div>
      </section>

      <div className="grid gap-8 lg:grid-cols-3">
        {/* Yangiliklar */}
        <section className="lg:col-span-2">
          <div className="mb-5 flex items-center justify-between border-b border-slate-200 pb-3">
            <h2 className="text-lg font-semibold text-slate-900">{t('home.newsTitle')}</h2>
            <Link
              to="/news"
              className="text-sm font-medium text-brand-600 transition-colors hover:text-brand-700"
            >
              {t('home.newsMore')} →
            </Link>
          </div>

          {news.data && news.data.content.length > 0 ? (
            <div className="space-y-4">
              {news.data.content.map((item) => (
                <Link
                  key={item.id}
                  to={`/news/${item.slug}`}
                  className="group flex gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition-all hover:border-brand-200 hover:shadow-md"
                >
                  {/* Rasmi yo'q yangilikda bo'sh ramka qoldirilmaydi - matn butun enni egallaydi. */}
                  {item.coverImageUrl && (
                    <img
                      src={item.coverImageUrl}
                      alt=""
                      className="hidden h-24 w-32 shrink-0 rounded-xl object-cover sm:block"
                    />
                  )}
                  <div className="flex min-w-0 flex-col">
                    <h3 className="font-semibold text-slate-900 group-hover:text-brand-700">
                      {item.title}
                    </h3>
                    {item.summary && (
                      <p className="mt-1.5 line-clamp-2 text-sm text-slate-600">{item.summary}</p>
                    )}
                    <div className="mt-auto flex items-center gap-2 pt-3 text-xs text-slate-400">
                      <span className="font-semibold text-brand-600">{t('site.badge')}</span>
                      <span aria-hidden>•</span>
                      <span>{formatDate(item.publishedAt)}</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <Card>
              <p className="text-sm text-slate-500">{t('news.empty')}</p>
            </Card>
          )}
        </section>

        {/* Yon panel: so'rovnoma */}
        <div className="space-y-8">
          {featuredPoll && (
            <section>
              <div className="mb-5 flex items-center justify-between border-b border-slate-200 pb-3">
                <h2 className="text-lg font-semibold text-slate-900">{t('home.pollTitle')}</h2>
                <Link
                  to="/polls"
                  className="text-sm font-medium text-brand-600 transition-colors hover:text-brand-700"
                >
                  {t('polls.title')} →
                </Link>
              </div>
              <PollListItem poll={featuredPoll} />
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
