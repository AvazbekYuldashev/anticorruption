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
  TextField,
  Typography,
} from '@mui/material';
import { contentApi, type SaveAboutPayload } from '../api/content';
import { AdminPage, MutationError, QueryState } from './common';

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
  const [form, setForm] = useState<AboutForm | null>(null);

  // Server javobi kelgach forma bir marta to'ldiriladi; keyingi tahrirlar
  // foydalanuvchinikidir va ustidan yozilmasligi kerak.
  useEffect(() => {
    if (query.data && form === null) {
      setForm({
        title: query.data.title ?? '',
        body: query.data.body ?? '',
        tasksTitle: query.data.tasksTitle ?? '',
        tasks: query.data.tasks.length > 0 ? query.data.tasks.map((task) => newTask(task)) : [newTask()],
        goal: query.data.goal ?? '',
      });
    }
  }, [query.data, form]);

  const save = useMutation({
    mutationFn: () => {
      const payload: SaveAboutPayload = {
        title: form!.title,
        body: form!.body,
        tasksTitle: form!.tasksTitle,
        tasks: form!.tasks.map((task) => task.text),
        goal: form!.goal,
      };
      return contentApi.saveAbout(payload);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'about'] });
      void queryClient.invalidateQueries({ queryKey: ['about'] });
    },
  });

  function patch(changes: Partial<AboutForm>) {
    setForm((current) => (current === null ? current : { ...current, ...changes }));
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
          <Stack spacing={2.5} sx={{ maxWidth: 900 }}>
            {save.isSuccess && !save.isPending && (
              <Alert severity="success">{t('admin.aboutSaved')}</Alert>
            )}
            <MutationError error={save.error} />

            <TextField
              label={t('admin.aboutFieldTitle')}
              value={form.title}
              onChange={(event) => patch({ title: event.target.value })}
              fullWidth
            />

            <TextField
              label={t('admin.aboutFieldBody')}
              value={form.body}
              onChange={(event) => patch({ body: event.target.value })}
              helperText={t('admin.aboutBodyHint')}
              multiline
              minRows={8}
              fullWidth
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

            <TextField
              label={t('admin.aboutFieldGoal')}
              value={form.goal}
              onChange={(event) => patch({ goal: event.target.value })}
              helperText={t('admin.aboutGoalHint')}
              multiline
              minRows={3}
              fullWidth
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
