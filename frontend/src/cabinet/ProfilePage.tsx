import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';

import { usersApi, type ChangePasswordPayload, type UpdateProfilePayload } from '../api/users';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBox, Field, Input, PageHeader } from '../components/ui';
import { errorMessage, fieldErrors } from '../lib/errors';

/**
 * Fuqaroning o'z hisobi sozlamalari.
 *
 * <p>Hisoblarni administrator ochadi va dastlabki parolni o'zi beradi.
 * Shu sabab egasida uni almashtirish imkoni bo'lishi shart: aks holda
 * administrator bilgan parol muddatsiz amal qilaveradi. Ilgari bu sahifa
 * faqat admin panelida bor edi, ya'ni fuqaro parolini umuman
 * o'zgartira olmasdi.
 *
 * <p>Email ayni paytda login hisoblanadi - u ham shu yerdan o'zgaradi.
 */

/** Saqlangandan keyingi qisqa tasdiq - xato qutisining yashil juftligi. */
function SuccessBox({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-emerald-200 dark:border-emerald-500/30 bg-emerald-50 dark:bg-emerald-500/15 p-4">
      <p className="text-sm text-emerald-800 dark:text-emerald-200">{message}</p>
    </div>
  );
}

// ------------------------------------------------------------- ma'lumotlar

function ProfileForm() {
  const { t } = useTranslation();
  const { user, refresh } = useAuth();
  const [saved, setSaved] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<UpdateProfilePayload>({
    defaultValues: {
      fullName: user?.fullName ?? '',
      email: user?.email ?? '',
      phone: user?.phone ?? '',
    },
  });

  async function onSubmit(values: UpdateProfilePayload) {
    setSubmitError(null);
    setSaved(false);
    try {
      await usersApi.updateProfile(values);
      // Yuqoridagi menyu va yon paneldagi ism yangilanishi kerak.
      await refresh();
      setSaved(true);
    } catch (error) {
      for (const [field, message] of Object.entries(fieldErrors(error))) {
        setError(field as keyof UpdateProfilePayload, { type: 'server', message });
      }
      setSubmitError(errorMessage(error, t));
    }
  }

  return (
    <Card>
      <h2 className="text-sm font-semibold tracking-wide text-slate-500 dark:text-slate-400 uppercase">
        {t('cabinet.profileSection')}
      </h2>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-5 space-y-5" noValidate>
        <Field label={t('cabinet.fieldName')} error={errors.fullName?.message} required>
          <Input autoComplete="name" {...register('fullName')} />
        </Field>

        <Field
          label={t('auth.fieldEmail')}
          hint={t('cabinet.emailIsLogin')}
          error={errors.email?.message}
          required
        >
          <Input type="email" autoComplete="email" {...register('email')} />
        </Field>

        <Field label={t('auth.fieldPhone')} error={errors.phone?.message}>
          <Input type="tel" autoComplete="tel" placeholder="+998901234567" {...register('phone')} />
        </Field>

        {submitError && <ErrorBox message={submitError} />}
        {saved && !submitError && <SuccessBox message={t('cabinet.profileSaved')} />}

        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? t('common.saving') : t('common.save')}
        </Button>
      </form>
    </Card>
  );
}

// ------------------------------------------------------------- parol

function PasswordForm() {
  const { t } = useTranslation();
  const [saved, setSaved] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordPayload>({
    defaultValues: { currentPassword: '', newPassword: '' },
  });

  async function onSubmit(values: ChangePasswordPayload) {
    setSubmitError(null);
    setSaved(false);
    try {
      await usersApi.changePassword(values);
      // Maydonlar tozalanadi: yozilgan parol ekranda qolmasligi kerak.
      reset({ currentPassword: '', newPassword: '' });
      setSaved(true);
    } catch (error) {
      for (const [field, message] of Object.entries(fieldErrors(error))) {
        setError(field as keyof ChangePasswordPayload, { type: 'server', message });
      }
      setSubmitError(errorMessage(error, t));
    }
  }

  return (
    <Card>
      <h2 className="text-sm font-semibold tracking-wide text-slate-500 dark:text-slate-400 uppercase">
        {t('cabinet.passwordSection')}
      </h2>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-5 space-y-5" noValidate>
        <Field label={t('cabinet.currentPassword')} error={errors.currentPassword?.message} required>
          <Input type="password" autoComplete="current-password" {...register('currentPassword')} />
        </Field>

        <Field
          label={t('cabinet.newPassword')}
          hint={t('auth.passwordHint')}
          error={errors.newPassword?.message}
          required
        >
          <Input type="password" autoComplete="new-password" {...register('newPassword')} />
        </Field>

        {submitError && <ErrorBox message={submitError} />}
        {saved && !submitError && <SuccessBox message={t('cabinet.passwordSaved')} />}

        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? t('common.saving') : t('cabinet.changePassword')}
        </Button>
      </form>
    </Card>
  );
}

export function CabinetProfilePage() {
  const { t } = useTranslation();

  return (
    <div className="max-w-2xl">
      <PageHeader title={t('cabinet.profileTitle')} description={t('cabinet.profileHint')} />
      <div className="space-y-6">
        <ProfileForm />
        <PasswordForm />
      </div>
    </div>
  );
}
