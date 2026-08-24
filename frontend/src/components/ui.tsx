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

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost';

const BUTTON_STYLES: Record<ButtonVariant, string> = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 focus-visible:outline-brand-600',
  secondary: 'bg-slate-800 text-white hover:bg-slate-900 focus-visible:outline-slate-800',
  outline: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50',
  danger: 'bg-red-600 text-white hover:bg-red-700 focus-visible:outline-red-600',
  ghost: 'text-brand-700 hover:bg-brand-50',
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
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium
        transition-colors focus-visible:outline-2 focus-visible:outline-offset-2
        disabled:cursor-not-allowed disabled:opacity-60
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
      <span className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
        {required ? (
          <span className="ml-1 text-red-500">*</span>
        ) : (
          <span className="ml-1.5 text-xs font-normal text-slate-400">({t('common.optional')})</span>
        )}
      </span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
      {error && <span className="mt-1 block text-xs font-medium text-red-600">{error}</span>}
    </label>
  );
}

const CONTROL_CLASS = `w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800
  placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-100 focus:outline-none
  disabled:bg-slate-50 disabled:text-slate-500`;

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
          className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
        />
        <span className="text-sm font-medium text-slate-700">{label}</span>
      </label>
      {hint && <p className="mt-1.5 ml-7 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

// ---------------------------------------------------------------- konteynerlar

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-slate-200 bg-white p-6 shadow-sm ${className}`}>
      {children}
    </div>
  );
}

export function PageHeader({ title, description }: { title: string; description?: string }) {
  return (
    <header className="mb-8">
      <h1 className="text-2xl font-semibold text-slate-900 sm:text-3xl">{title}</h1>
      {description && <p className="mt-2 max-w-3xl text-sm text-slate-600">{description}</p>}
    </header>
  );
}

export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="border-t border-slate-200 pt-6">
      <legend className="sr-only">{title}</legend>
      <h2 className="mb-4 text-sm font-semibold tracking-wide text-slate-500 uppercase">{title}</h2>
      <div className="space-y-5">{children}</div>
    </fieldset>
  );
}

// ---------------------------------------------------------------- holat ko'rsatkichlari

export function Spinner({ label }: { label?: string }) {
  const { t } = useTranslation();
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-sm text-slate-500">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-brand-600" />
      {label ?? t('common.loading')}
    </div>
  );
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  const { t } = useTranslation();
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4">
      <p className="text-sm text-red-800">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 text-sm font-medium text-red-700 underline"
        >
          {t('common.retry')}
        </button>
      )}
    </div>
  );
}

export function EmptyState({ message, children }: { message: string; children?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
      <p className="text-sm text-slate-500">{message}</p>
      {children && <div className="mt-4">{children}</div>}
    </div>
  );
}

// ---------------------------------------------------------------- holat nishoni

/**
 * Murojaat holatini rangli nishon bilan ko'rsatadi.
 *
 * <p>Matn backend'dan keladi (joriy tilda), rang esa mashina qiymatiga
 * bog'lanadi - shuning uchun til o'zgarganda ranglar joyida qoladi.
 */
const STATUS_COLORS: Record<string, string> = {
  NEW: 'bg-blue-50 text-blue-700 ring-blue-200',
  IN_REVIEW: 'bg-amber-50 text-amber-700 ring-amber-200',
  NEED_INFO: 'bg-purple-50 text-purple-700 ring-purple-200',
  RESOLVED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  REJECTED: 'bg-slate-100 text-slate-600 ring-slate-300',
};

export function StatusBadge({ status, label }: { status: string; label: string }) {
  const color = STATUS_COLORS[status] ?? 'bg-slate-100 text-slate-600 ring-slate-300';
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
      <span className="text-sm text-slate-600">
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
