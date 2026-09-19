import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBox, Field, Input } from '../components/ui';
import { errorMessage, fieldErrors } from '../lib/errors';

interface LocationState {
  from?: string;
}

// ---------------------------------------------------------------- kirish

interface LoginValues {
  email: string;
  password: string;
}

export function LoginPage() {
  const { t } = useTranslation();
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ defaultValues: { email: '', password: '' } });

  async function onSubmit(values: LoginValues) {
    setSubmitError(null);
    try {
      const user = await login(values);
      // Xodimni to'g'ridan-to'g'ri admin panelga, qolganlarni
      // ular kelgan sahifaga qaytaramiz.
      const from = (location.state as LocationState | null)?.from;
      const isStaff = user.role === 'MODERATOR' || user.role === 'ADMIN';
      navigate(from ?? (isStaff ? '/admin' : '/my'), { replace: true });
    } catch (error) {
      for (const [field, message] of Object.entries(fieldErrors(error))) {
        setError(field as keyof LoginValues, { type: 'server', message });
      }
      setSubmitError(errorMessage(error, t));
    }
  }

  return (
    <div className="mx-auto max-w-md py-8">
      <Card>
        <h1 className="text-xl font-semibold text-slate-900 dark:text-slate-100">{t('auth.loginTitle')}</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-5" noValidate>
          <Field label={t('auth.fieldEmail')} error={errors.email?.message} required>
            <Input type="email" autoComplete="email" {...register('email')} />
          </Field>

          <Field label={t('auth.fieldPassword')} error={errors.password?.message} required>
            <Input type="password" autoComplete="current-password" {...register('password')} />
          </Field>

          {submitError && <ErrorBox message={submitError} />}

          <Button type="submit" fullWidth disabled={isSubmitting}>
            {isSubmitting ? t('auth.loginBusy') : t('auth.loginSubmit')}
          </Button>
        </form>

        {/*
          Ochiq ro'yxatdan o'tish yo'q: hisoblarni administrator ochadi.
          Murojaat yuborish uchun esa hisob umuman kerak emas.
        */}
        <p className="mt-6 text-center text-xs text-slate-500 dark:text-slate-400">{t('auth.noSelfSignUp')}</p>
      </Card>
    </div>
  );
}
