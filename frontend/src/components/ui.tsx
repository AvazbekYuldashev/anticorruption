import type { ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';
import type { ButtonHTMLAttributes, InputHTMLAttributes } from 'react';
import { useTranslation } from 'react-i18next';

/*
 * Ommaviy sayt uchun asosiy UI qismlari.
 *
 * Tailwind sinflari shu yerda bir marta yig'ilgan: sahifalar ularni
 * takrorlamaydi, shuning uchun ko'rinishni bitta joydan o'zgartirish mumkin.
 * Admin panel MUI ishlatadi va bu komponentlarga tegmaydi.
 */

// ---------------------------------------------------------------- tugma

type ButtonVariant = 'primary' | 'accent' | 'secondary' | 'outline' | 'danger' | 'ghost';

/*
 * Tugma tekis bo'yoq emas: yengil gradient va o'z rangidagi yumshoq soya
 * beriladi. Shu tufayli u sahifadan "ko'tarilib" turadi va ko'z birinchi
 * navbatda unga tushadi. Aynan shu til admin panelida ham ishlatilgan
 * (admin/AdminApp.tsx dagi MuiButton) - ikki qism bir tizimning bo'lagi
 * ekani shundan ham sezilishi kerak.
 *
 * `accent` - sahifadagi eng asosiy bitta harakat uchun (bosh bannerdagi
 * "Murojaat qoldirish"). Uni ko'p ishlatish ma'nosini yo'qotadi.
 */
const BUTTON_STYLES: Record<ButtonVariant, string> = {
  primary: `bg-gradient-to-br from-brand-500 to-brand-700 text-white
    shadow-md shadow-brand-700/25 hover:shadow-lg hover:shadow-brand-700/35
    focus-visible:outline-brand-600`,
  // Matni to'q ko'k: oltin fonda oq yozuv o'qilmaydi (kontrast 2:1 dan past).
  accent: `bg-gradient-to-br from-accent-300 to-accent-500 text-brand-900
    shadow-md shadow-accent-700/30 hover:shadow-lg hover:shadow-accent-700/40
    focus-visible:outline-accent-500`,
  secondary: 'bg-slate-800 dark:bg-slate-700 text-white shadow-md shadow-slate-900/20 hover:bg-slate-900 dark:hover:bg-slate-600',
  outline:
    'border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 shadow-sm hover:border-brand-300 dark:hover:border-brand-400/40 hover:bg-brand-50 dark:hover:bg-brand-500/15 hover:text-brand-700 dark:hover:text-brand-300',
  danger: 'bg-red-600 text-white shadow-md shadow-red-700/25 hover:bg-red-700 focus-visible:outline-red-600',
  ghost: 'text-brand-700 dark:text-brand-300 hover:bg-brand-50 dark:hover:bg-brand-500/15',
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  fullWidth?: boolean;
}

export function Button({
  variant = 'primary',
  fullWidth,
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold
        transition-all duration-150 focus-visible:outline-2 focus-visible:outline-offset-2
        hover:-translate-y-px active:translate-y-0
        disabled:cursor-not-allowed disabled:opacity-60 disabled:shadow-none disabled:hover:translate-y-0
        ${BUTTON_STYLES[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
    />
  );
}

// ---------------------------------------------------------------- maydonlar

interface FieldProps {
  label: string;
  /** Maydon ostidagi tushuntirish - foydalanuvchiga nima yozishni aytadi. */
  hint?: string;
  error?: string;
  required?: boolean;
  children: ReactNode;
}

export function Field({ label, hint, error, required, children }: FieldProps) {
  const { t } = useTranslation();

  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
        {label}
        {required ? (
          <span className="ml-1 text-red-500">*</span>
        ) : (
          <span className="ml-1.5 text-xs font-normal text-slate-400 dark:text-slate-500">({t('common.optional')})</span>
        )}
      </span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">{hint}</span>}
      {error && <span className="mt-1 block text-xs font-medium text-red-600 dark:text-red-400">{error}</span>}
    </label>
  );
}

const CONTROL_CLASS = `w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2.5 text-sm text-slate-800 dark:text-slate-200
  placeholder:text-slate-400 dark:placeholder:text-slate-500 focus:border-brand-500 focus:ring-2 focus:ring-brand-100 dark:focus:ring-brand-500/30 focus:outline-none
  disabled:bg-slate-50 dark:disabled:bg-slate-800/60 disabled:text-slate-500 dark:disabled:text-slate-400`;

export function Input({ className = '', ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`${CONTROL_CLASS} ${className}`} />;
}

export function Textarea({ className = '', ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={`${CONTROL_CLASS} ${className}`} />;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  options: { value: string; label: string }[];
  /** Bo'sh variant matni; berilmasa bo'sh variant ko'rsatilmaydi. */
  placeholder?: string;
}

export function Select({ options, placeholder, className = '', ...props }: SelectProps) {
  return (
    <select {...props} className={`${CONTROL_CLASS} ${className}`}>
      {placeholder !== undefined && <option value="">{placeholder}</option>}
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}

interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string;
  hint?: string;
}

export function Checkbox({ label, hint, ...props }: CheckboxProps) {
  return (
    <div>
      <label className="flex cursor-pointer items-start gap-3">
        <input
          {...props}
          type="checkbox"
          className="mt-0.5 h-4 w-4 rounded border-slate-300 dark:border-slate-700 text-brand-600 dark:text-brand-300 focus:ring-brand-500"
        />
        <span className="text-sm font-medium text-slate-700 dark:text-slate-300">{label}</span>
      </label>
      {hint && <p className="mt-1.5 ml-7 text-xs text-slate-500 dark:text-slate-400">{hint}</p>}
    </div>
  );
}

// ---------------------------------------------------------------- konteynerlar

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 shadow-sm ${className}`}>
      {children}
    </div>
  );
}

export function PageHeader({ title, description }: { title: string; description?: string }) {
  return (
    <header className="mb-8">
      <h1 className="text-2xl font-semibold text-slate-900 dark:text-slate-100 sm:text-3xl">{title}</h1>
      {/* Qisqa rangli chiziq - sarlavha va matn orasidagi ajratkich. */}
      <span className="mt-3 block h-1 w-12 rounded-full bg-brand-500" />
      {description && <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600 dark:text-slate-400">{description}</p>}
    </header>
  );
}

export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="border-t border-slate-200 dark:border-slate-800 pt-6">
      <legend className="sr-only">{title}</legend>
      <h2 className="mb-4 text-sm font-semibold tracking-wide text-slate-500 dark:text-slate-400 uppercase">{title}</h2>
      <div className="space-y-5">{children}</div>
    </fieldset>
  );
}

// ---------------------------------------------------------------- holat ko'rsatkichlari

export function Spinner({ label }: { label?: string }) {
  const { t } = useTranslation();
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-sm text-slate-500 dark:text-slate-400">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 dark:border-slate-700 border-t-brand-600" />
      {label ?? t('common.loading')}
    </div>
  );
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  const { t } = useTranslation();
  return (
    <div className="rounded-xl border border-red-200 dark:border-red-500/30 bg-red-50 dark:bg-red-500/15 p-4">
      <p className="text-sm text-red-800 dark:text-red-200">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 text-sm font-medium text-red-700 dark:text-red-300 underline"
        >
          {t('common.retry')}
        </button>
      )}
    </div>
  );
}

export function EmptyState({ message, children }: { message: string; children?: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-6 py-12 text-center">
      {/* Belgi bo'sh joyni "buzilgan" emas, "hozircha yo'q" deb o'qitadi. */}
      <span
        aria-hidden
        className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-800 text-lg text-slate-400 dark:text-slate-500"
      >
        —
      </span>
      <p className="text-sm text-slate-500 dark:text-slate-400">{message}</p>
      {children && <div className="mt-4">{children}</div>}
    </div>
  );
}

// ---------------------------------------------------------------- holat nishoni

/**
 * Murojaat yoki so'rovnoma holatini rangli nishon bilan ko'rsatadi.
 *
 * <p>Matn backend'dan keladi (joriy tilda), rang esa mashina qiymatiga
 * bog'lanadi - shuning uchun til o'zgarganda ranglar joyida qoladi.
 */
const STATUS_COLORS: Record<string, string> = {
  NEW: 'bg-blue-50 dark:bg-blue-500/15 text-blue-700 dark:text-blue-300 ring-blue-200 dark:ring-blue-500/30',
  IN_REVIEW: 'bg-amber-50 dark:bg-amber-500/15 text-amber-700 dark:text-amber-300 ring-amber-200 dark:ring-amber-500/30',
  NEED_INFO: 'bg-purple-50 dark:bg-purple-500/15 text-purple-700 dark:text-purple-300 ring-purple-200 dark:ring-purple-500/30',
  RESOLVED: 'bg-emerald-50 dark:bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 ring-emerald-200 dark:ring-emerald-500/30',
  REJECTED: 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 ring-slate-300 dark:ring-slate-600',

  // So'rovnoma holatlari - nomlari murojaat holatlari bilan to'qnashmaydi
  DRAFT: 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 ring-slate-300 dark:ring-slate-600',
  SCHEDULED: 'bg-blue-50 dark:bg-blue-500/15 text-blue-700 dark:text-blue-300 ring-blue-200 dark:ring-blue-500/30',
  OPEN: 'bg-emerald-50 dark:bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 ring-emerald-200 dark:ring-emerald-500/30',
  STOPPED: 'bg-amber-50 dark:bg-amber-500/15 text-amber-700 dark:text-amber-300 ring-amber-200 dark:ring-amber-500/30',
  CLOSED: 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 ring-slate-300 dark:ring-slate-600',
};

export function StatusBadge({ status, label }: { status: string; label: string }) {
  const color = STATUS_COLORS[status] ?? 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 ring-slate-300 dark:ring-slate-600';
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${color}`}
    >
      {label}
    </span>
  );
}

// ---------------------------------------------------------------- sahifalash

interface PaginationProps {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  const { t } = useTranslation();
  if (totalPages <= 1) return null;

  return (
    <nav className="mt-6 flex items-center justify-center gap-3">
      <Button variant="outline" disabled={page === 0} onClick={() => onChange(page - 1)}>
        {t('common.prev')}
      </Button>
      <span className="text-sm text-slate-600 dark:text-slate-400">
        {t('common.pageOf', { page: page + 1, total: totalPages })}
      </span>
      <Button
        variant="outline"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        {t('common.next')}
      </Button>
    </nav>
  );
}
