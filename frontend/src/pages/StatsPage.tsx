import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { statsApi } from '../api/stats';
import type { StatItem } from '../api/types';
import { Card, EmptyState, ErrorBox, PageHeader, Spinner } from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatNumber } from '../lib/format';

/**
 * Oddiy gorizontal diagramma.
 *
 * <p>Diagramma kutubxonasi qo'shilmadi: bu yerda kerak bo'lgan narsa -
 * nisbatni ko'rsatuvchi bir nechta chiziq, uning uchun bir necha o'nlab
 * kilobayt qo'shimcha kod olib kelish o'zini oqlamaydi.
 *
 * <p>Rang faqat ma'no tashiganda beriladi ({@code colors} orqali - masalan
 * murojaat holatlariga). Kategoriya, fakultet, murojaatchi turi kabi oddiy
 * nomlar bir xil ohangda qoladi: ularni kattaligiga qarab bo'yash chiziq
 * uzunligi allaqachon ko'rsatib turgan narsani rang bilan takrorlash bo'lardi,
 * ranglarni esa har biriga alohida berish "qaysi rang nimani anglatadi?"
 * degan ortiqcha savol tug'diradi.
 */
/**
 * Murojaat holatining rangi. Holat ma'no tashiydi - "ijobiy hal qilindi" yaxshi,
 * "rad etildi" tanqidiy - shuning uchun u qat'iy holat shkalasini kiyadi.
 *
 * <p>"Yangi" hech qanday baho emas, u shunchaki boshlang'ich bosqich, shuning
 * uchun saytning asosiy ko'k rangida qoladi.
 */
const STATUS_BAR_COLORS: Record<string, string> = {
  NEW: 'bg-brand-500',
  IN_REVIEW: 'bg-status-warning',
  NEED_INFO: 'bg-status-serious',
  RESOLVED: 'bg-status-good',
  REJECTED: 'bg-status-critical',
};

function BarList({ items, colors }: { items: StatItem[]; colors?: Record<string, string> }) {
  const visible = items.filter((item) => item.count > 0);
  const max = Math.max(...visible.map((item) => item.count), 1);

  if (visible.length === 0) {
    return <p className="text-sm text-slate-400">—</p>;
  }

  return (
    <ul className="space-y-3">
      {visible.map((item) => (
        <li key={item.key}>
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span className="text-slate-700">{item.label}</span>
            <span className="shrink-0 font-medium text-slate-900">{formatNumber(item.count)}</span>
          </div>
          <div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-100">
            <div
              className={`h-full rounded-full ${colors?.[item.key] ?? 'bg-brand-500'}`}
              style={{ width: `${(item.count / max) * 100}%` }}
            />
          </div>
        </li>
      ))}
    </ul>
  );
}

/**
 * Umumiy ko'rsatkich kartochkasi.
 *
 * <p>{@code tone="good"} - hal qilinganlar soni: bu yagona ko'rsatkich bo'lib,
 * o'zi baho tashiydi, va u "Holat bo'yicha" diagrammasidagi yashil chiziq bilan
 * bir xil rangda - o'quvchi ikkalasi bitta narsa ekanini rangdan biladi.
 */
function Tile({
  label,
  value,
  tone = 'neutral',
}: {
  label: string;
  value: string;
  tone?: 'neutral' | 'good';
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <div
        className={`text-2xl font-semibold ${tone === 'good' ? 'text-status-good' : 'text-brand-700'}`}
      >
        {value}
      </div>
      <div className="mt-1 text-xs text-slate-500">{label}</div>
    </div>
  );
}

export function StatsPage() {
  const { t } = useTranslation();

  const stats = useQuery({ queryKey: ['stats', 'public'], queryFn: statsApi.publicStats });
  const rating = useQuery({ queryKey: ['stats', 'rating'], queryFn: statsApi.facultyRating });

  if (stats.isPending) return <Spinner />;
  if (stats.isError) return <ErrorBox message={errorMessage(stats.error, t)} onRetry={() => stats.refetch()} />;

  const data = stats.data;

  return (
    <div className="space-y-10">
      <PageHeader title={t('stats.title')} description={t('stats.intro')} />

      <section>
        <h2 className="mb-4 text-lg font-semibold text-slate-900">{t('stats.overview')}</h2>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
          <Tile label={t('home.statTotal')} value={formatNumber(data.total)} />
          <Tile label={t('home.statResolved')} value={formatNumber(data.resolved)} tone="good" />
          <Tile label={t('home.statOpen')} value={formatNumber(data.open)} />
          <Tile label={t('home.statLast30')} value={formatNumber(data.last30Days)} />
          <Tile
            label={t('stats.avgDays')}
            value={
              data.averageResolutionDays === null
                ? t('stats.noAvg')
                : t('stats.days', { count: data.averageResolutionDays })
            }
          />
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <Card>
          <h2 className="mb-4 font-medium text-slate-900">{t('stats.byStatus')}</h2>
          <BarList items={data.byStatus} colors={STATUS_BAR_COLORS} />
        </Card>
        <Card>
          <h2 className="mb-4 font-medium text-slate-900">{t('stats.byReporter')}</h2>
          <BarList items={data.byReporterType} />
        </Card>
        <Card>
          <h2 className="mb-4 font-medium text-slate-900">{t('stats.byCategory')}</h2>
          <BarList items={data.byCategory} />
        </Card>
        <Card>
          <h2 className="mb-4 font-medium text-slate-900">{t('stats.byFaculty')}</h2>
          <BarList items={data.byFaculty} />
        </Card>
      </section>

      <section>
        <h2 className="mb-2 text-lg font-semibold text-slate-900">{t('stats.rating')}</h2>
        <p className="mb-4 max-w-3xl rounded-lg bg-amber-50 p-4 text-sm text-amber-900">
          {t('stats.ratingNote')}
        </p>

        {rating.data && rating.data.length === 0 && <EmptyState message={t('stats.ratingEmpty')} />}

        {rating.data && rating.data.length > 0 && (
          <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs tracking-wide text-slate-500 uppercase">
                <tr>
                  <th className="px-4 py-3 font-medium">{t('stats.colFaculty')}</th>
                  <th className="px-4 py-3 font-medium">{t('stats.colTotal')}</th>
                  <th className="px-4 py-3 font-medium">{t('stats.colResolved')}</th>
                  <th className="px-4 py-3 font-medium">{t('stats.colOpen')}</th>
                  <th className="px-4 py-3 font-medium">{t('stats.colRate')}</th>
                  <th className="px-4 py-3 font-medium">{t('stats.colAvgDays')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rating.data.map((row) => (
                  <tr key={row.facultyId} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-900">{row.facultyName}</td>
                    <td className="px-4 py-3 text-slate-700">{formatNumber(row.total)}</td>
                    <td className="px-4 py-3 text-slate-700">{formatNumber(row.resolved)}</td>
                    <td className="px-4 py-3 text-slate-700">{formatNumber(row.open)}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-slate-100">
                          {/* Hal qilinganlik ulushi - yuqoridagi yashil bilan bir xil ma'no. */}
                          <div
                            className="h-full rounded-full bg-status-good"
                            style={{ width: `${row.resolutionRate}%` }}
                          />
                        </div>
                        <span className="text-xs text-slate-600">{row.resolutionRate}%</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      {row.averageResolutionDays === null
                        ? '—'
                        : t('stats.days', { count: row.averageResolutionDays })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
