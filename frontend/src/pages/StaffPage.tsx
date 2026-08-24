import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import { Card, EmptyState, ErrorBox, PageHeader, Spinner } from '../components/ui';
import { errorMessage } from '../lib/errors';

/** Ism bosh harflari - surat bo'lmaganda o'rniga ko'rsatiladi. */
function initials(fullName: string): string {
  return fullName
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

export function StaffPage() {
  const { t } = useTranslation();
  const query = useQuery({ queryKey: ['staff'], queryFn: contentApi.staff });

  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  return (
    <div>
      <PageHeader title={t('staff.title')} description={t('staff.intro')} />

      {query.data.length === 0 ? (
        <EmptyState message={t('staff.empty')} />
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {query.data.map((member) => (
            <Card key={member.id}>
              <div className="flex items-start gap-4">
                {member.photoUrl ? (
                  <img
                    src={member.photoUrl}
                    alt=""
                    className="h-16 w-16 shrink-0 rounded-full object-cover"
                  />
                ) : (
                  <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-brand-100 text-lg font-semibold text-brand-700">
                    {initials(member.fullName)}
                  </div>
                )}
                <div className="min-w-0">
                  <h2 className="font-medium text-slate-900">{member.fullName}</h2>
                  <p className="mt-0.5 text-sm text-slate-600">{member.position}</p>
                  {member.academicDegree && (
                    <p className="mt-0.5 text-xs text-slate-500">{member.academicDegree}</p>
                  )}
                </div>
              </div>

              <dl className="mt-4 space-y-1.5 border-t border-slate-100 pt-4 text-sm">
                {member.receptionHours && (
                  <div className="flex gap-2">
                    <dt className="text-slate-500">{t('staff.reception')}:</dt>
                    <dd className="text-slate-700">{member.receptionHours}</dd>
                  </div>
                )}
                {member.phone && (
                  <div className="flex gap-2">
                    <dt className="text-slate-500">{t('staff.phone')}:</dt>
                    <dd>
                      <a href={`tel:${member.phone}`} className="text-brand-600 hover:underline">
                        {member.phone}
                      </a>
                    </dd>
                  </div>
                )}
                {member.email && (
                  <div className="flex gap-2">
                    <dt className="text-slate-500">{t('staff.email')}:</dt>
                    <dd className="min-w-0">
                      <a
                        href={`mailto:${member.email}`}
                        className="block truncate text-brand-600 hover:underline"
                      >
                        {member.email}
                      </a>
                    </dd>
                  </div>
                )}
              </dl>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
