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
            {/*
              Kartochkalar ikki ustunda: sarlavha uzun bo'lgani uchun uchta
              ustunda satrlar juda tor chiqadi. Har birida sarlavha, qisqa
              mazmun, manba-sana-ko'rishlar qatori va o'qish tugmasi bor.
            */}
            <div className="grid gap-6 lg:grid-cols-2">
              {query.data.content.map((item) => (
                <article
                  key={item.id}
                  className="flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition-shadow hover:shadow-md"
                >
                  {item.coverImageUrl && (
                    <Link to={`/news/${item.slug}`} className="relative block">
                      <img src={item.coverImageUrl} alt="" className="h-48 w-full object-cover" />
                      {item.imageCount > 0 && (
                        <span className="absolute right-3 bottom-3 rounded-full bg-black/60 px-2.5 py-1 text-xs text-white">
                          {t('news.imageCount', { count: item.imageCount })}
                        </span>
                      )}
                    </Link>
                  )}

                  <div className="flex flex-1 flex-col p-6">
                    <h2 className="text-lg leading-snug font-bold text-slate-900">
                      <Link to={`/news/${item.slug}`} className="hover:text-brand-700">
                        {item.title}
                      </Link>
                    </h2>

                    {item.summary && (
                      <p className="mt-3 line-clamp-2 text-sm text-slate-500 italic">
                        {item.summary}
                      </p>
                    )}

                    <div className="mt-4 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
                      <span className="font-semibold text-brand-600">{t('site.badge')}</span>
                      <span aria-hidden>•</span>
                      <span>{formatDate(item.publishedAt)}</span>
                      <span aria-hidden>•</span>
                      <span>{t('news.views', { count: item.viewCount })}</span>
                    </div>

                    <div className="mt-auto pt-5">
                      <Link
                        to={`/news/${item.slug}`}
                        className="inline-flex items-center gap-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-700"
                      >
                        {t('news.readMore')}
                        <span aria-hidden>→</span>
                      </Link>
                    </div>
                  </div>
                </article>
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

      {/*
        Bu maqolaning boshqa tildagi nusxalari. Yuqoridagi umumiy til
        tugmasi butun saytni almashtiradi va o'quvchini ro'yxatga qaytarardi;
        bu havolalar esa aynan shu maqolaning tarjimasiga olib boradi.
      */}
      {news.translations.length > 0 && (
        <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
          <span className="text-slate-400">{t('news.otherLanguages')}</span>
          {news.translations.map((translation) => (
            <Link
              key={translation.id}
              to={`/news/${translation.slug}`}
              className="rounded-full border border-slate-200 px-3 py-1 font-medium text-brand-600 hover:bg-slate-50"
            >
              {translation.languageName}
            </Link>
          ))}
        </div>
      )}

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
