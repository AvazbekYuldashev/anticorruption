import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import { EmptyState, ErrorBox, Spinner } from '../components/ui';
import { errorMessage } from '../lib/errors';
import { renderRichText } from '../lib/richText';

/**
 * "Bo'lim haqida" sahifasi.
 *
 * <p>Mazmun admin panelidan tayyor tuzilma bo'yicha kiritiladi: sarlavha,
 * kirish matni, vazifalar ro'yxati va maqsad. Sahifa shu tuzilmani rasmiy
 * hujjat ko'rinishida chiqaradi - o'qishga qulay kenglik, tekislangan matn
 * va belgilangan ro'yxat.
 */
export function AboutPage() {
  const { t } = useTranslation();
  const query = useQuery({ queryKey: ['about'], queryFn: contentApi.about });

  if (query.isPending) return <Spinner />;
  if (query.isError)
    return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  const about = query.data;

  if (!about.filled) {
    return (
      <div className="mx-auto max-w-4xl">
        <EmptyState message={t('about.empty')} />
      </div>
    );
  }

  return (
    <article className="mx-auto max-w-4xl">
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-10">
        {about.title && (
          <h1 className="text-center text-xl font-semibold text-brand-600 sm:text-2xl">
            {about.title}
          </h1>
        )}

        {about.body && (
          <p className="mt-8 text-justify text-[15px] leading-7 whitespace-pre-line text-slate-700">
            {renderRichText(about.body)}
          </p>
        )}

        {about.tasks.length > 0 && (
          <section className="mt-8">
            {about.tasksTitle && (
              <h2 className="font-semibold text-brand-600">{about.tasksTitle}</h2>
            )}

            <ul className="mt-3 space-y-2.5">
              {about.tasks.map((task, index) => (
                <li key={index} className="flex gap-3 text-[15px] leading-6 text-slate-700">
                  {/* Belgi matnning birinchi qatoriga tekislanadi. */}
                  <span
                    aria-hidden
                    className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-xs font-semibold text-emerald-600"
                  >
                    ✓
                  </span>
                  <span>{task}</span>
                </li>
              ))}
            </ul>
          </section>
        )}

        {about.goal && (
          <p className="mt-8 border-l-4 border-brand-200 bg-brand-50/60 py-3 pl-4 text-[15px] leading-7 whitespace-pre-line text-slate-700">
            {renderRichText(about.goal)}
          </p>
        )}
      </div>
    </article>
  );
}
