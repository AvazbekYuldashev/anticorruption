import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import { pollsApi } from '../api/polls';
import { statsApi } from '../api/stats';
import { Button, Card } from '../components/ui';
import { formatDate, formatNumber } from '../lib/format';
import { PollCard } from './PollsPage';

function StatTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 text-center">
      <div className="text-2xl font-semibold text-brand-700 sm:text-3xl">{formatNumber(value)}</div>
      <div className="mt-1 text-xs text-slate-500">{label}</div>
    </div>
  );
}

export function HomePage() {
  const { t } = useTranslation();

  const stats = useQuery({ queryKey: ['stats', 'public'], queryFn: statsApi.publicStats });
  const news = useQuery({ queryKey: ['news', 'latest'], queryFn: () => contentApi.news(undefined, 0, 3) });
  const polls = useQuery({ queryKey: ['polls', 'active'], queryFn: pollsApi.active });
  const links = useQuery({ queryKey: ['links'], queryFn: contentApi.links });

  // Bosh sahifada faqat bitta so'rovnoma ko'rsatiladi - eng yangisi.
  const featuredPoll = polls.data?.[0];

  return (
    <div className="space-y-12">
      {/* Asosiy chaqiruv */}
      <section className="overflow-hidden rounded-2xl bg-brand-800 px-6 py-12 text-white sm:px-10 sm:py-16">
        <div className="max-w-3xl">
          {/* Saytning kimga tegishli ekani birinchi ekrandayoq ko'rinib tursin. */}
          <div className="mb-6 flex items-center gap-3">
            <img
              src="/brand/asti-logo-256.png"
              alt=""
              width={56}
              height={56}
              className="h-14 w-14 shrink-0 rounded-lg bg-white/95 p-1"
            />
            <div>
              <p className="text-sm font-medium text-white">{t('site.institute')}</p>
              <p className="text-xs text-brand-200">
                {t('site.name')} · {t('site.official')}
              </p>
            </div>
          </div>

          <h1 className="text-3xl font-semibold sm:text-4xl">{t('home.heroTitle')}</h1>
          <p className="mt-4 text-base text-brand-100 sm:text-lg">{t('home.heroText')}</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/submit">
              <Button className="bg-white !text-brand-800 hover:bg-brand-50">
                {t('home.ctaSubmit')}
              </Button>
            </Link>
            <Link to="/track">
              <Button className="border border-white/40 bg-transparent text-white hover:bg-white/10">
                {t('home.ctaTrack')}
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Raqamlar */}
      {stats.data && (
        <section>
          <h2 className="mb-4 text-lg font-semibold text-slate-900">{t('home.statsTitle')}</h2>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatTile label={t('home.statTotal')} value={stats.data.total} />
            <StatTile label={t('home.statResolved')} value={stats.data.resolved} />
            <StatTile label={t('home.statOpen')} value={stats.data.open} />
            <StatTile label={t('home.statLast30')} value={stats.data.last30Days} />
          </div>
          <div className="mt-3 text-right">
            <Link to="/stats" className="text-sm font-medium text-brand-600 hover:underline">
              {t('stats.title')} →
            </Link>
          </div>
        </section>
      )}

      {/* Qanday ishlaydi */}
      <section>
        <h2 className="mb-4 text-lg font-semibold text-slate-900">{t('home.howTitle')}</h2>
        <div className="grid gap-4 md:grid-cols-3">
          {[1, 2, 3].map((step) => (
            <Card key={step}>
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-sm font-semibold text-brand-700">
                {step}
              </div>
              <h3 className="mt-3 font-medium text-slate-900">{t(`home.step${step}Title`)}</h3>
              <p className="mt-1.5 text-sm text-slate-600">{t(`home.step${step}Text`)}</p>
            </Card>
          ))}
        </div>
      </section>

      <div className="grid gap-8 lg:grid-cols-3">
        {/* Yangiliklar */}
        <section className="lg:col-span-2">
          <div className="mb-4 flex items-baseline justify-between">
            <h2 className="text-lg font-semibold text-slate-900">{t('home.newsTitle')}</h2>
            <Link to="/news" className="text-sm font-medium text-brand-600 hover:underline">
              {t('home.newsMore')} →
            </Link>
          </div>

          {news.data && news.data.content.length > 0 ? (
            <div className="space-y-4">
              {news.data.content.map((item) => (
                <Link
                  key={item.id}
                  to={`/news/${item.slug}`}
                  className="flex gap-4 rounded-xl border border-slate-200 bg-white p-4 transition-shadow hover:shadow-md"
                >
                  {item.coverImageUrl && (
                    <img
                      src={item.coverImageUrl}
                      alt=""
                      className="hidden h-20 w-28 shrink-0 rounded-lg object-cover sm:block"
                    />
                  )}
                  <div className="min-w-0">
                    <h3 className="font-medium text-slate-900">{item.title}</h3>
                    {item.summary && (
                      <p className="mt-1 line-clamp-2 text-sm text-slate-600">{item.summary}</p>
                    )}
                    <p className="mt-2 text-xs text-slate-400">{formatDate(item.publishedAt)}</p>
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

        {/* Yon panel: so'rovnoma va havolalar */}
        <div className="space-y-8">
          {featuredPoll && (
            <section>
              <h2 className="mb-4 text-lg font-semibold text-slate-900">{t('home.pollTitle')}</h2>
              <PollCard poll={featuredPoll} />
            </section>
          )}

          {links.data && links.data.length > 0 && (
            <section>
              <h2 className="mb-4 text-lg font-semibold text-slate-900">{t('home.linksTitle')}</h2>
              <Card className="!p-4">
                <ul className="space-y-2">
                  {links.data.slice(0, 6).map((link) => (
                    <li key={link.id}>
                      <a
                        href={link.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sm text-brand-600 hover:underline"
                      >
                        {link.title}
                      </a>
                    </li>
                  ))}
                </ul>
              </Card>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
