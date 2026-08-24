import i18next from 'i18next';

/**
 * Sana va raqamlarni joriy tilga mos formatlash.
 *
 * <p>`Intl` ishlatiladi - tashqi kutubxona qo'shilmaydi. Kirill o'zbekcha
 * uchun brauzerda alohida ma'lumot bo'lmasligi mumkin, shuning uchun
 * u rus formatiga o'giriladi: ikkalasi ham "kun.oy.yil" ko'rinishida.
 */
function intlLocale(): string {
  switch (i18next.language) {
    case 'uz':
      return 'uz-Latn-UZ';
    case 'uz-cyrl':
      return 'ru-RU';
    case 'ru':
      return 'ru-RU';
    default:
      return 'en-GB';
  }
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return new Intl.DateTimeFormat(intlLocale(), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value));
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  return new Intl.DateTimeFormat(intlLocale(), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat(intlLocale()).format(value);
}

/** Fayl hajmini odam o'qiydigan ko'rinishga o'giradi. */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
