import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { NewsImageResponse } from '../api/types';

/**
 * Albom: rasmlar to'ri va ularni to'liq ko'rish oynasi (lightbox).
 *
 * <p>Tashqi kutubxona qo'shilmadi - bu yerda kerak bo'lgan narsa
 * bir nechta rasm, klaviatura bilan boshqarish va fon. Tayyor
 * galereya kutubxonalari buning uchun juda katta.
 */
export function Gallery({ images }: { images: NewsImageResponse[] }) {
  const { t } = useTranslation();
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  const close = useCallback(() => setOpenIndex(null), []);

  const step = useCallback(
    (delta: number) => {
      setOpenIndex((current) => {
        if (current === null) return current;
        // Aylanma: oxirgidan keyin birinchisiga qaytadi.
        return (current + delta + images.length) % images.length;
      });
    },
    [images.length],
  );

  // Klaviatura bilan boshqarish faqat oyna ochiq bo'lganda ulanadi.
  useEffect(() => {
    if (openIndex === null) return;

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') close();
      if (event.key === 'ArrowRight') step(1);
      if (event.key === 'ArrowLeft') step(-1);
    }

    document.addEventListener('keydown', onKeyDown);
    // Oyna ochiqligida orqa fon aylanmasin.
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [openIndex, close, step]);

  if (images.length === 0) return null;

  const active = openIndex === null ? null : images[openIndex];

  return (
    <section>
      <h2 className="mb-3 text-lg font-semibold text-slate-900">
        {t('news.gallery')}{' '}
        <span className="text-sm font-normal text-slate-400">
          ({t('news.imageCount', { count: images.length })})
        </span>
      </h2>

      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {images.map((image, index) => (
          <li key={image.id}>
            <button
              type="button"
              onClick={() => setOpenIndex(index)}
              className="group block w-full overflow-hidden rounded-lg border border-slate-200 bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
            >
              <img
                src={image.url}
                alt={image.caption ?? ''}
                loading="lazy"
                className="aspect-4/3 w-full object-cover transition-transform duration-200 group-hover:scale-105"
              />
            </button>
            {image.caption && (
              <p className="mt-1 text-xs text-slate-500">{image.caption}</p>
            )}
          </li>
        ))}
      </ul>

      {active && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={t('news.gallery')}
          className="fixed inset-0 z-50 flex flex-col bg-black/90 p-4"
          onClick={close}
        >
          <div className="flex items-center justify-between text-white">
            <span className="text-sm">
              {t('gallery.counter', { current: (openIndex ?? 0) + 1, total: images.length })}
            </span>
            <button
              type="button"
              onClick={close}
              aria-label={t('gallery.close')}
              className="rounded p-2 text-2xl leading-none hover:bg-white/10"
            >
              ×
            </button>
          </div>

          {/* Rasm ustiga bosganda oyna yopilmasin */}
          <div
            className="flex min-h-0 flex-1 items-center justify-center gap-3"
            onClick={(event) => event.stopPropagation()}
          >
            {images.length > 1 && (
              <button
                type="button"
                onClick={() => step(-1)}
                aria-label={t('gallery.prev')}
                className="shrink-0 rounded-full bg-white/10 p-3 text-white hover:bg-white/20"
              >
                ‹
              </button>
            )}

            <img
              src={active.url}
              alt={active.caption ?? ''}
              className="max-h-full max-w-full object-contain"
            />

            {images.length > 1 && (
              <button
                type="button"
                onClick={() => step(1)}
                aria-label={t('gallery.next')}
                className="shrink-0 rounded-full bg-white/10 p-3 text-white hover:bg-white/20"
              >
                ›
              </button>
            )}
          </div>

          {active.caption && (
            <p className="mt-3 text-center text-sm text-white/80">{active.caption}</p>
          )}
        </div>
      )}
    </section>
  );
}
