import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

interface Props {
  /** Kirish uchun talab qilinadigan eng past daraja. */
  require?: 'user' | 'staff' | 'admin';
}

/**
 * Ruxsat tekshiruvi.
 *
 * <p>Backend baribir har bir so'rovni o'zi tekshiradi - bu yerdagi tekshiruv
 * xavfsizlik chorasi emas, foydalanuvchini keraksiz "403" ekranidan
 * qutqaradigan qulaylik. Shuning uchun uni chetlab o'tish hech narsa bermaydi.
 */
export function ProtectedRoute({ require = 'user' }: Props) {
  const { user, isStaff, isAdmin, initializing } = useAuth();
  const location = useLocation();
  const { t } = useTranslation();

  // Token tekshirilmaguncha kutamiz, aks holda kirgan foydalanuvchi
  // ham bir lahzaga login sahifasiga uloqtirilardi.
  if (initializing) {
    return <div className="p-8 text-center text-slate-500">{t('common.loading')}</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (require === 'staff' && !isStaff) {
    return <Navigate to="/" replace />;
  }

  if (require === 'admin' && !isAdmin) {
    return <Navigate to="/admin" replace />;
  }

  return <Outlet />;
}
