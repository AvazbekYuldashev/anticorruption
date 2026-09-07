import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { complaintsApi } from '../api/complaints';
import { referenceApi } from '../api/reference';
import { statsApi } from '../api/stats';
import {
  Button,
  EmptyState,
  ErrorBox,
  Field,
  Input,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  StatusBadge,
} from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatDate } from '../lib/format';

interface Filters {
  code: string;
  category: string;
  status: string;
  facultyId: string;
}

const EMPTY_FILTERS: Filters = { code: '', category: '', status: '', facultyId: '' };

/**
 * Yuqoridagi hisob kartochkasi.
 *
 * <p>Rang chap chetdagi chiziq va yumshoq fon orqali beriladi: raqam
 * ajralib tursin, lekin sahifa rang-barang bo'lib ketmasin.
 */
function StatTile({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: 'blue' | 'green' | 'amber';
}) {
  const tones = {
    blue: 'border-l-brand-500 bg-brand-50/70 text-brand-700',
    green: 'border-l-emerald-500 bg-emerald-50/70 text-emerald-700',
    amber: 'border-l-amber-500 bg-amber-50/70 text-amber-700',
  };

  return (
    <div className={`rounded-xl border border-slate-200 border-l-4 p-4 ${tones[tone]}`}>
      <p className="text-sm text-slate-600">{label}</p>
      <p className="mt-1 text-2xl font-bold">{value}</p>
    </div>
  );
}

/**
 * Murojaatlar reyestri.
 *
 * <p>Yuqorida umumiy manzara - oxirgi oy, hal qilinganlar va jarayondagilar.
 * Keyin kod bo'yicha tezkor qidiruv, batafsil filtrlar esa kerak bo'lgandagina
 * ochiladi: kundalik ish ko'pincha bitta kodni topishdan iborat.
 */
export function PublicRegisterPage() {
  const { t } = useTranslation();
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [applied, setApplied] = useState<Filters>(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);

  const stats = useQuery({ queryKey: ['stats', 'public'], queryFn: statsApi.publicStats });
  const reference = useQuery({ queryKey: ['reference'], queryFn: referenceApi.all });

  const query = useQuery({
    queryKey: ['register', applied, page],
    queryFn: () =>
      complaintsApi.register({
        code: applied.code || undefined,
        category: applied.category || undefined,
        status: applied.status || undefined,
        facultyId: applied.facultyId ? Number(applied.facultyId) : null,
        page,
      }),
    // Sahifa almashganda jadval bo'shab qolmasin - eski natija turaveradi.
    placeholderData: keepPreviousData,
  });

  function applyFilters() {
    setPage(0);
    setApplied(filters);
  }

  function resetFilters() {
    setPage(0);
    setFilters(EMPTY_FILTERS);
    setApplied(EMPTY_FILTERS);
  }

  return (
    <div>
      <PageHeader title={t('register.title')} description={t('register.intro')} />

      {stats.data && (
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <StatTile label={t('home.statLast30')} value={stats.data.last30Days} tone="blue" />
          <StatTile label={t('home.statResolved')} value={stats.data.resolved} tone="green" />
          <StatTile label={t('home.statOpen')} value={stats.data.open} tone="amber" />
        </div>
      )}

      <form
        onSubmit={(event) => {
          event.preventDefault();
          applyFilters();
        }}
        className="mb-6"
      >
        <div className="flex flex-wrap items-center gap-3">
          <Input
            value={filters.code}
            onChange={(event) => setFilters({ ...filters, code: event.target.value })}
            placeholder={t('register.searchPlaceholder')}
            className="max-w-sm flex-1 font-mono uppercase"
          />
          <Button type="submit">{t('common.search')}</Button>
          <button
            type="button"
            onClick={() => setShowFilters(!showFilters)}
            className="text-sm font-medium text-brand-600 hover:underline"
          >
            {t('register.moreFilters')} {showFilters ? '↑' : '↓'}
          </button>
        </div>

        {showFilters && (
          <div className="mt-4 rounded-xl border border-slate-200 bg-white p-4">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Field label={t('register.filterCategory')}>
                <Select
                  value={filters.category}
                  onChange={(event) => setFilters({ ...filters, category: event.target.value })}
                  placeholder={t('common.all')}
                  options={reference.data?.categories ?? []}
                />
              </Field>

              <Field label={t('register.filterStatus')}>
                <Select
                  value={filters.status}
                  onChange={(event) => setFilters({ ...filters, status: event.target.value })}
                  placeholder={t('common.all')}
                  options={reference.data?.statuses ?? []}
                />
              </Field>

              <Field label={t('register.filterFaculty')}>
                <Select
                  value={filters.facultyId}
                  onChange={(event) => setFilters({ ...filters, facultyId: event.target.value })}
                  placeholder={t('common.all')}
                  options={(reference.data?.faculties ?? []).map((faculty) => ({
                    value: String(faculty.id),
                    label: faculty.name,
                  }))}
                />
              </Field>
            </div>

            <div className="mt-4 flex gap-3">
              <Button type="submit">{t('common.filter')}</Button>
              <Button type="button" variant="outline" onClick={resetFilters}>
                {t('common.reset')}
              </Button>
            </div>
          </div>
        )}
      </form>

      {query.isPending && <Spinner />}

      {query.isError && <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />}

      {query.data && (
        <>
          <p className="mb-3 text-sm text-slate-500">
            {t('register.found', { count: query.data.totalElements })}
          </p>

          {query.data.content.length === 0 ? (
            <EmptyState message={t('register.empty')} />
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-slate-200 bg-slate-50 text-xs tracking-wide text-slate-500 uppercase">
                  <tr>
                    <th className="px-5 py-3.5 font-semibold">{t('register.colCode')}</th>
                    <th className="px-5 py-3.5 font-semibold">{t('register.colStatus')}</th>
                    <th className="hidden px-5 py-3.5 font-semibold md:table-cell">
                      {t('register.colCategory')}
                    </th>
                    <th className="hidden px-5 py-3.5 font-semibold lg:table-cell">
                      {t('register.colFaculty')}
                    </th>
                    <th className="px-5 py-3.5 font-semibold">{t('register.colDate')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {query.data.content.map((entry) => (
                    <tr key={entry.trackingCode} className="transition-colors hover:bg-slate-50">
                      <td className="px-5 py-4 font-mono text-sm font-semibold text-slate-900">
                        {entry.trackingCode}
                      </td>
                      <td className="px-5 py-4">
                        <StatusBadge status={entry.status} label={entry.statusLabel} />
                      </td>
                      <td className="hidden px-5 py-4 text-slate-700 md:table-cell">
                        {entry.categoryLabel}
                      </td>
                      <td className="hidden px-5 py-4 text-slate-600 lg:table-cell">
                        {entry.facultyName}
                      </td>
                      <td className="px-5 py-4 whitespace-nowrap text-slate-500">
                        {formatDate(entry.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <Pagination page={page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
