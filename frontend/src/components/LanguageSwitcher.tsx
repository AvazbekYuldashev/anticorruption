import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { LANGUAGES, changeLanguage, type LanguageCode } from '../i18n';

/**
 * Til almashtirgich.
 *
 * <p>Til o'zgarganda barcha so'rovlar bekor qilinadi va qaytadan yuboriladi:
 * backend nomlarni (holat, kategoriya, fakultet) joriy tilda qaytaradi,
 * shuning uchun keshdagi eski javoblar endi to'g'ri kelmaydi.
 */
export function LanguageSwitcher({ compact = false }: { compact?: boolean }) {
  const { i18n, t } = useTranslation();
  const queryClient = useQueryClient();

  async function handleChange(code: LanguageCode) {
    if (code === i18n.language) return;
    await changeLanguage(code);
    await queryClient.invalidateQueries();
  }

  return (
    <div className="flex items-center gap-1" role="group" aria-label={t('common.language')}>
      {LANGUAGES.map((language) => {
        const active = i18n.language === language.code;
        return (
          <button
            key={language.code}
            type="button"
            onClick={() => void handleChange(language.code)}
            aria-current={active ? 'true' : undefined}
            className={`rounded px-2 py-1 text-xs font-medium transition-colors ${
              active
                ? 'bg-white/20 text-white'
                : 'text-white/70 hover:bg-white/10 hover:text-white'
            }`}
          >
            {compact ? language.code.toUpperCase().slice(0, 2) : language.name}
          </button>
        );
      })}
    </div>
  );
}
