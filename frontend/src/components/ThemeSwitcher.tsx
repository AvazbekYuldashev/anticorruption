import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { THEME_MODES, setThemeMode, useThemeMode, type ThemeMode } from '../lib/theme';

const iconProps = {
  width: 16,
  height: 16,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
};

const ICONS: Record<ThemeMode, ReactNode> = {
  light: (
    <svg {...iconProps}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
    </svg>
  ),
  dark: (
    <svg {...iconProps}>
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
    </svg>
  ),
  auto: (
    <svg {...iconProps}>
      <rect x="2" y="4" width="20" height="13" rx="2" />
      <path d="M8 21h8M12 17v4" />
    </svg>
  ),
};

/**
 * Yorug' / qorong'i / avtomatik rejim tanlagichi - sayt tepasidagi ko'k qatorda.
 *
 * <p>Keng ekranda uchala rejim yonma-yon turadi. Telefonda qator tor, shuning
 * uchun bitta tugma qoladi: u joriy rejimni ko'rsatadi va bosilganda
 * navbatdagisiga o'tadi.
 */
export function ThemeSwitcher({ compact = false }: { compact?: boolean }) {
  const { t } = useTranslation();
  const mode = useThemeMode();

  if (compact) {
    const next = THEME_MODES[(THEME_MODES.indexOf(mode) + 1) % THEME_MODES.length];
    return (
      <button
        type="button"
        onClick={() => setThemeMode(next)}
        title={`${t('theme.label')}: ${t(`theme.${mode}`)}`}
        aria-label={`${t('theme.label')}: ${t(`theme.${mode}`)}`}
        className="rounded p-1.5 text-white/80 transition-colors hover:bg-white/10 hover:text-white"
      >
        {ICONS[mode]}
      </button>
    );
  }

  return (
    <div
      className="flex items-center gap-0.5 rounded-lg bg-white/10 p-0.5"
      role="radiogroup"
      aria-label={t('theme.label')}
    >
      {THEME_MODES.map((option) => {
        const active = option === mode;
        return (
          <button
            key={option}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => setThemeMode(option)}
            title={t(`theme.${option}`)}
            aria-label={t(`theme.${option}`)}
            className={`rounded-md p-1.5 transition-colors ${
              active ? 'bg-white/25 text-white shadow-sm' : 'text-white/65 hover:bg-white/10 hover:text-white'
            }`}
          >
            {ICONS[option]}
          </button>
        );
      })}
    </div>
  );
}
