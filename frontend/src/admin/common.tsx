import type { ReactNode } from 'react';
import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material';
import { errorMessage, fieldErrors } from '../lib/errors';

/** Sarlavha va o'ng tomondagi asosiy amal tugmasi. */
export function AdminPage({
  title,
  description,
  action,
  children,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <Box>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
       
       
        spacing={2}
        sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between', mb: 3 }}
      >
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 600 }}>
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, maxWidth: 720 }}>
              {description}
            </Typography>
          )}
        </Box>
        {action}
      </Stack>
      {children}
    </Box>
  );
}

/**
 * So'rovning yuklanish va xatolik holatlarini bir joyda ko'rsatadi.
 * Har bir sahifada bir xil uch shoxli shartni yozmaslik uchun.
 */
export function QueryState({
  isPending,
  error,
  children,
}: {
  isPending: boolean;
  error: unknown;
  children: ReactNode;
}) {
  const { t } = useTranslation();

  if (isPending) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={28} />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{errorMessage(error, t)}</Alert>;
  }

  return <>{children}</>;
}

/**
 * Xatolikni ko'rsatadi.
 *
 * <p>Validatsiya xatoligida umumiy xabar ("Kiritilgan ma'lumotlarda xatolik
 * bor") qaysi maydonda muammo borligini aytmaydi - shuning uchun backend
 * qaytargan maydonlar ro'yxati ham chiqariladi. Aks holda foydalanuvchi
 * uzun shaklni ko'zi bilan qidirib chiqishga majbur bo'lardi.
 */
export function MutationError({ error }: { error: unknown }) {
  const { t } = useTranslation();
  if (!error) return null;

  const fields = Object.entries(fieldErrors(error));

  return (
    <Alert severity="error" sx={{ mt: 2 }}>
      {errorMessage(error, t)}

      {fields.length > 0 && (
        <Box component="ul" sx={{ m: 0, mt: 1, pl: 2.5 }}>
          {fields.map(([field, message]) => (
            <Box component="li" key={field}>
              <Typography variant="body2" component="span">
                <b>{fieldLabel(field, t)}</b> — {message}
              </Typography>
            </Box>
          ))}
        </Box>
      )}
    </Alert>
  );
}

/**
 * Maydon yo'lini o'qiladigan nomga aylantiradi.
 *
 * <p>Backend `questions[0].options` kabi yo'l yuboradi. Har bir bo'lak
 * alohida tarjima qilinadi, raqam esa odam sanaydigan ko'rinishga
 * o'tkaziladi (0 -> 1). Tarjimasi yo'q bo'lak o'z nomi bilan qoladi:
 * noma'lum maydon ham hech narsadan ko'ra foydaliroq.
 */
function fieldLabel(path: string, t: TFunction): string {
  return path
    .split('.')
    .map((segment) => {
      const match = /^(\w+)\[(\d+)\]$/.exec(segment);
      const name = match ? match[1] : segment;
      const label = t(`admin.fieldNames.${name}`, name);

      return match ? `${label} ${Number(match[2]) + 1}` : label;
    })
    .join(' · ');
}

const STATUS_COLORS: Record<
  string,
  'default' | 'primary' | 'warning' | 'secondary' | 'success' | 'error'
> = {
  NEW: 'primary',
  IN_REVIEW: 'warning',
  NEED_INFO: 'secondary',
  RESOLVED: 'success',
  REJECTED: 'default',
};

export function StatusChip({ status, label }: { status: string; label: string }) {
  return <Chip size="small" label={label} color={STATUS_COLORS[status] ?? 'default'} />;
}

/** O'chirishdan oldin tasdiqlash. */
export function ConfirmDialog({
  open,
  title,
  message,
  busy,
  onCancel,
  onConfirm,
}: {
  open: boolean;
  title: string;
  message?: string;
  busy?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onClose={onCancel} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary">
          {message ?? t('common.deleteConfirm')}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={busy}>
          {t('common.cancel')}
        </Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={busy}>
          {t('common.delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/** Yaratish/tahrirlash oynasi uchun umumiy chig'anoq. */
export function FormDialog({
  open,
  title,
  busy,
  error,
  onClose,
  onSubmit,
  children,
  maxWidth = 'sm',
  submitLabel,
}: {
  open: boolean;
  title: string;
  busy?: boolean;
  error?: unknown;
  onClose: () => void;
  onSubmit: () => void;
  children: ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md';
  /** Tasdiqlash tugmasi matni; berilmasa "Saqlash". */
  submitLabel?: string;
}) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onClose={onClose} maxWidth={maxWidth} fullWidth>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit();
        }}
      >
        <DialogTitle>{title}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {children}
          </Stack>
          <MutationError error={error} />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={busy}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" variant="contained" disabled={busy}>
            {submitLabel ?? t('common.save')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
