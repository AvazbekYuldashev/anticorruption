import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { NewsBlockResponse } from '../api/types';
import { renderRichText } from '../lib/richText';

/** Lightbox bo'ylab yuriladigan rasm. */
interface Shot {
  key: string;
  url: string;
  caption: string | null;
}

/**
 * Bloklardagi barcha rasmlarni chiqish tartibida bitta ro'yxatga yig'adi:
 * yakka rasm ham, albom ichidagilar ham. Shu tufayli o'quvchi lightbox
 * ichida butun yangilik bo'ylab bemalol yura oladi.
 */
function collectShots(blocks: NewsBlockResponse[]): Shot[] {
  const shots: Shot[] = [];

  for (const block of blocks) {
    if (block.type === 'IMAGE' && block.url) {
      shots.push({ key: `b${block.id}`, url: block.url, caption: block.caption });
    } else if (block.type === 'GALLERY') {
      for (const image of block.images) {
        shots.push({ key: `i${image.id}`, url: image.url, caption: image.caption });
      }
    }
  }

  return shots;
}

/**
 * Yangilik mazmunini bloklar tartibida chiqaradi: sarlavha, matn xatboshi,
 * yakka rasm va albom. Har qanday rasm bosilsa to'liq ekranda ochiladi.
 *
 * <p>Lightbox uchun tashqi kutubxona olinmadi: kerak bo'lgan narsa bir
 * nechta rasm, klaviatura bilan boshqarish va fon - tayyor galereya
 * kutubxonalari buning uchun juda katta.
 */
export function NewsContent({ blocks }: { blocks: NewsBlockResponse[] }) {
  const { t } = useTranslation();

  const shots = useMemo(() => collectShots(blocks), [blocks]);
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  const close = useCallback(() => setOpenIndex(null), []);

  const step = useCallback(
    (delta: number) => {
      setOpenIndex((current) => {
        if (current === null) return current;
        // Aylanma: oxirgidan keyin birinchisiga qaytadi.
        return (current + delta + shots.length) % shots.length;
      });
    },
    [shots.length],
  );

  useEffect(() => {
    if (openIndex === null) return;

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') close();
      if (event.key === 'ArrowRight') step(1);
      if (event.key === 'ArrowLeft') step(-1);
    }

    document.addEventListener('keydown', onKeyDown);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [openIndex, close, step]);

  if (blocks.length === 0) return null;

  const active = openIndex === null ? null : shots[openIndex];
  const indexOf = (key: string) => shots.findIndex((shot) => shot.key === key);

  return (
    <div className="space-y-5">
      {blocks.map((block) => {
        if (block.type === 'HEADING') {
          return (
            <h2
              key={block.id}
              className="prose-content pt-2 text-lg font-semibold text-slate-900 sm:text-xl"
            >
              {renderRichText(block.text)}
            </h2>
          );
        }

        if (block.type === 'TEXT') {
          return (
            <p key={block.id} className="prose-content text-slate-700">
              {renderRichText(block.text)}
            </p>
          );
        }

        if (block.type === 'GALLERY') {
          return (
            <figure key={block.id}>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {block.images.map((image) => (
                  <button
                    key={image.id}
                    type="button"
                    onClick={() => setOpenIndex(indexOf(`i${image.id}`))}
                    className="overflow-hidden rounded-lg border border-slate-200 bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
                  >
                    <img
                      src={image.url}
                      alt={image.caption ?? ''}
                      loading="lazy"
                      className="aspect-square w-full object-cover transition-transform hover:scale-105"
                    />
                  </button>
                ))}
              </div>
              {block.caption && (
                <figcaption className="mt-1.5 text-center text-xs text-slate-500">
                  {block.caption}
                </figcaption>
              )}
            </figure>
          );
        }

        return (
          <figure key={block.id}>
            <button
              type="button"
              onClick={() => setOpenIndex(indexOf(`b${block.id}`))}
              className="block w-full overflow-hidden rounded-xl border border-slate-200 bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
            >
              <img
                src={block.url ?? ''}
                alt={block.caption ?? ''}
                loading="lazy"
                className="w-full object-contain"
              />
            </button>
            {block.caption && (
              <figcaption className="mt-1.5 text-center text-xs text-slate-500">
                {block.caption}
              </figcaption>
            )}
          </figure>
        );
      })}

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
              {t('gallery.counter', { current: (openIndex ?? 0) + 1, total: shots.length })}
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
            {shots.length > 1 && (
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

            {shots.length > 1 && (
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
    </div>
  );
}
