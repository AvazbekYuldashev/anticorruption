import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation } from '@tanstack/react-query';
import { Alert, Box, Button, Divider, Stack, TextField, Typography } from '@mui/material';
import { usersApi } from '../api/users';
import { useAuth } from '../auth/AuthContext';
import { AdminPage, MutationError } from './common';

/**
 * O'z hisobi sozlamalari.
 *
 * <p>Hisoblarni administrator ochadi va dastlabki parolni o'zi beradi,
 * shuning uchun egasida uni almashtirish imkoni bo'lishi shart: aks holda
 * administrator bilgan parol muddatsiz amal qilardi.
 *
 * <p>Email ayni paytda login hisoblanadi - u ham shu yerdan o'zgaradi.
 */
export function ProfilePage() {
  const { t } = useTranslation();
  const { user, refresh } = useAuth();

  const [profile, setProfile] = useState({
    fullName: user?.fullName ?? '',
    email: user?.email ?? '',
    phone: user?.phone ?? '',
  });

  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' });

  const saveProfile = useMutation({
    mutationFn: () => usersApi.updateProfile(profile),
    // Yuqoridagi panel va menyu yangi ismni ko'rsatishi kerak.
    onSuccess: () => refresh(),
  });

  const savePassword = useMutation({
    mutationFn: () => usersApi.changePassword(passwords),
    onSuccess: () => setPasswords({ currentPassword: '', newPassword: '' }),
  });

  return (
    <AdminPage title={t('admin.profileTitle')} description={t('admin.profileHint')}>
      <Stack spacing={4} sx={{ maxWidth: 560 }}>
        <Box component="section">
          <Typography variant="subtitle2" sx={{ mb: 2 }}>
            {t('admin.profileSection')}
          </Typography>

          <Stack spacing={2}>
            {saveProfile.isSuccess && !saveProfile.isPending && (
              <Alert severity="success">{t('admin.profileSaved')}</Alert>
            )}
            <MutationError error={saveProfile.error} />

            <TextField
              label={t('admin.colName')}
              value={profile.fullName}
              onChange={(event) => setProfile({ ...profile, fullName: event.target.value })}
              required
              fullWidth
            />
            <TextField
              label={t('auth.fieldEmail')}
              type="email"
              value={profile.email}
              onChange={(event) => setProfile({ ...profile, email: event.target.value })}
              helperText={t('admin.emailIsLogin')}
              required
              fullWidth
            />
            <TextField
              label={t('staff.phone')}
              value={profile.phone}
              onChange={(event) => setProfile({ ...profile, phone: event.target.value })}
              placeholder="+998901234567"
              fullWidth
            />

            <Box>
              <Button
                variant="contained"
                disabled={saveProfile.isPending}
                onClick={() => saveProfile.mutate()}
              >
                {saveProfile.isPending ? t('common.saving') : t('common.save')}
              </Button>
            </Box>
          </Stack>
        </Box>

        <Divider />

        <Box component="section">
          <Typography variant="subtitle2" sx={{ mb: 2 }}>
            {t('admin.passwordSection')}
          </Typography>

          <Stack spacing={2}>
            {savePassword.isSuccess && !savePassword.isPending && (
              <Alert severity="success">{t('admin.passwordSaved')}</Alert>
            )}
            <MutationError error={savePassword.error} />

            <TextField
              label={t('admin.currentPassword')}
              type="password"
              autoComplete="current-password"
              value={passwords.currentPassword}
              onChange={(event) =>
                setPasswords({ ...passwords, currentPassword: event.target.value })
              }
              required
              fullWidth
            />
            <TextField
              label={t('admin.newPassword')}
              type="password"
              autoComplete="new-password"
              value={passwords.newPassword}
              onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })}
              helperText={t('admin.passwordHint')}
              required
              fullWidth
            />

            <Box>
              <Button
                variant="contained"
                disabled={
                  savePassword.isPending ||
                  passwords.currentPassword === '' ||
                  passwords.newPassword === ''
                }
                onClick={() => savePassword.mutate()}
              >
                {savePassword.isPending ? t('common.saving') : t('admin.changePassword')}
              </Button>
            </Box>
          </Stack>
        </Box>
      </Stack>
    </AdminPage>
  );
}
