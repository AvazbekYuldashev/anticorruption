import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Chip,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { referenceApi } from '../api/reference';
import { usersApi, type CreateUserPayload } from '../api/users';
import type { Role } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { formatDate } from '../lib/format';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

const ROLES: Role[] = ['CITIZEN', 'MODERATOR', 'ADMIN'];

const EMPTY_FORM: CreateUserPayload = {
  fullName: '',
  email: '',
  phone: '',
  password: '',
  role: 'CITIZEN',
};

export function UsersPage() {
  const { t } = useTranslation();
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();

  const [role, setRole] = useState<Role | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [form, setForm] = useState<CreateUserPayload | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; name: string } | null>(null);

  // Rol nomlari backenddan keladi - ular joriy tilda bo'ladi.
  const reference = useQuery({ queryKey: ['reference'], queryFn: referenceApi.all });

  const query = useQuery({
    queryKey: ['admin', 'users', role, page, size],
    queryFn: () => usersApi.list(role, page, size),
    placeholderData: keepPreviousData,
  });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
  }

  const roleMutation = useMutation({
    mutationFn: ({ id, newRole }: { id: number; newRole: Role }) =>
      usersApi.changeRole(id, newRole),
    onSuccess: refresh,
  });

  const stateMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      usersApi.setEnabled(id, enabled),
    onSuccess: refresh,
  });

  const create = useMutation({
    mutationFn: () => usersApi.create(form!),
    onSuccess: () => {
      setForm(null);
      refresh();
    },
  });

  const remove = useMutation({
    mutationFn: () => usersApi.remove(deleting!.id),
    onSuccess: () => {
      setDeleting(null);
      refresh();
    },
  });

  /** Rol nomi joriy tilda ma'lumotnomadan olinadi. */
  function roleLabel(value: Role): string {
    return reference.data?.roles.find((option) => option.value === value)?.label ?? value;
  }

  return (
    <AdminPage
      title={t('admin.usersTitle')}
      description={t('admin.usersHint')}
      action={
        <Button variant="contained" onClick={() => setForm(EMPTY_FORM)}>
          {t('admin.addUser')}
        </Button>
      }
    >
      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
        <TextField
          select
          size="small"
          label={t('admin.colRole')}
          value={role}
          onChange={(event) => {
            setRole(event.target.value as Role | '');
            setPage(0);
          }}
          sx={{ minWidth: 220 }}
        >
          <MenuItem value="">{t('common.all')}</MenuItem>
          {ROLES.map((value) => (
            <MenuItem key={value} value={value}>
              {roleLabel(value)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      <MutationError error={roleMutation.error ?? stateMutation.error ?? remove.error} />

      <QueryState isPending={query.isPending} error={query.error}>
        {query.data && (
          <Paper variant="outlined" sx={{ mt: 2 }}>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>{t('admin.colName')}</TableCell>
                    <TableCell>{t('admin.colEmail')}</TableCell>
                    <TableCell>{t('admin.colRole')}</TableCell>
                    <TableCell>{t('admin.colState')}</TableCell>
                    <TableCell>{t('admin.colDate')}</TableCell>
                    <TableCell align="right">{t('common.actions')}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {query.data.content.map((user) => {
                    // Administrator o'z rolini va holatini o'zgartira olmaydi -
                    // backend ham buni rad etadi, interfeys ham taklif qilmaydi.
                    const isSelf = user.id === currentUser?.id;

                    return (
                      <TableRow key={user.id} hover>
                        <TableCell>{user.fullName}</TableCell>
                        <TableCell>
                          <Typography variant="body2" color="text.secondary">
                            {user.email}
                          </Typography>
                        </TableCell>
                        <TableCell sx={{ minWidth: 180 }}>
                          <TextField
                            select
                            size="small"
                            value={user.role}
                            disabled={isSelf || roleMutation.isPending}
                            onChange={(event) =>
                              roleMutation.mutate({
                                id: user.id,
                                newRole: event.target.value as Role,
                              })
                            }
                            fullWidth
                          >
                            {ROLES.map((value) => (
                              <MenuItem key={value} value={value}>
                                {roleLabel(value)}
                              </MenuItem>
                            ))}
                          </TextField>
                        </TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={user.enabled ? t('common.active') : t('admin.blocked')}
                            color={user.enabled ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" color="text.secondary">
                            {formatDate(user.createdAt)}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Button
                            size="small"
                            color={user.enabled ? 'error' : 'primary'}
                            disabled={isSelf || stateMutation.isPending}
                            onClick={() =>
                              stateMutation.mutate({ id: user.id, enabled: !user.enabled })
                            }
                          >
                            {user.enabled ? t('admin.block') : t('admin.unblock')}
                          </Button>
                          {/* O'chirish - murojaati yo'q hisoblar uchun; qolganini bloklash kerak. */}
                          <Button
                            size="small"
                            color="error"
                            disabled={isSelf}
                            onClick={() => setDeleting({ id: user.id, name: user.fullName })}
                          >
                            {t('common.delete')}
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>

            <TablePagination
              component="div"
              count={query.data.totalElements}
              page={page}
              rowsPerPage={size}
              onPageChange={(_, next) => setPage(next)}
              onRowsPerPageChange={(event) => {
                setSize(Number(event.target.value));
                setPage(0);
              }}
              rowsPerPageOptions={[10, 20, 50]}
            />
          </Paper>
        )}
      </QueryState>

      {form && (
        <FormDialog
          open
          title={t('admin.addUser')}
          busy={create.isPending}
          error={create.error}
          onClose={() => setForm(null)}
          onSubmit={() => create.mutate()}
        >
          <TextField
            label={t('admin.colName')}
            value={form.fullName}
            onChange={(event) => setForm({ ...form, fullName: event.target.value })}
            required
            fullWidth
          />
          <TextField
            label={t('auth.fieldEmail')}
            type="email"
            value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })}
            helperText={t('admin.emailIsLogin')}
            required
            fullWidth
          />
          <TextField
            label={t('staff.phone')}
            value={form.phone ?? ''}
            onChange={(event) => setForm({ ...form, phone: event.target.value })}
            placeholder="+998901234567"
            fullWidth
          />
          <TextField
            label={t('auth.fieldPassword')}
            type="text"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            helperText={t('admin.passwordHint')}
            required
            fullWidth
          />
          <TextField
            select
            label={t('admin.colRole')}
            value={form.role}
            onChange={(event) => setForm({ ...form, role: event.target.value as Role })}
            fullWidth
          >
            {ROLES.map((value) => (
              <MenuItem key={value} value={value}>
                {roleLabel(value)}
              </MenuItem>
            ))}
          </TextField>
        </FormDialog>
      )}

      <ConfirmDialog
        open={deleting !== null}
        title={deleting?.name ?? ''}
        busy={remove.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => remove.mutate()}
      />
    </AdminPage>
  );
}
