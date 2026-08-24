import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import { Button, Card, EmptyState, ErrorBox, PageHeader, Spinner } from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatDate } from '../lib/format';

/** Admin panelidan boshqariladigan matnli sahifa ("Bo'lim haqida" va h.k.). */
export function StaticPageView() {
  const { t } = useTranslation();
  const { slug = '' } = useParams();

  const query = useQuery({
    queryKey: ['page', slug],
    queryFn: () => contentApi.pageBySlug(slug),
    retry: false,
  });

  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorBox message={errorMessage(query.error, t)} />;

  return (
    <article className="mx-auto max-w-3xl">
      <PageHeader title={query.data.title} />
      <Card>
        <div className="prose-content text-slate-700">{query.data.body}</div>
      </Card>
      <p className="mt-3 text-xs text-slate-400">
        {t('track.updated')}: {formatDate(query.data.updatedAt)}
      </p>
    </article>
  );
}

export function LinksPage() {
  const { t } = useTranslation();
  const query = useQuery({ queryKey: ['links'], queryFn: contentApi.links });

  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  // Havolalar guruhlar bo'yicha ajratiladi; guruhsizlari oxirida turadi.
  const grouped = new Map<string, typeof query.data>();
  for (const link of query.data) {
    const group = link.groupName ?? t('links.other');
    grouped.set(group, [...(grouped.get(group) ?? []), link]);
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={t('links.title')} />

      {query.data.length === 0 ? (
        <EmptyState message={t('links.empty')} />
      ) : (
        <div className="space-y-6">
          {[...grouped.entries()].map(([group, links]) => (
            <Card key={group}>
              <h2 className="mb-3 text-sm font-semibold tracking-wide text-slate-500 uppercase">
                {group}
              </h2>
              <ul className="space-y-3">
                {links.map((link) => (
                  <li key={link.id}>
                    <a
                      href={link.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="font-medium text-brand-600 hover:underline"
                    >
                      {link.title}
                    </a>
                    {link.description && (
                      <p className="mt-0.5 text-sm text-slate-600">{link.description}</p>
                    )}
                  </li>
                ))}
              </ul>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

export function NotFoundPage() {
  const { t } = useTranslation();

  return (
    <div className="mx-auto max-w-lg py-16 text-center">
      <p className="text-6xl font-semibold text-brand-200">404</p>
      <h1 className="mt-4 text-xl font-semibold text-slate-900">{t('common.notFound')}</h1>
      <p className="mt-2 text-sm text-slate-600">{t('common.notFoundHint')}</p>
      <Link to="/" className="mt-6 inline-block">
        <Button>{t('common.goHome')}</Button>
      </Link>
    </div>
  );
}
