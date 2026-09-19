import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery } from '@tanstack/react-query';
import { complaintsApi } from '../api/complaints';
import { referenceApi } from '../api/reference';
import type { ComplaintCreatedResponse, CreateComplaintRequest } from '../api/types';
import {
  Button,
  Card,
  Checkbox,
  ErrorBox,
  Field,
  Input,
  PageHeader,
  Section,
  Select,
  Spinner,
  Textarea,
} from '../components/ui';
import { errorMessage, fieldErrors } from '../lib/errors';

type FormValues = {
  title: string;
  description: string;
  category: string;
  reporterType: string;
  facultyId: string;
  departmentId: string;
  subjectName: string;
  accusedPosition: string;
  incidentDate: string;
  incidentPlace: string;
  courseYear: string;
  groupName: string;
  studyForm: string;
  anonymous: boolean;
  reporterName: string;
  reporterEmail: string;
  reporterPhone: string;
};

const EMPTY_FORM: FormValues = {
  title: '',
  description: '',
  category: '',
  reporterType: '',
  facultyId: '',
  departmentId: '',
  subjectName: '',
  accusedPosition: '',
  incidentDate: '',
  incidentPlace: '',
  courseYear: '',
  groupName: '',
  studyForm: '',
  anonymous: false,
  reporterName: '',
  reporterEmail: '',
  reporterPhone: '',
};

/** Bo'sh satrni null ga aylantiradi - backend bo'sh satrni qiymat deb qabul qilmasin. */
function nullable(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}

const STUDENT_TYPES = new Set(['STUDENT', 'MASTER_STUDENT']);

export function SubmitComplaintPage() {
  const { t } = useTranslation();
  const [created, setCreated] = useState<ComplaintCreatedResponse | null>(null);

  const reference = useQuery({ queryKey: ['reference'], queryFn: referenceApi.all });

  const {
    register,
    handleSubmit,
    watch,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({ defaultValues: EMPTY_FORM });

  const anonymous = watch('anonymous');
  const reporterType = watch('reporterType');
  const facultyId = watch('facultyId');
  const description = watch('description');
  const isStudent = STUDENT_TYPES.has(reporterType);

  // Tanlangan fakultetning kafedralari. Fakultet tanlanmaguncha ro'yxat bo'sh.
  const departments =
    reference.data?.faculties.find((faculty) => String(faculty.id) === facultyId)?.departments ?? [];

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload: CreateComplaintRequest = {
        title: values.title.trim(),
        description: values.description.trim(),
        category: values.category as CreateComplaintRequest['category'],
        reporterType: values.reporterType as CreateComplaintRequest['reporterType'],
        facultyId: values.facultyId ? Number(values.facultyId) : null,
        departmentId: values.departmentId ? Number(values.departmentId) : null,
        subjectName: nullable(values.subjectName),
        accusedPosition: (nullable(values.accusedPosition) ??
          null) as CreateComplaintRequest['accusedPosition'],
        incidentDate: nullable(values.incidentDate),
        incidentPlace: nullable(values.incidentPlace),
        anonymous: values.anonymous,
        // Talabaga tegishli maydonlar boshqa maqomlarda yuborilmaydi -
        // backend ham ularni e'tiborsiz qoldiradi, lekin so'rovni ham toza tutamiz.
        courseYear: isStudent && values.courseYear ? Number(values.courseYear) : null,
        groupName: isStudent ? nullable(values.groupName) : null,
        studyForm: isStudent
          ? ((nullable(values.studyForm) ?? null) as CreateComplaintRequest['studyForm'])
          : null,
        // Anonim murojaatda ism va telefon umuman yuborilmaydi.
        reporterName: values.anonymous ? null : nullable(values.reporterName),
        reporterEmail: nullable(values.reporterEmail),
        reporterPhone: values.anonymous ? null : nullable(values.reporterPhone),
      };
      return complaintsApi.create(payload);
    },
    onSuccess: (response) => {
      setCreated(response);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onError: (error) => {
      /*
       * Validatsiya qoidalari faqat backendda yozilgan - ularni bu yerda
       * takrorlamaymiz. Backend maydon xatoliklarini joriy tilda qaytaradi,
       * biz esa ularni shaklga joylashtiramiz. Shu tufayli qoida o'zgarsa
       * ikkita joyni tuzatish shart bo'lmaydi.
       */
      for (const [field, message] of Object.entries(fieldErrors(error))) {
        if (field in EMPTY_FORM) {
          setError(field as keyof FormValues, { type: 'server', message });
        }
      }
    },
  });

  if (reference.isLoading) return <Spinner />;
  if (reference.isError) {
    return <ErrorBox message={errorMessage(reference.error, t)} onRetry={() => reference.refetch()} />;
  }

  if (created) {
    return (
      <SuccessPanel
        created={created}
        onNewComplaint={() => {
          setCreated(null);
          reset(EMPTY_FORM);
        }}
      />
    );
  }

  const options = reference.data!;

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={t('submit.title')} description={t('submit.intro')} />

      <Card>
        <form
          onSubmit={handleSubmit((values) => mutation.mutate(values))}
          className="space-y-8"
          noValidate
        >
          {/* --- Nima yuz berdi --- */}
          <div className="space-y-5">
            <Field
              label={t('submit.fieldTitle')}
              hint={t('submit.fieldTitleHint')}
              error={errors.title?.message}
              required
            >
              <Input {...register('title')} maxLength={200} />
            </Field>

            <Field
              label={t('submit.fieldDescription')}
              hint={t('submit.fieldDescriptionHint')}
              error={errors.description?.message}
              required
            >
              <Textarea {...register('description')} rows={7} maxLength={10000} />
              <span className="mt-1 block text-right text-xs text-slate-400 dark:text-slate-500">
                {description.trim().length} / 10000
              </span>
            </Field>

            <Field
              label={t('submit.fieldCategory')}
              error={errors.category?.message}
              required
            >
              <Select
                {...register('category')}
                placeholder={t('common.select')}
                options={options.categories}
              />
            </Field>
          </div>

          {/* --- Qayerda yuz berdi --- */}
          <Section title={t('submit.sectionWhere')}>
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label={t('submit.fieldFaculty')} hint={t('submit.facultyHint')}>
                <Select
                  {...register('facultyId')}
                  placeholder={t('common.select')}
                  options={options.faculties.map((faculty) => ({
                    value: String(faculty.id),
                    label: faculty.name,
                  }))}
                />
              </Field>

              <Field label={t('submit.fieldDepartment')}>
                <Select
                  {...register('departmentId')}
                  placeholder={t('common.select')}
                  disabled={departments.length === 0}
                  options={departments.map((department) => ({
                    value: String(department.id),
                    label: department.name,
                  }))}
                />
              </Field>

              <Field label={t('submit.fieldSubject')} error={errors.subjectName?.message}>
                <Input {...register('subjectName')} maxLength={200} />
              </Field>

              <Field label={t('submit.fieldPosition')}>
                <Select
                  {...register('accusedPosition')}
                  placeholder={t('common.select')}
                  options={options.positions}
                />
              </Field>

              <Field label={t('submit.fieldIncidentDate')} error={errors.incidentDate?.message}>
                <Input type="date" {...register('incidentDate')} max={new Date().toISOString().slice(0, 10)} />
              </Field>

              <Field
                label={t('submit.fieldIncidentPlace')}
                hint={t('submit.fieldIncidentPlaceHint')}
                error={errors.incidentPlace?.message}
              >
                <Input {...register('incidentPlace')} maxLength={250} />
              </Field>
            </div>
          </Section>

          {/* --- Siz kimsiz --- */}
          <Section title={t('submit.sectionWho')}>
            <Field
              label={t('submit.fieldReporterType')}
              error={errors.reporterType?.message}
              required
            >
              <Select
                {...register('reporterType')}
                placeholder={t('common.select')}
                options={options.reporterTypes}
              />
            </Field>

            {isStudent && (
              <div className="rounded-lg bg-slate-50 dark:bg-slate-800/60 p-4">
                <p className="mb-4 text-xs text-slate-500 dark:text-slate-400">{t('submit.studentOnly')}</p>
                <div className="grid gap-5 sm:grid-cols-3">
                  <Field label={t('submit.fieldCourseYear')} error={errors.courseYear?.message}>
                    <Input type="number" min={1} max={7} {...register('courseYear')} />
                  </Field>
                  <Field label={t('submit.fieldGroupName')} error={errors.groupName?.message}>
                    <Input {...register('groupName')} maxLength={50} />
                  </Field>
                  <Field label={t('submit.fieldStudyForm')}>
                    <Select
                      {...register('studyForm')}
                      placeholder={t('common.select')}
                      options={options.studyForms}
                    />
                  </Field>
                </div>
              </div>
            )}
          </Section>

          {/* --- Aloqa --- */}
          <Section title={t('submit.sectionContact')}>
            <Checkbox
              {...register('anonymous')}
              label={t('submit.fieldAnonymous')}
              hint={t('submit.anonymousHint')}
            />

            <div className="grid gap-5 sm:grid-cols-2">
              {/* Anonim murojaatda ism va telefon so'ralmaydi - ular saqlanmaydi ham. */}
              {!anonymous && (
                <>
                  <Field label={t('submit.fieldReporterName')} error={errors.reporterName?.message}>
                    <Input {...register('reporterName')} maxLength={150} />
                  </Field>
                  <Field label={t('submit.fieldReporterPhone')} error={errors.reporterPhone?.message}>
                    <Input {...register('reporterPhone')} maxLength={30} placeholder="+998901234567" />
                  </Field>
                </>
              )}

              <Field
                label={t('submit.fieldReporterEmail')}
                hint={t('submit.emailHint')}
                error={errors.reporterEmail?.message}
              >
                <Input type="email" {...register('reporterEmail')} maxLength={180} />
              </Field>
            </div>
          </Section>

          {mutation.isError && (
            <ErrorBox message={errorMessage(mutation.error, t)} />
          )}

          <Button type="submit" disabled={mutation.isPending} fullWidth>
            {mutation.isPending ? t('submit.submitting') : t('submit.submitButton')}
          </Button>
        </form>
      </Card>
    </div>
  );
}

// ---------------------------------------------------------------- muvaffaqiyat

function SuccessPanel({
  created,
  onNewComplaint,
}: {
  created: ComplaintCreatedResponse;
  onNewComplaint: () => void;
}) {
  const { t } = useTranslation();
  const [copied, setCopied] = useState(false);

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(created.trackingCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* clipboard ruxsat bermasa kod baribir ekranda ko'rinib turibdi */
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <Card className="border-emerald-200 dark:border-emerald-500/30 bg-emerald-50 dark:bg-emerald-500/15">
        <div className="flex items-start gap-3">
          <span
            aria-hidden
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-600 text-white"
          >
            ✓
          </span>
          <div>
            <h1 className="text-lg font-semibold text-emerald-900 dark:text-emerald-100">{t('submit.successTitle')}</h1>
            <p className="mt-1 text-sm text-emerald-800 dark:text-emerald-200">{created.message}</p>
          </div>
        </div>
      </Card>

      <Card>
        <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('submit.successCode')}</p>
        <div className="mt-2 flex flex-wrap items-center gap-3">
          <code className="rounded-lg bg-slate-100 dark:bg-slate-800 px-4 py-2 font-mono text-xl font-semibold tracking-wider text-slate-900 dark:text-slate-100">
            {created.trackingCode}
          </code>
          <Button variant="outline" onClick={() => void copyCode()}>
            {copied ? t('common.copied') : t('common.copy')}
          </Button>
        </div>
        <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">{t('submit.successKeep')}</p>
      </Card>

      <AttachmentUploader trackingCode={created.trackingCode} />

      <div className="flex flex-wrap gap-3">
        <Link to={`/track?code=${encodeURIComponent(created.trackingCode)}`}>
          <Button>{t('submit.successTrack')}</Button>
        </Link>
        <Button variant="outline" onClick={onNewComplaint}>
          {t('submit.successNew')}
        </Button>
      </div>
    </div>
  );
}

/** Murojaat yuborilgandan keyin dalil biriktirish. Kuzatuv kodi egalik isboti. */
function AttachmentUploader({ trackingCode }: { trackingCode: string }) {
  const { t } = useTranslation();
  const [uploaded, setUploaded] = useState<string[]>([]);

  const mutation = useMutation({
    mutationFn: (file: File) => complaintsApi.attach(trackingCode, file),
    onSuccess: (attachment) => setUploaded((names) => [...names, attachment.originalName]),
  });

  return (
    <Card>
      <h2 className="font-medium text-slate-900 dark:text-slate-100">{t('submit.attachTitle')}</h2>
      <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{t('submit.attachHint')}</p>

      <label className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-2.5 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800/60">
        <input
          type="file"
          className="hidden"
          accept="image/jpeg,image/png,image/webp,application/pdf,text/plain"
          disabled={mutation.isPending}
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) mutation.mutate(file);
            // Bir xil faylni qayta tanlash mumkin bo'lishi uchun tozalaymiz.
            event.target.value = '';
          }}
        />
        {mutation.isPending ? t('submit.attaching') : t('submit.attachButton')}
      </label>

      {uploaded.length > 0 && (
        <ul className="mt-4 space-y-1.5">
          {uploaded.map((name) => (
            <li key={name} className="flex items-center gap-2 text-sm text-emerald-700 dark:text-emerald-300">
              <span aria-hidden>✓</span>
              {name}
            </li>
          ))}
        </ul>
      )}

      {mutation.isError && (
        <div className="mt-4">
          <ErrorBox message={errorMessage(mutation.error, t)} />
        </div>
      )}
    </Card>
  );
}
