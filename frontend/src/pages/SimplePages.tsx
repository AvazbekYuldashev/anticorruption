import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/ui';

export function NotFoundPage() {
  const { t } = useTranslation();

  return (
    <div className="mx-auto max-w-lg py-16 text-center">
      <p className="text-6xl font-semibold text-brand-200">404</p>
      <h1 className="mt-4 text-xl font-semibold text-slate-900">{t('common.notFound')}</h1>
      <p className="mt-2 text-sm text-slate-600">{t('common.notFoundHint')}</p>
      <Link to="/" className="mt-6 inline-block">
        <Button>{t('common.goHome')}</Button>
      </Link>
    </div>
  );
}
