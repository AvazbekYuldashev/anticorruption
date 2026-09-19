import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { complaintsApi } from '../api/complaints';
import {
  Button,
  Card,
  EmptyState,
  ErrorBox,
  PageHeader,
  Pagination,
  Spinner,
  StatusBadge,
} from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatDate, formatDateTime } from '../lib/format';

/**
 * Fuqaro o'zi yuborgan murojaatlar.
 *
 * <p>Anonim yuborilgan murojaatlar bu yerga tushmaydi: ular hech kimga
 * bog'lanmaydi va faqat kuzatuv kodi bilan ochiladi.
 */
export function CabinetComplaintsPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['complaints', 'mine', page],
    queryFn: () => complaintsApi.mine(page),
    placeholderData: keepPreviousData,
  });

  if (query.isPending) return <Spinner />;
  if (query.isError)
    return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  return (
    <div>
      <PageHeader title={t('my.title')} />

      {query.data.content.length === 0 ? (
        <EmptyState message={t('my.empty')}>
          <p className="mx-auto mb-4 max-w-md text-xs text-slate-400 dark:text-slate-500">{t('my.emptyHint')}</p>
          <Link to="/submit">
            <Button>{t('my.submitFirst')}</Button>
          </Link>
        </EmptyState>
      ) : (
        <>
          <div className="space-y-4">
            {query.data.content.map((complaint) => (
              <Card
                key={complaint.id}
                className="transition-all duration-150 hover:-translate-y-px hover:border-brand-200 dark:hover:border-brand-400/30 hover:shadow-md"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-mono text-xs text-slate-500 dark:text-slate-400">{complaint.trackingCode}</p>
                    <h2 className="mt-1 font-medium text-slate-900 dark:text-slate-100">{complaint.title}</h2>
                    <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{complaint.categoryLabel}</p>
                  </div>
                  <StatusBadge status={complaint.status} label={complaint.statusLabel} />
                </div>

                <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 dark:border-slate-800 pt-3">
                  <span className="text-xs text-slate-400 dark:text-slate-500">{formatDate(complaint.createdAt)}</span>
                  <Link
                    to={`/my/complaints/${complaint.id}`}
                    className="text-sm font-medium text-brand-600 dark:text-brand-300 transition-colors hover:text-brand-700 dark:hover:text-brand-300 hover:underline"
                  >
                    {t('my.viewDetail')} →
                  </Link>
                </div>
              </Card>
            ))}
          </div>

          <Pagination page={page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <div className="flex flex-col gap-0.5 border-b border-slate-100 dark:border-slate-800 py-2.5 sm:flex-row sm:gap-4">
      <dt className="w-48 shrink-0 text-sm text-slate-500 dark:text-slate-400">{label}</dt>
      <dd className="text-sm text-slate-800 dark:text-slate-200">{value}</dd>
    </div>
  );
}

export function CabinetComplaintDetailPage() {
  const { t } = useTranslation();
  const { id = '' } = useParams();

  const query = useQuery({
    queryKey: ['complaints', 'mine', 'detail', id],
    queryFn: () => complaintsApi.mineDetail(Number(id)),
    retry: false,
  });

  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorBox message={errorMessage(query.error, t)} />;

  const complaint = query.data;

  return (
    <div>
      <Link
        to="/my/complaints"
        className="text-sm font-medium text-brand-600 dark:text-brand-300 transition-colors hover:text-brand-700 dark:hover:text-brand-300 hover:underline"
      >
        ← {t('my.title')}
      </Link>

      <div className="mt-4 space-y-6">
        <Card>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="font-mono text-sm text-slate-500 dark:text-slate-400">{complaint.trackingCode}</p>
              <h1 className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">{complaint.title}</h1>
            </div>
            <StatusBadge status={complaint.status} label={complaint.statusLabel} />
          </div>

          <p className="prose-content mt-4 text-sm text-slate-700 dark:text-slate-300">{complaint.description}</p>

          <dl className="mt-4">
            <Row label={t('track.category')} value={complaint.categoryLabel} />
            <Row label={t('track.faculty')} value={complaint.facultyName} />
            <Row label={t('track.department')} value={complaint.departmentName} />
            <Row label={t('track.subject')} value={complaint.subjectName} />
            <Row label={t('submit.fieldPosition')} value={complaint.accusedPositionLabel} />
            <Row label={t('submit.fieldIncidentPlace')} value={complaint.incidentPlace} />
            <Row label={t('track.submitted')} value={formatDateTime(complaint.createdAt)} />
            <Row
              label={t('track.closed')}
              value={complaint.closedAt ? formatDateTime(complaint.closedAt) : null}
            />
          </dl>
        </Card>

        <Card>
          <h2 className="font-medium text-slate-900 dark:text-slate-100">{t('track.official')}</h2>
          {complaint.officialResponse ? (
            <p className="prose-content mt-2 text-sm text-slate-700 dark:text-slate-300">{complaint.officialResponse}</p>
          ) : (
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{t('track.noOfficial')}</p>
          )}
        </Card>

        {complaint.attachments.length > 0 && (
          <Card>
            <h2 className="mb-3 font-medium text-slate-900 dark:text-slate-100">{t('track.attachments')}</h2>
            <ul className="space-y-2">
              {complaint.attachments.map((attachment) => (
                <li key={attachment.id}>
                  <a
                    href={attachment.downloadUrl}
                    className="text-sm text-brand-600 dark:text-brand-300 hover:underline"
                  >
                    {attachment.originalName}
                  </a>
                </li>
              ))}
            </ul>
          </Card>
        )}

        <Card>
          <h2 className="mb-4 font-medium text-slate-900 dark:text-slate-100">{t('track.history')}</h2>
          <ol className="space-y-4">
            {complaint.history.map((entry) => (
              <li key={entry.id} className="flex gap-3">
                <span aria-hidden className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand-500" />
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={entry.newStatus} label={entry.newStatusLabel} />
                    <span className="text-xs text-slate-400 dark:text-slate-500">{formatDateTime(entry.changedAt)}</span>
                  </div>
                  {entry.note && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{entry.note}</p>}
                </div>
              </li>
            ))}
          </ol>
        </Card>
      </div>
    </div>
  );
}
