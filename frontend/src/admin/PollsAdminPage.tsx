import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  FormControlLabel,
  IconButton,
  LinearProgress,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { pollsApi, type SavePollPayload } from '../api/polls';
import type { PollResponse } from '../api/types';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

const EMPTY: SavePollPayload = {
  question: '',
  description: '',
  multipleChoice: false,
  active: true,
  options: [{ text: '' }, { text: '' }],
};

export function PollsAdminPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [dialog, setDialog] = useState<{ id: number | null; form: SavePollPayload } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; question: string } | null>(null);

  const query = useQuery({ queryKey: ['admin', 'polls'], queryFn: pollsApi.all });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'polls'] });
    void queryClient.invalidateQueries({ queryKey: ['polls'] });
  }

  const save = useMutation({
    mutationFn: () => {
      const { id, form } = dialog!;
      const payload: SavePollPayload = {
        ...form,
        // Bo'sh variantlar yuborilmaydi.
        options: form.options.filter((option) => option.text.trim() !== ''),
      };
      return id === null ? pollsApi.create(payload) : pollsApi.update(id, payload);
    },
    onSuccess: () => {
      setDialog(null);
      refresh();
    },
  });

  const setActive = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => pollsApi.setActive(id, active),
    onSuccess: refresh,
  });

  const remove = useMutation({
    mutationFn: () => pollsApi.remove(deleting!.id),
    onSuccess: () => {
      setDeleting(null);
      refresh();
    },
  });

  return (
    <AdminPage
      title={t('admin.pollsTitle')}
      action={
        <Button variant="contained" onClick={() => setDialog({ id: null, form: EMPTY })}>
          {t('admin.addPoll')}
        </Button>
      }
    >
      <MutationError error={setActive.error ?? remove.error} />

      <QueryState isPending={query.isPending} error={query.error}>
        <Stack spacing={2} sx={{ mt: 2 }}>
          {query.data?.map((poll) => (
            <PollRow
              key={poll.id}
              poll={poll}
              busy={setActive.isPending}
              onToggleActive={(active) => setActive.mutate({ id: poll.id, active })}
              onEdit={() =>
                setDialog({
                  id: poll.id,
                  form: {
                    question: poll.question,
                    description: poll.description ?? '',
                    multipleChoice: poll.multipleChoice,
                    active: poll.active,
                    startsAt: poll.startsAt,
                    endsAt: poll.endsAt,
                    // Mavjud variantlarning id si saqlanadi - aks holda
                    // ular yangi variant deb qaraladi va ovozlari yo'qoladi.
                    options: poll.options.map((option) => ({ id: option.id, text: option.text })),
                  },
                })
              }
              onDelete={() => setDeleting({ id: poll.id, question: poll.question })}
            />
          ))}

          {query.data?.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
              {t('common.noData')}
            </Typography>
          )}
        </Stack>
      </QueryState>

      {dialog && (
        <FormDialog
          open
          maxWidth="md"
          title={dialog.id === null ? t('admin.addPoll') : t('admin.editPoll')}
          busy={save.isPending}
          error={save.error}
          onClose={() => setDialog(null)}
          onSubmit={() => save.mutate()}
        >
          <TextField
            label={t('admin.fieldQuestion')}
            value={dialog.form.question}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, question: event.target.value } })
            }
            required
            fullWidth
          />
          <TextField
            label={t('admin.fieldDescription')}
            value={dialog.form.description}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, description: event.target.value } })
            }
            multiline
            rows={2}
            fullWidth
          />

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              {t('admin.fieldOptions')}
            </Typography>
            {dialog.id !== null && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                {t('admin.editOptionsWarning')}
              </Alert>
            )}

            <Stack spacing={1.5}>
              {dialog.form.options.map((option, index) => (
                <Stack key={index} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <TextField
                    size="small"
                    value={option.text}
                    onChange={(event) => {
                      const options = [...dialog.form.options];
                      options[index] = { ...options[index], text: event.target.value };
                      setDialog({ ...dialog, form: { ...dialog.form, options } });
                    }}
                    fullWidth
                  />
                  <IconButton
                    size="small"
                    color="error"
                    disabled={dialog.form.options.length <= 2}
                    onClick={() =>
                      setDialog({
                        ...dialog,
                        form: {
                          ...dialog.form,
                          options: dialog.form.options.filter((_, i) => i !== index),
                        },
                      })
                    }
                  >
                    <Typography variant="caption">✕</Typography>
                  </IconButton>
                </Stack>
              ))}
            </Stack>

            <Button
              size="small"
              sx={{ mt: 1.5 }}
              disabled={dialog.form.options.length >= 20}
              onClick={() =>
                setDialog({
                  ...dialog,
                  form: { ...dialog.form, options: [...dialog.form.options, { text: '' }] },
                })
              }
            >
              + {t('admin.addOption')}
            </Button>
          </Box>

          <Stack direction="row" spacing={3} sx={{ flexWrap: 'wrap' }}>
            <FormControlLabel
              control={
                <Switch
                  checked={dialog.form.multipleChoice ?? false}
                  onChange={(event) =>
                    setDialog({
                      ...dialog,
                      form: { ...dialog.form, multipleChoice: event.target.checked },
                    })
                  }
                />
              }
              label={t('admin.multipleChoice')}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={dialog.form.active ?? true}
                  onChange={(event) =>
                    setDialog({ ...dialog, form: { ...dialog.form, active: event.target.checked } })
                  }
                />
              }
              label={t('admin.pollActive')}
            />
          </Stack>
        </FormDialog>
      )}

      <ConfirmDialog
        open={deleting !== null}
        title={deleting?.question ?? ''}
        busy={remove.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => remove.mutate()}
      />
    </AdminPage>
  );
}

function PollRow({
  poll,
  busy,
  onToggleActive,
  onEdit,
  onDelete,
}: {
  poll: PollResponse;
  busy: boolean;
  onToggleActive: (active: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { t } = useTranslation();

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="subtitle1">{poll.question}</Typography>
            {poll.description && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {poll.description}
              </Typography>
            )}
            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', mt: 1 }}>
              <Chip
                size="small"
                label={poll.active ? t('common.active') : t('common.inactive')}
                color={poll.active ? 'success' : 'default'}
              />
              {poll.multipleChoice && <Chip size="small" label={t('admin.multipleChoice')} />}
              <Chip
                size="small"
                variant="outlined"
                label={t('polls.voters', { count: poll.voterCount })}
              />
            </Stack>
          </Box>

          <Stack direction="row" spacing={0.5} sx={{ flexShrink: 0 }}>
            <Button size="small" onClick={onEdit}>
              {t('common.edit')}
            </Button>
            <Button size="small" disabled={busy} onClick={() => onToggleActive(!poll.active)}>
              {poll.active ? t('admin.deactivate') : t('admin.activate')}
            </Button>
            <Button size="small" color="error" onClick={onDelete}>
              {t('common.delete')}
            </Button>
          </Stack>
        </Stack>

        <Stack spacing={1.5} sx={{ mt: 2 }}>
          {poll.options.map((option) => (
            <Box key={option.id}>
              <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
                <Typography variant="body2">{option.text}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {option.voteCount} · {option.percentage}%
                </Typography>
              </Stack>
              <LinearProgress
                variant="determinate"
                value={option.percentage}
                sx={{ height: 6, borderRadius: 3 }}
              />
            </Box>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}
