import type { ReactNode } from 'react';
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
import { errorMessage } from '../lib/errors';

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

/** Amal xatoligi uchun qisqa xabar. */
export function MutationError({ error }: { error: unknown }) {
  const { t } = useTranslation();
  if (!error) return null;
  return (
    <Alert severity="error" sx={{ mt: 2 }}>
      {errorMessage(error, t)}
    </Alert>
  );
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
}: {
  open: boolean;
  title: string;
  busy?: boolean;
  error?: unknown;
  onClose: () => void;
  onSubmit: () => void;
  children: ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md';
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
            {t('common.save')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
