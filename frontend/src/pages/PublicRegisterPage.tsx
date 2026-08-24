import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { complaintsApi } from '../api/complaints';
import { referenceApi } from '../api/reference';
import {
  Button,
  Card,
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

export function PublicRegisterPage() {
  const { t } = useTranslation();
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [applied, setApplied] = useState<Filters>(EMPTY_FILTERS);
  const [page, setPage] = useState(0);

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

      <Card className="mb-6">
        <form
          onSubmit={(event) => {
            event.preventDefault();
            applyFilters();
          }}
        >
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label={t('register.filterCode')}>
              <Input
                value={filters.code}
                onChange={(event) => setFilters({ ...filters, code: event.target.value })}
                placeholder="AC-2026-"
                className="font-mono uppercase"
              />
            </Field>

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
        </form>
      </Card>

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
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-slate-200 bg-slate-50 text-xs tracking-wide text-slate-500 uppercase">
                  <tr>
                    <th className="px-4 py-3 font-medium">{t('register.colCode')}</th>
                    <th className="px-4 py-3 font-medium">{t('register.colCategory')}</th>
                    <th className="px-4 py-3 font-medium">{t('register.colFaculty')}</th>
                    <th className="px-4 py-3 font-medium">{t('register.colReporter')}</th>
                    <th className="px-4 py-3 font-medium">{t('register.colStatus')}</th>
                    <th className="px-4 py-3 font-medium">{t('register.colDate')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {query.data.content.map((entry) => (
                    <tr key={entry.trackingCode} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-mono text-xs font-medium text-slate-900">
                        {entry.trackingCode}
                      </td>
                      <td className="px-4 py-3 text-slate-700">{entry.categoryLabel}</td>
                      <td className="px-4 py-3 text-slate-600">{entry.facultyName}</td>
                      <td className="px-4 py-3 text-slate-600">{entry.reporterTypeLabel ?? '—'}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={entry.status} label={entry.statusLabel} />
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-slate-500">
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
