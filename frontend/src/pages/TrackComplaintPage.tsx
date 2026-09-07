import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { complaintsApi } from '../api/complaints';
import { Button, Card, ErrorBox, Field, Input, PageHeader, Spinner, StatusBadge } from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatDateTime } from '../lib/format';

/*
  Yorliq va qiymat ataylab bir-biriga o'xshamaydi: yorliq kichik, katta harfli
  va oqargan, qiymat esa kattaroq va to'q. Ikkalasi bir xil o'lchamda bo'lsa
  ko'z ro'yxatni bir tekis matn deb o'qiydi va kerakli qatorni izlab qoladi.
*/
function Row({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <div className="flex flex-col gap-0.5 border-b border-slate-100 py-2.5 last:border-b-0 sm:flex-row sm:items-baseline sm:gap-4">
      <dt className="w-48 shrink-0 text-[11px] font-bold tracking-wider text-slate-400 uppercase">
        {label}
      </dt>
      <dd className="text-sm text-slate-800">{value}</dd>
    </div>
  );
}

export function TrackComplaintPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();

  // Kod URL da bo'lsa sahifa ochilishi bilan qidiriladi - murojaat
  // yuborilgandan keyingi havola aynan shunday ishlaydi.
  const submittedCode = searchParams.get('code') ?? '';
  const [input, setInput] = useState(submittedCode);

  const query = useQuery({
    queryKey: ['complaint', 'track', submittedCode],
    queryFn: () => complaintsApi.track(submittedCode),
    enabled: submittedCode.trim().length > 0,
    retry: false,
  });

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={t('track.title')} description={t('track.intro')} />

      <Card>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            const code = input.trim().toUpperCase();
            if (code) setSearchParams({ code });
          }}
          className="flex flex-col gap-4 sm:flex-row sm:items-end"
        >
          <div className="flex-1">
            <Field label={t('track.fieldCode')} required>
              <Input
                value={input}
                onChange={(event) => setInput(event.target.value)}
                placeholder="AC-2026-XXXXXX"
                className="font-mono tracking-wider uppercase"
                autoComplete="off"
              />
            </Field>
          </div>
          <Button type="submit" disabled={query.isFetching || input.trim() === ''}>
            {query.isFetching ? t('track.searching') : t('track.button')}
          </Button>
        </form>
      </Card>

      {query.isFetching && <Spinner label={t('track.searching')} />}

      {query.isError && !query.isFetching && (
        <div className="mt-6">
          <ErrorBox message={errorMessage(query.error, t)} />
        </div>
      )}

      {query.data && !query.isFetching && (
        <div className="mt-6 space-y-6">
          <Card>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-mono text-sm text-slate-500">{query.data.trackingCode}</p>
                <h2 className="mt-1 text-lg font-semibold text-slate-900">{query.data.title}</h2>
              </div>
              <StatusBadge status={query.data.status} label={query.data.statusLabel} />
            </div>

            <dl className="mt-4">
              <Row label={t('track.category')} value={query.data.categoryLabel} />
              <Row label={t('track.faculty')} value={query.data.facultyName} />
              <Row label={t('track.department')} value={query.data.departmentName} />
              <Row label={t('track.subject')} value={query.data.subjectName} />
              <Row label={t('track.submitted')} value={formatDateTime(query.data.createdAt)} />
              <Row label={t('track.updated')} value={formatDateTime(query.data.updatedAt)} />
              <Row label={t('track.closed')} value={query.data.closedAt ? formatDateTime(query.data.closedAt) : null} />
              <Row
                label={t('track.attachments')}
                value={
                  query.data.attachmentCount > 0
                    ? t('track.attachmentCount', { count: query.data.attachmentCount })
                    : null
                }
              />
            </dl>
          </Card>

          <Card>
            <h3 className="font-medium text-slate-900">{t('track.official')}</h3>
            {query.data.officialResponse ? (
              <p className="prose-content mt-2 text-sm text-slate-700">
                {query.data.officialResponse}
              </p>
            ) : (
              <p className="mt-2 text-sm text-slate-500">{t('track.noOfficial')}</p>
            )}
          </Card>

          <Card>
            <h3 className="mb-5 font-medium text-slate-900">{t('track.history')}</h3>
            {/*
              Holatlar tarixi vaqt chizig'i ko'rinishida: nuqtalarni
              bog'lovchi chiziq murojaat qanday yo'l bosganini ko'rsatadi.
            */}
            <ol className="relative space-y-6 border-l border-slate-200 pl-6">
              {query.data.history.map((entry) => (
                <li key={entry.id} className="relative">
                  <span
                    aria-hidden
                    className="absolute top-1.5 -left-[27px] h-3 w-3 rounded-full border-2 border-white bg-brand-500 ring-1 ring-brand-200"
                  />
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={entry.newStatus} label={entry.newStatusLabel} />
                    <span className="text-xs text-slate-400">
                      {formatDateTime(entry.changedAt)}
                    </span>
                  </div>
                  {entry.note && <p className="mt-1.5 text-sm text-slate-600">{entry.note}</p>}
                </li>
              ))}
            </ol>
          </Card>
        </div>
      )}
    </div>
  );
}
