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
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  LinearProgress,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { pollsApi, type SavePollPayload } from '../api/polls';
import type {
  PollQuestionResponse,
  PollResponse,
  PollType,
  QuizStatisticsResponse,
} from '../api/types';
import { formatDateTime } from '../lib/format';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';
import {
  PollQuestionsEditor,
  emptyQuestion,
  toEditorQuestions,
  toSaveQuestions,
  type EditorQuestion,
} from './PollQuestionsEditor';

interface PollForm {
  title: string;
  description: string;
  active: boolean;
  startsAt: string;
  endsAt: string;
  questions: EditorQuestion[];
}

function emptyForm(): PollForm {
  return {
    title: '',
    description: '',
    active: true,
    startsAt: '',
    endsAt: '',
    questions: [emptyQuestion()],
  };
}

/**
 * ISO vaqtni `datetime-local` maydoni kutadigan ko'rinishga o'giradi.
 *
 * <p>Maydon mahalliy vaqt bilan ishlaydi, server esa UTC saqlaydi -
 * shuning uchun o'girish ikki tomonlama qilinadi.
 */
function toLocalInput(iso: string | null): string {
  if (!iso) return '';
  const date = new Date(iso);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function fromLocalInput(value: string): string | null {
  return value ? new Date(value).toISOString() : null;
}

/** So'rovnomalar bo'limi. */
export function PollsAdminPage() {
  return <PollsAdminPageFor type="SURVEY" />;
}

/** Testlar (viktorinalar) bo'limi - o'sha sahifa, faqat turi boshqa. */
export function QuizzesAdminPage() {
  return <PollsAdminPageFor type="QUIZ" />;
}

/**
 * So'rovnoma va test bir xil boshqariladi: savollar, variantlar, muddat,
 * to'xtatish va qayta o'tkazish. Yagona farqi - testda har bir variantda
 * "to'g'ri javob" belgisi bo'ladi va ro'yxatlar turiga qarab ajratiladi.
 */
function PollsAdminPageFor({ type }: { type: PollType }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const quiz = type === 'QUIZ';

  const [dialog, setDialog] = useState<{ id: number | null; form: PollForm } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; title: string } | null>(null);
  const [statisticsFor, setStatisticsFor] = useState<number | null>(null);
  const [restarting, setRestarting] = useState<
    { id: number; title: string; startsAt: string; endsAt: string } | null
  >(null);

  const query = useQuery({ queryKey: ['admin', 'polls', type], queryFn: () => pollsApi.all(type) });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'polls'] });
    void queryClient.invalidateQueries({ queryKey: ['polls'] });
  }

  const save = useMutation({
    mutationFn: () => {
      const { id, form } = dialog!;
      const payload: SavePollPayload = {
        title: form.title,
        description: form.description,
        type,
        active: form.active,
        startsAt: fromLocalInput(form.startsAt),
        endsAt: fromLocalInput(form.endsAt),
        questions: toSaveQuestions(form.questions),
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

  const setStopped = useMutation({
    mutationFn: ({ id, stopped }: { id: number; stopped: boolean }) =>
      pollsApi.setStopped(id, stopped),
    onSuccess: refresh,
  });

  const restart = useMutation({
    mutationFn: () =>
      pollsApi.restart(restarting!.id, {
        startsAt: fromLocalInput(restarting!.startsAt),
        endsAt: fromLocalInput(restarting!.endsAt),
      }),
    onSuccess: () => {
      setRestarting(null);
      refresh();
    },
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
      title={t(quiz ? 'admin.testsTitle' : 'admin.pollsTitle')}
      action={
        <Button variant="contained" onClick={() => setDialog({ id: null, form: emptyForm() })}>
          {t(quiz ? 'admin.addTest' : 'admin.addPoll')}
        </Button>
      }
    >
      <MutationError error={setActive.error ?? setStopped.error ?? remove.error} />

      <QueryState isPending={query.isPending} error={query.error}>
        <Stack spacing={2} sx={{ mt: 2 }}>
          {query.data?.map((poll) => (
            <PollRow
              key={poll.id}
              poll={poll}
              busy={setActive.isPending || setStopped.isPending}
              onToggleActive={(active) => setActive.mutate({ id: poll.id, active })}
              onToggleStopped={(stopped) => setStopped.mutate({ id: poll.id, stopped })}
              onRestart={() =>
                setRestarting({ id: poll.id, title: poll.title, startsAt: '', endsAt: '' })
              }
              onStatistics={() => setStatisticsFor(poll.id)}
              onEdit={() =>
                setDialog({
                  id: poll.id,
                  form: {
                    title: poll.title,
                    description: poll.description ?? '',
                    active: poll.active,
                    startsAt: toLocalInput(poll.startsAt),
                    endsAt: toLocalInput(poll.endsAt),
                    // Mavjud savol va variantlarning id si saqlanadi - aks holda
                    // ular yangi deb qaraladi va ovozlari yo'qoladi.
                    questions: toEditorQuestions(poll.questions),
                  },
                })
              }
              onDelete={() => setDeleting({ id: poll.id, title: poll.title })}
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
          title={
            dialog.id === null
              ? t(quiz ? 'admin.addTest' : 'admin.addPoll')
              : t(quiz ? 'admin.editTest' : 'admin.editPoll')
          }
          busy={save.isPending}
          error={save.error}
          onClose={() => setDialog(null)}
          onSubmit={() => save.mutate()}
        >
          <TextField
            label={t(quiz ? 'admin.fieldTestTitle' : 'admin.fieldPollTitle')}
            value={dialog.form.title}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, title: event.target.value } })
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

          <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }}>
            <TextField
              label={t('admin.fieldStartsAt')}
              type="datetime-local"
              value={dialog.form.startsAt}
              onChange={(event) =>
                setDialog({ ...dialog, form: { ...dialog.form, startsAt: event.target.value } })
              }
              slotProps={{ inputLabel: { shrink: true } }}
              sx={{ flex: 1, minWidth: 220 }}
            />
            <TextField
              label={t('admin.fieldEndsAt')}
              type="datetime-local"
              value={dialog.form.endsAt}
              onChange={(event) =>
                setDialog({ ...dialog, form: { ...dialog.form, endsAt: event.target.value } })
              }
              slotProps={{ inputLabel: { shrink: true } }}
              sx={{ flex: 1, minWidth: 220 }}
            />
          </Stack>
          <Typography variant="caption" color="text.secondary">
            {t('admin.periodHint')}
          </Typography>

          <Divider />

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              {t('admin.questions')}
            </Typography>
            {dialog.id !== null && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                {t('admin.editOptionsWarning')}
              </Alert>
            )}

            <PollQuestionsEditor
              value={dialog.form.questions}
              quiz={quiz}
              onChange={(questions) => setDialog({ ...dialog, form: { ...dialog.form, questions } })}
            />
          </Box>

          <FormControlLabel
            control={
              <Switch
                checked={dialog.form.active}
                onChange={(event) =>
                  setDialog({ ...dialog, form: { ...dialog.form, active: event.target.checked } })
                }
              />
            }
            label={t('admin.pollActive')}
          />
        </FormDialog>
      )}

      {restarting && (
        <FormDialog
          open
          title={t('admin.restartPoll')}
          submitLabel={t('admin.restartPoll')}
          busy={restart.isPending}
          error={restart.error}
          onClose={() => setRestarting(null)}
          onSubmit={() => restart.mutate()}
        >
          <Alert severity="info">{t('admin.restartHint', { title: restarting.title })}</Alert>

          <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }}>
            <TextField
              label={t('admin.fieldStartsAt')}
              type="datetime-local"
              value={restarting.startsAt}
              onChange={(event) => setRestarting({ ...restarting, startsAt: event.target.value })}
              slotProps={{ inputLabel: { shrink: true } }}
              sx={{ flex: 1, minWidth: 220 }}
            />
            <TextField
              label={t('admin.fieldEndsAt')}
              type="datetime-local"
              value={restarting.endsAt}
              onChange={(event) => setRestarting({ ...restarting, endsAt: event.target.value })}
              slotProps={{ inputLabel: { shrink: true } }}
              sx={{ flex: 1, minWidth: 220 }}
            />
          </Stack>
          <Typography variant="caption" color="text.secondary">
            {t('admin.periodHint')}
          </Typography>
        </FormDialog>
      )}

      {statisticsFor !== null && (
        <PollStatisticsDialog pollId={statisticsFor} onClose={() => setStatisticsFor(null)} />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title={deleting?.title ?? ''}
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
  onToggleStopped,
  onRestart,
  onStatistics,
  onEdit,
  onDelete,
}: {
  poll: PollResponse;
  busy: boolean;
  onToggleActive: (active: boolean) => void;
  onToggleStopped: (stopped: boolean) => void;
  onRestart: () => void;
  onStatistics: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { t } = useTranslation();
  const stopped = poll.stoppedAt !== null;

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack
          direction="row"
          spacing={2}
          sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}
        >
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="subtitle1">{poll.title}</Typography>
            {poll.description && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {poll.description}
              </Typography>
            )}
            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1, mt: 1 }}>
              <Chip
                size="small"
                label={poll.statusLabel}
                color={poll.status === 'OPEN' ? 'success' : stopped ? 'warning' : 'default'}
              />
              {poll.runNumber > 1 && (
                <Chip
                  size="small"
                  variant="outlined"
                  label={t('admin.runNumber', { number: poll.runNumber })}
                />
              )}
              <Chip
                size="small"
                variant="outlined"
                label={t('admin.questionCount', { count: poll.questionCount })}
              />
              <Chip
                size="small"
                variant="outlined"
                label={t('polls.voters', { count: poll.voterCount })}
              />
              {(poll.startsAt || poll.endsAt) && (
                <Chip
                  size="small"
                  variant="outlined"
                  label={`${formatDateTime(poll.startsAt)} — ${formatDateTime(poll.endsAt)}`}
                />
              )}
            </Stack>
          </Box>

          <Stack
            direction="row"
            spacing={0.5}
            sx={{ flexShrink: 0, flexWrap: 'wrap', justifyContent: 'flex-end' }}
          >
            <Button size="small" onClick={onStatistics}>
              {t('admin.statistics')}
            </Button>
            <Button size="small" onClick={onEdit}>
              {t('common.edit')}
            </Button>
            <Button size="small" disabled={busy} onClick={() => onToggleStopped(!stopped)}>
              {stopped ? t('admin.resumePoll') : t('admin.stopPoll')}
            </Button>
            {/* Qayta o'tkazish yangi so'rovnoma ochadi - eskisi tegilmaydi. */}
            <Button size="small" onClick={onRestart}>
              {t('admin.restartPoll')}
            </Button>
            <Button size="small" disabled={busy} onClick={() => onToggleActive(!poll.active)}>
              {poll.active ? t('admin.deactivate') : t('admin.activate')}
            </Button>
            <Button size="small" color="error" onClick={onDelete}>
              {t('common.delete')}
            </Button>
          </Stack>
        </Stack>

        <Stack spacing={2.5} sx={{ mt: 2 }}>
          {poll.questions.map((question) => (
            <QuestionResults key={question.id} question={question} />
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}

/** Bitta savolning natijalari - ustunlar bilan. */
function QuestionResults({ question }: { question: PollQuestionResponse }) {
  const { t } = useTranslation();

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap', mb: 1 }}>
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {question.text}
        </Typography>
        {question.multipleChoice && <Chip size="small" label={t('admin.multipleChoice')} />}
        {!question.required && <Chip size="small" label={t('polls.optional')} />}
        <Typography variant="caption" color="text.secondary">
          {t('polls.answered', { count: question.answeredCount })}
        </Typography>
      </Stack>

      <Stack spacing={1.5}>
        {question.options.map((option) => (
          <Box key={option.id}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center', minWidth: 0 }}>
                <Typography variant="body2">{option.text}</Typography>
                {option.correct && (
                  <Chip size="small" color="success" label={t('admin.correctAnswer')} />
                )}
              </Stack>
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
    </Box>
  );
}

/**
 * Test bo'yicha ball hisoboti - faqat admin panelida ko'rinadi.
 *
 * <p>Savol to'liq to'g'ri hisoblanadi: ishtirokchi barcha to'g'ri
 * variantlarni belgilagan va ortiqchasini tanlamagan bo'lsa.
 */
function QuizStatistics({ stats }: { stats: QuizStatisticsResponse }) {
  const { t } = useTranslation();

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1 }}>
        {t('admin.quizStats')}
      </Typography>

      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1, mb: 2 }}>
        <Chip size="small" label={t('admin.quizParticipants', { count: stats.participants })} />
        <Chip
          size="small"
          variant="outlined"
          label={t('admin.quizAverage', { value: stats.averagePercentage })}
        />
        <Chip
          size="small"
          variant="outlined"
          label={t('admin.quizAverageCorrect', { value: stats.averageCorrect })}
        />
      </Stack>

      <Stack spacing={1.5}>
        {stats.questions.map((question) => (
          <Box key={question.questionId}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="body2">{question.text}</Typography>
              <Typography variant="body2" color="text.secondary">
                {t('admin.quizCorrectCount', {
                  correct: question.correctCount,
                  answered: question.answeredCount,
                })}{' '}
                · {question.correctRate}%
              </Typography>
            </Stack>
            <LinearProgress
              variant="determinate"
              color="success"
              value={question.correctRate}
              sx={{ height: 6, borderRadius: 3 }}
            />
          </Box>
        ))}
      </Stack>
    </Box>
  );
}

/** So'rovnoma statistikasi: ishtirok darajasi va savollar kesimidagi natijalar. */
function PollStatisticsDialog({ pollId, onClose }: { pollId: number; onClose: () => void }) {
  const { t } = useTranslation();
  const query = useQuery({
    queryKey: ['admin', 'polls', pollId, 'statistics'],
    queryFn: () => pollsApi.statistics(pollId),
  });

  return (
    <Dialog open maxWidth="md" fullWidth onClose={onClose}>
      <DialogTitle>{t('admin.statistics')}</DialogTitle>
      <DialogContent dividers>
        <QueryState isPending={query.isPending} error={query.error}>
          {query.data && (
            <Stack spacing={3}>
              <Box>
                <Typography variant="subtitle1">{query.data.title}</Typography>
                <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1, mt: 1 }}>
                  <Chip size="small" label={query.data.statusLabel} />
                  <Chip
                    size="small"
                    variant="outlined"
                    label={t('admin.runNumber', { number: query.data.runNumber })}
                  />
                  <Chip
                    size="small"
                    variant="outlined"
                    label={t('polls.voters', { count: query.data.voterCount })}
                  />
                  <Chip
                    size="small"
                    variant="outlined"
                    label={t('admin.questionCount', { count: query.data.questionCount })}
                  />
                  <Chip
                    size="small"
                    variant="outlined"
                    label={t('admin.completionRate', { value: query.data.completionRate })}
                  />
                </Stack>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                  {t('admin.firstVote')}: {formatDateTime(query.data.firstVoteAt)} ·{' '}
                  {t('admin.lastVote')}: {formatDateTime(query.data.lastVoteAt)}
                </Typography>
              </Box>

              {query.data.quiz && <QuizStatistics stats={query.data.quiz} />}

              <Divider />

              {query.data.questions.map((question) => (
                <QuestionResults key={question.id} question={question} />
              ))}
            </Stack>
          )}
        </QueryState>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.close')}</Button>
      </DialogActions>
    </Dialog>
  );
}
