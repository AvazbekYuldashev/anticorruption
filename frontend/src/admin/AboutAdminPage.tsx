import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  IconButton,
  Paper,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { contentApi, type SaveAboutPayload } from '../api/content';
import { LANGUAGES } from '../i18n';
import { AdminPage, MutationError, QueryState } from './common';
import { RichTextField } from './RichTextField';
import { softShadow } from './theme';

/**
 * Muharrirdagi vazifa bandi.
 *
 * <p>Ro'yxat qayta tartiblanadi va o'chiriladi, shuning uchun bandning
 * matndan tashqari o'zgarmas kaliti ham kerak: indeks bo'yicha render
 * qilinsa, o'rtadagi band o'chirilganda React qatorlarni chalkashtiradi.
 */
interface TaskItem {
  key: string;
  text: string;
}

let keyCounter = 0;

function newTask(text = ''): TaskItem {
  keyCounter += 1;
  return { key: `task-${keyCounter}`, text };
}

interface AboutForm {
  title: string;
  body: string;
  tasksTitle: string;
  tasks: TaskItem[];
  goal: string;
}

/** Asosiy til: uning matni tarjima jadvalida emas, asosiy yozuvda turadi. */
const BASE_LANGUAGE = 'uz';

function toForm(
  title: string | null | undefined,
  body: string | null | undefined,
  tasksTitle: string | null | undefined,
  tasks: string[],
  goal: string | null | undefined,
): AboutForm {
  return {
    title: title ?? '',
    body: body ?? '',
    tasksTitle: tasksTitle ?? '',
    tasks: tasks.length > 0 ? tasks.map((task) => newTask(task)) : [newTask()],
    goal: goal ?? '',
  };
}

function serialize(form: AboutForm) {
  return {
    title: form.title,
    body: form.body,
    tasksTitle: form.tasksTitle,
    tasks: form.tasks.map((task) => task.text).filter((text) => text.trim() !== ''),
    goal: form.goal,
  };
}

/** Bo'sh varaq serverga yuborilmaydi - u yerda ham saqlanmaydi. */
function isEmpty(form: AboutForm): boolean {
  const value = serialize(form);
  return (
    value.title.trim() === '' &&
    value.body.trim() === '' &&
    value.tasksTitle.trim() === '' &&
    value.goal.trim() === '' &&
    value.tasks.length === 0
  );
}

/**
 * "Bo'lim haqida" sahifasini tahrirlaydi.
 *
 * <p>Sahifa saytda bitta, shuning uchun ro'yxat ham, dialog ham yo'q:
 * to'g'ridan-to'g'ri forma ochiladi va saqlanadi.
 */
export function AboutAdminPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const query = useQuery({ queryKey: ['admin', 'about'], queryFn: contentApi.adminAbout });

  /*
   * Har bir til uchun alohida forma. Asosiy til ("uz") javobning yuqori
   * qismidan, qolganlari `translations` ro'yxatidan to'ldiriladi.
   */
  const [forms, setForms] = useState<Record<string, AboutForm> | null>(null);
  const [active, setActive] = useState<string>(BASE_LANGUAGE);

  // Server javobi kelgach formalar bir marta to'ldiriladi; keyingi tahrirlar
  // foydalanuvchinikidir va ustidan yozilmasligi kerak.
  useEffect(() => {
    if (!query.data || forms !== null) return;
    const data = query.data;

    const next: Record<string, AboutForm> = {
      [BASE_LANGUAGE]: toForm(data.title, data.body, data.tasksTitle, data.tasks, data.goal),
    };

    for (const language of LANGUAGES) {
      if (language.code === BASE_LANGUAGE) continue;
      const translation = data.translations.find((item) => item.languageCode === language.code);
      next[language.code] = toForm(
        translation?.title,
        translation?.body,
        translation?.tasksTitle,
        translation?.tasks ?? [],
        translation?.goal,
      );
    }

    setForms(next);
  }, [query.data, forms]);

  const form = forms?.[active] ?? null;

  const save = useMutation({
    mutationFn: () => {
      const all = forms!;

      const payload: SaveAboutPayload = {
        ...serialize(all[BASE_LANGUAGE]),
        translations: LANGUAGES.filter((language) => language.code !== BASE_LANGUAGE)
          .filter((language) => !isEmpty(all[language.code]))
          .map((language) => ({
            languageCode: language.code,
            ...serialize(all[language.code]),
          })),
      };
      return contentApi.saveAbout(payload);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'about'] });
      void queryClient.invalidateQueries({ queryKey: ['about'] });
    },
  });

  function patch(changes: Partial<AboutForm>) {
    setForms((current) =>
      current === null
        ? current
        : { ...current, [active]: { ...current[active], ...changes } },
    );
  }

  function updateTask(index: number, text: string) {
    patch({ tasks: form!.tasks.map((task, i) => (i === index ? { ...task, text } : task)) });
  }

  function moveTask(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= form!.tasks.length) return;
    const next = [...form!.tasks];
    [next[index], next[target]] = [next[target], next[index]];
    patch({ tasks: next });
  }

  return (
    <AdminPage title={t('admin.aboutTitle')} description={t('admin.aboutHint')}>
      <QueryState isPending={query.isPending} error={query.error}>
        {form && (
          // Forma kartochka ichida: ilgari maydonlar fon ustida yolg'iz turardi.
          <Stack
            spacing={2.5}
            sx={{
              maxWidth: 900,
              p: { xs: 2, md: 3 },
              borderRadius: 4,
              bgcolor: 'background.paper',
              border: '1px solid rgba(148, 163, 184, 0.22)',
              boxShadow: softShadow,
            }}
          >
            {save.isSuccess && !save.isPending && (
              <Alert severity="success">{t('admin.aboutSaved')}</Alert>
            )}
            <MutationError error={save.error} />

            {/*
              Til varaqalari. Barcha tillar bitta "Saqlash" bilan yuboriladi -
              muharrir varaqlar orasida yurib, oxirida bir marta saqlaydi.
            */}
            <Box>
              <Tabs
                value={active}
                onChange={(_, value: string) => setActive(value)}
                variant="scrollable"
                scrollButtons="auto"
              >
                {LANGUAGES.map((language) => (
                  <Tab key={language.code} value={language.code} label={language.name} />
                ))}
              </Tabs>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                {active === BASE_LANGUAGE
                  ? t('admin.baseLanguageHint')
                  : t('admin.translationFallbackHint')}
              </Typography>
            </Box>

            <TextField
              label={t('admin.aboutFieldTitle')}
              value={form.title}
              onChange={(event) => patch({ title: event.target.value })}
              fullWidth
            />

            <RichTextField
              label={t('admin.aboutFieldBody')}
              value={form.body}
              onChange={(body) => patch({ body })}
              helperText={t('admin.aboutBodyHint')}
              minRows={8}
            />

            <Box>
              <TextField
                label={t('admin.aboutFieldTasksTitle')}
                value={form.tasksTitle}
                onChange={(event) => patch({ tasksTitle: event.target.value })}
                placeholder={t('admin.aboutTasksPlaceholder')}
                fullWidth
              />

              <Typography variant="subtitle2" sx={{ mt: 2.5, mb: 1 }}>
                {t('admin.aboutTasks')}
              </Typography>

              <Stack spacing={1.5}>
                {form.tasks.map((task, index) => (
                  <Paper key={task.key} variant="outlined" sx={{ p: 1.5 }}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <TextField
                        size="small"
                        value={task.text}
                        onChange={(event) => updateTask(index, event.target.value)}
                        placeholder={t('admin.aboutTaskPlaceholder')}
                        fullWidth
                      />
                      <IconButton
                        size="small"
                        disabled={index === 0}
                        onClick={() => moveTask(index, -1)}
                        aria-label={t('admin.moveUp')}
                      >
                        <Typography variant="caption">↑</Typography>
                      </IconButton>
                      <IconButton
                        size="small"
                        disabled={index === form.tasks.length - 1}
                        onClick={() => moveTask(index, 1)}
                        aria-label={t('admin.moveDown')}
                      >
                        <Typography variant="caption">↓</Typography>
                      </IconButton>
                      <IconButton
                        size="small"
                        color="error"
                        disabled={form.tasks.length <= 1}
                        onClick={() => patch({ tasks: form.tasks.filter((_, i) => i !== index) })}
                        aria-label={t('admin.removeTask')}
                      >
                        <Typography variant="caption">×</Typography>
                      </IconButton>
                    </Stack>
                  </Paper>
                ))}
              </Stack>

              <Button
                size="small"
                sx={{ mt: 1.5 }}
                disabled={form.tasks.length >= 50}
                onClick={() => patch({ tasks: [...form.tasks, newTask()] })}
              >
                + {t('admin.addTask')}
              </Button>
            </Box>

            <RichTextField
              label={t('admin.aboutFieldGoal')}
              value={form.goal}
              onChange={(goal) => patch({ goal })}
              helperText={t('admin.aboutGoalHint')}
              minRows={3}
            />

            <Box>
              <Button variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>
                {save.isPending ? t('common.saving') : t('common.save')}
              </Button>
            </Box>
          </Stack>
        )}
      </QueryState>
    </AdminPage>
  );
}
