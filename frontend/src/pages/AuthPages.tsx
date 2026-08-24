import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
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
        <h1 className="text-xl font-semibold text-slate-900">{t('auth.loginTitle')}</h1>

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

        <p className="mt-6 text-center text-sm text-slate-600">
          {t('auth.noAccount')}{' '}
          <Link to="/signup" className="font-medium text-brand-600 hover:underline">
            {t('auth.registerSubmit')}
          </Link>
        </p>
      </Card>
    </div>
  );
}

// ---------------------------------------------------------------- ro'yxatdan o'tish

interface SignUpValues {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export function SignUpPage() {
  const { t } = useTranslation();
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignUpValues>({
    defaultValues: { fullName: '', email: '', phone: '', password: '' },
  });

  async function onSubmit(values: SignUpValues) {
    setSubmitError(null);
    try {
      await registerUser({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim() || undefined,
        password: values.password,
      });
      navigate('/my', { replace: true });
    } catch (error) {
      for (const [field, message] of Object.entries(fieldErrors(error))) {
        setError(field as keyof SignUpValues, { type: 'server', message });
      }
      setSubmitError(errorMessage(error, t));
    }
  }

  return (
    <div className="mx-auto max-w-md py-8">
      <Card>
        <h1 className="text-xl font-semibold text-slate-900">{t('auth.registerTitle')}</h1>
        <p className="mt-2 text-sm text-slate-600">{t('auth.registerHint')}</p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-5" noValidate>
          <Field label={t('auth.fieldFullName')} error={errors.fullName?.message} required>
            <Input autoComplete="name" {...register('fullName')} />
          </Field>

          <Field label={t('auth.fieldEmail')} error={errors.email?.message} required>
            <Input type="email" autoComplete="email" {...register('email')} />
          </Field>

          <Field label={t('auth.fieldPhone')} error={errors.phone?.message}>
            <Input autoComplete="tel" placeholder="+998901234567" {...register('phone')} />
          </Field>

          <Field
            label={t('auth.fieldPassword')}
            hint={t('auth.passwordHint')}
            error={errors.password?.message}
            required
          >
            <Input type="password" autoComplete="new-password" {...register('password')} />
          </Field>

          {submitError && <ErrorBox message={submitError} />}

          <Button type="submit" fullWidth disabled={isSubmitting}>
            {isSubmitting ? t('auth.registerBusy') : t('auth.registerSubmit')}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          {t('auth.hasAccount')}{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            {t('auth.loginSubmit')}
          </Link>
        </p>
      </Card>
    </div>
  );
}
