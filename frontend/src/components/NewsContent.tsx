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
 * Chiqarish uchun guruh: oddiy blok yoki ketma-ket kelgan yakka rasmlar.
 *
 * <p>Yonma-yon turgan rasm bloklari ustma-ust cho'zilib ketmasligi kerak,
 * shuning uchun ular bitta albom kabi to'rga yig'iladi. Yolg'iz rasm esa
 * matn oqimida to'liq kenglikda qoladi.
 */
type Group =
  | { kind: 'block'; block: NewsBlockResponse }
  | { kind: 'images'; shots: Shot[] };

function groupBlocks(blocks: NewsBlockResponse[]): Group[] {
  const groups: Group[] = [];

  for (const block of blocks) {
    if (block.type === 'IMAGE' && block.url) {
      const shot: Shot = { key: `b${block.id}`, url: block.url, caption: block.caption };
      const last = groups.at(-1);

      if (last?.kind === 'images') {
        last.shots.push(shot);
      } else {
        groups.push({ kind: 'images', shots: [shot] });
      }
      continue;
    }

    groups.push({ kind: 'block', block });
  }

  return groups;
}

/** Bir xil o'lchamdagi nishonchalar to'ri: albom ham, rasmlar qatori ham shunday chiqadi. */
function ImageGrid({ shots, onOpen }: { shots: Shot[]; onOpen: (key: string) => void }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      {shots.map((shot) => (
        <button
          key={shot.key}
          type="button"
          onClick={() => onOpen(shot.key)}
          className="group overflow-hidden rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
        >
          <img
            src={shot.url}
            alt={shot.caption ?? ''}
            loading="lazy"
            className="aspect-[4/3] w-full object-cover transition-transform duration-200 group-hover:scale-105"
          />
        </button>
      ))}
    </div>
  );
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
  const groups = useMemo(() => groupBlocks(blocks), [blocks]);
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

  const open = useCallback(
    (key: string) => setOpenIndex(shots.findIndex((shot) => shot.key === key)),
    [shots],
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

  return (
    <div className="space-y-5">
      {groups.map((group) => {
        if (group.kind === 'images') {
          // Yolg'iz rasm matn oqimida to'liq kenglikda qoladi.
          if (group.shots.length === 1) {
            const shot = group.shots[0];
            return (
              <figure key={shot.key}>
                <button
                  type="button"
                  onClick={() => open(shot.key)}
                  className="block w-full overflow-hidden rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
                >
                  <img
                    src={shot.url}
                    alt={shot.caption ?? ''}
                    loading="lazy"
                    className="w-full object-contain"
                  />
                </button>
                {shot.caption && (
                  <figcaption className="mt-1.5 text-center text-xs text-slate-500 dark:text-slate-400">
                    {shot.caption}
                  </figcaption>
                )}
              </figure>
            );
          }

          return <ImageGrid key={group.shots[0].key} shots={group.shots} onOpen={open} />;
        }

        const block = group.block;

        if (block.type === 'HEADING') {
          return (
            <h2
              key={block.id}
              className="prose-content pt-2 text-lg font-semibold text-slate-900 dark:text-slate-100 sm:text-xl"
            >
              {renderRichText(block.text)}
            </h2>
          );
        }

        if (block.type === 'GALLERY') {
          return (
            <figure key={block.id}>
              <ImageGrid
                shots={block.images.map((image) => ({
                  key: `i${image.id}`,
                  url: image.url,
                  caption: image.caption,
                }))}
                onOpen={open}
              />
              {block.caption && (
                <figcaption className="mt-1.5 text-center text-xs text-slate-500 dark:text-slate-400">
                  {block.caption}
                </figcaption>
              )}
            </figure>
          );
        }

        if (block.type === 'TEXT') {
          return (
            <p key={block.id} className="prose-content text-slate-700 dark:text-slate-300">
              {renderRichText(block.text)}
            </p>
          );
        }

        // Fayli yo'q rasm bloki: bo'sh ramka o'rniga hech narsa chiqmaydi.
        return null;
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

          {/*
            O'qlar rasm yonida emas, uning ustida - ekran chetlarida turadi:
            keng rasm butun bo'shliqni egallaganda ham ular ko'rinib qoladi.
          */}
          <div
            className="relative flex min-h-0 flex-1 items-center justify-center"
            onClick={(event) => event.stopPropagation()}
          >
            {shots.length > 1 && (
              <button
                type="button"
                onClick={() => step(-1)}
                aria-label={t('gallery.prev')}
                className="absolute top-1/2 left-0 z-10 -translate-y-1/2 rounded-full bg-white/10 p-3 text-2xl leading-none text-white hover:bg-white/20 sm:p-4 sm:text-3xl"
              >
                ‹
              </button>
            )}

            <div className="flex h-full w-full items-center justify-center px-14 sm:px-20">
              <img
                src={active.url}
                alt={active.caption ?? ''}
                className="max-h-full max-w-full object-contain"
              />
            </div>

            {shots.length > 1 && (
              <button
                type="button"
                onClick={() => step(1)}
                aria-label={t('gallery.next')}
                className="absolute top-1/2 right-0 z-10 -translate-y-1/2 rounded-full bg-white/10 p-3 text-2xl leading-none text-white hover:bg-white/20 sm:p-4 sm:text-3xl"
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
