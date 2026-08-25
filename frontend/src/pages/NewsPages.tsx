import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import { NewsContent } from '../components/NewsContent';
import { Card, EmptyState, ErrorBox, Input, PageHeader, Pagination, Spinner } from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatDate } from '../lib/format';

export function NewsListPage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [applied, setApplied] = useState('');
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['news', applied, page],
    queryFn: () => contentApi.news(applied || undefined, page, 9),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <PageHeader title={t('news.title')} />

      <form
        onSubmit={(event) => {
          event.preventDefault();
          setPage(0);
          setApplied(search.trim());
        }}
        className="mb-6 max-w-md"
      >
        <Input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder={t('news.searchPlaceholder')}
          type="search"
        />
      </form>

      {query.isPending && <Spinner />}
      {query.isError && <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />}

      {query.data &&
        (query.data.content.length === 0 ? (
          <EmptyState message={t('news.empty')} />
        ) : (
          <>
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {query.data.content.map((item) => (
                <Link
                  key={item.id}
                  to={`/news/${item.slug}`}
                  className="flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white transition-shadow hover:shadow-md"
                >
                  <div className="relative">
                    {item.coverImageUrl ? (
                      <img src={item.coverImageUrl} alt="" className="h-44 w-full object-cover" />
                    ) : (
                      <div className="h-44 w-full bg-gradient-to-br from-brand-100 to-brand-50" />
                    )}
                    {/* Albom borligi ro'yxatdayoq ko'rinib tursin. */}
                    {item.imageCount > 0 && (
                      <span className="absolute right-2 bottom-2 rounded-full bg-black/60 px-2 py-0.5 text-xs text-white">
                        {t('news.imageCount', { count: item.imageCount })}
                      </span>
                    )}
                  </div>
                  <div className="flex flex-1 flex-col p-5">
                    <h2 className="font-medium text-slate-900">{item.title}</h2>
                    {item.summary && (
                      <p className="mt-2 line-clamp-3 flex-1 text-sm text-slate-600">
                        {item.summary}
                      </p>
                    )}
                    <div className="mt-4 flex items-center justify-between text-xs text-slate-400">
                      <span>{formatDate(item.publishedAt)}</span>
                      <span>{t('news.views', { count: item.viewCount })}</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>

            <Pagination page={page} totalPages={query.data.totalPages} onChange={setPage} />
          </>
        ))}
    </div>
  );
}

export function NewsDetailPage() {
  const { t } = useTranslation();
  const { slug = '' } = useParams();

  const query = useQuery({
    queryKey: ['news', 'detail', slug],
    queryFn: () => contentApi.newsBySlug(slug),
    retry: false,
  });

  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorBox message={errorMessage(query.error, t)} />;

  const news = query.data;

  return (
    <article className="mx-auto max-w-3xl">
      <Link to="/news" className="text-sm font-medium text-brand-600 hover:underline">
        ← {t('news.backToList')}
      </Link>

      <h1 className="mt-4 text-2xl font-semibold text-slate-900 sm:text-3xl">{news.title}</h1>
      <div className="mt-2 flex flex-wrap gap-4 text-xs text-slate-400">
        <span>{formatDate(news.publishedAt)}</span>
        <span>{t('news.views', { count: news.viewCount })}</span>
      </div>

      {news.coverImageUrl && (
        <img
          src={news.coverImageUrl}
          alt=""
          className="mt-6 w-full rounded-xl object-cover"
        />
      )}

      {news.summary && (
        <p className="mt-6 border-l-4 border-brand-200 pl-4 text-base text-slate-700">
          {news.summary}
        </p>
      )}

      <Card className="mt-6">
        <NewsContent blocks={news.blocks} />
      </Card>
    </article>
  );
}
