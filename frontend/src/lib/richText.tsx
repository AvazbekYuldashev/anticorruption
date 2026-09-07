import type { ReactNode } from 'react';

/**
 * Matn ichidagi formatlash belgilarini React elementlariga o'giradi.
 *
 * <p>Qo'llab-quvvatlanadigan belgilar:
 * `**qalin**`, `*kursiv*`, `__tagli__`, `~~chizilgan~~`, `[matn](havola)`.
 *
 * <p>Nega HTML saqlanmaydi: admin yozgan HTML ni sahifaga qo'yish uchun uni
 * tozalash kerak bo'lardi va bitta e'tibordan chetda qolgan teg saytga
 * begona skript kiritish yo'lini ochib berardi. Belgilar esa hech qachon
 * HTML ga aylanmaydi - bu yerda faqat quyida sanalgan elementlar yasaladi,
 * qolgan hamma narsa oddiy matn bo'lib qoladi.
 */

/** Muharrir tanlangan matnni shu belgilar bilan o'raydi. */
export const RICH_TEXT_MARKS = {
  bold: '**',
  italic: '*',
  underline: '__',
  strike: '~~',
} as const;

/** Matn maydonida bajarilgan o'zgarish va undan keyingi tanlov. */
export interface MarkupEdit {
  text: string;
  /** Muharrir kursorni shu oraliqqa qo'yadi. */
  selection: [number, number];
}

/**
 * Tanlangan matnni belgilar bilan o'raydi.
 *
 * <p>Hech narsa tanlanmagan bo'lsa namuna so'z qo'yiladi va o'sha tanlangan
 * holda qoladi - foydalanuvchi darhol ustidan yozib ketaveradi.
 *
 * <p>Alohida funksiya: shu tufayli uni komponentsiz, to'g'ridan-to'g'ri
 * sinab ko'rish mumkin.
 */
export function applyMark(
  text: string,
  start: number,
  end: number,
  mark: string,
  sample: string,
): MarkupEdit {
  const selected = text.slice(start, end) || sample;

  return {
    text: text.slice(0, start) + mark + selected + mark + text.slice(end),
    selection: [start + mark.length, start + mark.length + selected.length],
  };
}

/**
 * Tanlangan matnni havolaga aylantiradi.
 *
 * <p>Kursor manzil o'rniga qo'yiladi va o'rin egallovchi tanlangan bo'ladi:
 * foydalanuvchi manzilni yopishtiradi-yu, ishi tugaydi.
 */
export function applyLink(
  text: string,
  start: number,
  end: number,
  sample: string,
  placeholder: string,
): MarkupEdit {
  const selected = text.slice(start, end) || sample;
  // "[" + matn + "](" - manzil shundan keyin boshlanadi.
  const hrefStart = start + selected.length + 3;

  return {
    text: `${text.slice(0, start)}[${selected}](${placeholder})${text.slice(end)}`,
    selection: [hrefStart, hrefStart + placeholder.length],
  };
}

/**
 * Eng yaqin formatlash belgisini topadi.
 *
 * <p>Qalin `**` kursivdan oldin turadi, aks holda `**matn**` ikkita bo'sh
 * kursivga bo'linib ketardi. Kursivning ichi `*` ni qabul qilmaydi - shu
 * tufayli u qo'shni qalin belgilarni "yutib" yubormaydi.
 */
const MARKUP =
  /\*\*([\s\S]+?)\*\*|__([\s\S]+?)__|~~([\s\S]+?)~~|\*([^*\n]+?)\*|\[([^\]\n]+)\]\(([^)\s]+)\)/;

const SAFE_SCHEMES = ['http:', 'https:', 'mailto:'];

/** Matnni formatlangan holda chizadi. Bo'sh matn uchun null. */
export function renderRichText(text: string | null | undefined): ReactNode {
  if (!text) return null;
  return parse(text, 'r');
}

function parse(text: string, keyPrefix: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  let rest = text;
  let index = 0;

  while (rest.length > 0) {
    const match = MARKUP.exec(rest);
    if (!match) {
      nodes.push(rest);
      break;
    }

    if (match.index > 0) {
      nodes.push(rest.slice(0, match.index));
    }

    const key = `${keyPrefix}-${index}`;
    index += 1;
    nodes.push(element(match, key));

    // Har bir moslik kamida uch belgi - halqa albatta tugaydi.
    rest = rest.slice(match.index + match[0].length);
  }

  return nodes;
}

function element(match: RegExpExecArray, key: string): ReactNode {
  const [, bold, underline, strike, italic, linkText, linkHref] = match;

  if (bold !== undefined) return <strong key={key}>{parse(bold, key)}</strong>;
  if (underline !== undefined) return <u key={key}>{parse(underline, key)}</u>;
  if (strike !== undefined) return <s key={key}>{parse(strike, key)}</s>;
  if (italic !== undefined) return <em key={key}>{parse(italic, key)}</em>;

  const href = safeHref(linkHref);
  if (href === null) {
    // Ruxsat etilmagan sxema (masalan `javascript:`) - havola emas, oddiy matn.
    return <span key={key}>{match[0]}</span>;
  }

  return (
    <a key={key} href={href} target="_blank" rel="noopener noreferrer">
      {linkText}
    </a>
  );
}

/**
 * Havola manzilini tekshiradi.
 *
 * <p>Faqat sanab o'tilgan sxemalar o'tadi. `javascript:` va `data:` kabi
 * manzillar havola bo'lmaydi - ular bosilganda sahifada kod bajarilardi.
 * Ro'yxat "taqiqlanganlar" emas, "ruxsat etilganlar": yangi xavfli sxema
 * paydo bo'lsa ham u avtomatik ravishda tashqarida qoladi.
 */
export function safeHref(raw: string): string | null {
  try {
    const url = new URL(raw, window.location.origin);
    return SAFE_SCHEMES.includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}
