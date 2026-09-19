import { useMemo, useState } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
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
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import FolderRounded from '@mui/icons-material/FolderRounded';
import ChevronRightRounded from '@mui/icons-material/ChevronRightRounded';
import { pollsApi, type SavePollPayload } from '../api/polls';
import { pollGroupsApi } from '../api/pollGroups';
import type {
  PollGroupResponse,
  PollQuestionResponse,
  PollResponse,
  PollType,
  QuizStatisticsResponse,
} from '../api/types';
import { formatDateTime } from '../lib/format';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';
import { brand, hoverShadow, nestedWash, status } from './theme';
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
  /** Guruh id si satr sifatida - Select faqat satr qiymat bilan ishlaydi. */
  groupId: string;
  startsAt: string;
  endsAt: string;
  questions: EditorQuestion[];
}

function emptyForm(groupId: number | null): PollForm {
  return {
    title: '',
    description: '',
    active: true,
    groupId: groupId == null ? '' : String(groupId),
    startsAt: '',
    endsAt: '',
    questions: [emptyQuestion()],
  };
}

/** Guruh yaratish/tahrirlash shakli. */
interface GroupForm {
  name: string;
  description: string;
  displayOrder: string;
  /** Test guruhida: har bir foydalanuvchiga nechta savol; bo'sh satr - hammasi. */
  questionsPerAttempt: string;
}

function emptyGroupForm(): GroupForm {
  return { name: '', description: '', displayOrder: '', questionsPerAttempt: '' };
}

/** Tez tanlanadigan sonlar: test odatda shulardan biricha savoldan iborat bo'ladi. */
const DRAW_PRESETS = [20, 30, 50, 100, 200];

/**
 * Ro'yxatda savol natijalari shu miqdorgacha darhol ochiq turadi.
 *
 * <p>Testning savollar bazasi 200-300 ta savoldan iborat bo'lishi mumkin -
 * hammasi ochilsa guruh sahifasi cheksiz uzun bo'lib ketardi. Ko'prog'i
 * tugma bilan ochiladi, to'liq hisobot esa "Statistika" oynasida.
 */
const INLINE_RESULTS_LIMIT = 10;

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
 *
 * <p>Sahifa ikki bosqichli: `/admin/polls` guruhlar ro'yxatini,
 * `/admin/polls/{guruh}` esa o'sha guruh ichidagi so'rovnomalarni ko'rsatadi.
 * Hamma so'rovnomani bitta ro'yxatda yoyib tashlash yillar o'tishi bilan
 * cheksiz varaqlanadigan sahifaga aylanardi.
 */
function PollsAdminPageFor({ type }: { type: PollType }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const quiz = type === 'QUIZ';

  // Yo'lda guruh bo'lmasa - guruhlar ro'yxati, bo'lsa - o'sha guruh ichi.
  const params = useParams<{ groupId?: string }>();
  const openGroupId = params.groupId ? Number(params.groupId) : null;
  const listPath = quiz ? '/admin/tests' : '/admin/polls';

  const [dialog, setDialog] = useState<{ id: number | null; form: PollForm } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; title: string } | null>(null);
  const [statisticsFor, setStatisticsFor] = useState<number | null>(null);
  const [restarting, setRestarting] = useState<
    { id: number; title: string; startsAt: string; endsAt: string } | null
  >(null);
  const [groupDialog, setGroupDialog] = useState<{ id: number | null; form: GroupForm } | null>(
    null,
  );
  const [deletingGroup, setDeletingGroup] = useState<{ id: number; name: string } | null>(null);

  const query = useQuery({ queryKey: ['admin', 'polls', type], queryFn: () => pollsApi.all(type) });

  const groups = useQuery({
    queryKey: ['admin', 'pollGroups', type],
    queryFn: () => pollGroupsApi.list(type),
  });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'polls'] });
    void queryClient.invalidateQueries({ queryKey: ['polls'] });
  }

  /** Guruh o'zgarsa so'rovnomalar ro'yxati ham qayta yuklanadi: guruh nomi ularda ko'rinadi. */
  function refreshGroups() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'pollGroups'] });
    refresh();
  }

  /**
   * So'rovnomalarni guruhlarga taqsimlaydi.
   *
   * <p>Guruh ro'yxati serverdan alohida keladi, shuning uchun bo'sh guruh ham
   * ko'rinadi - aks holda endigina yaratilgan guruh yo'qolgandek bo'lardi.
   */
  const byGroup = useMemo(() => {
    const map = new Map<number, PollResponse[]>();

    for (const poll of query.data ?? []) {
      if (poll.groupId === null) continue;
      const list = map.get(poll.groupId);
      if (list) list.push(poll);
      else map.set(poll.groupId, [poll]);
    }

    return map;
  }, [query.data]);

  /*
   * Guruh majburiy, shuning uchun birinchi guruh yaratilmaguncha so'rovnoma
   * qo'shib bo'lmaydi: shakldagi "Guruh" maydonini bo'sh qoldirishdan boshqa
   * iloj qolmasdi va saqlash baribir xato bilan tugardi.
   */
  const noGroups = (groups.data?.length ?? 0) === 0;

  const save = useMutation({
    mutationFn: () => {
      const { id, form } = dialog!;
      const payload: SavePollPayload = {
        title: form.title,
        description: form.description,
        type,
        active: form.active,
        groupId: form.groupId === '' ? null : Number(form.groupId),
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

  /** Ro'yxatdan turib guruhga ko'chirish - to'liq tahrirlash shaklini ochmasdan. */
  const move = useMutation({
    mutationFn: ({ id, groupId }: { id: number; groupId: number }) =>
      pollsApi.setGroup(id, groupId),
    onSuccess: refreshGroups,
  });

  const saveGroup = useMutation({
    mutationFn: () => {
      const { id, form } = groupDialog!;
      const payload = {
        name: form.name,
        description: form.description,
        type,
        displayOrder: form.displayOrder === '' ? undefined : Number(form.displayOrder),
        // So'rovnomalar guruhida savollar har doim to'liq beriladi.
        questionsPerAttempt:
          quiz && form.questionsPerAttempt !== '' ? Number(form.questionsPerAttempt) : null,
      };
      return id === null
        ? pollGroupsApi.create(payload)
        : pollGroupsApi.update(id, payload);
    },
    onSuccess: () => {
      setGroupDialog(null);
      refreshGroups();
    },
  });

  const removeGroup = useMutation({
    mutationFn: () => pollGroupsApi.remove(deletingGroup!.id),
    onSuccess: () => {
      setDeletingGroup(null);
      refreshGroups();
    },
  });

  const openGroup = groups.data?.find((group) => group.id === openGroupId) ?? null;

  return (
    <AdminPage
      title={openGroup ? openGroup.name : t(quiz ? 'admin.testsTitle' : 'admin.pollsTitle')}
      description={openGroup?.description ?? undefined}
      action={
        openGroup ? (
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" component={RouterLink} to={listPath}>
              {t('common.back')}
            </Button>
            <Button
              variant="contained"
              onClick={() => setDialog({ id: null, form: emptyForm(openGroup.id) })}
            >
              {t(quiz ? 'admin.addTest' : 'admin.addPoll')}
            </Button>
          </Stack>
        ) : (
          <Button
            variant="contained"
            onClick={() => setGroupDialog({ id: null, form: emptyGroupForm() })}
          >
            {t('admin.addGroup')}
          </Button>
        )
      }
    >
      <MutationError
        error={setActive.error ?? setStopped.error ?? remove.error ?? move.error ?? removeGroup.error}
      />

      <QueryState isPending={query.isPending} error={query.error}>
        {(() => {
          /*
           * Bir so'rovnoma kartochkasini yasash uchun kerakli barcha
           * ishlovchilar shu yerda yig'iladi: ular guruhli va guruhsiz
           * bo'limlarda bir xil.
           */
          const renderPoll = (poll: PollResponse) => (
            <PollRow
              key={poll.id}
              poll={poll}
              groups={groups.data ?? []}
              busy={setActive.isPending || setStopped.isPending || move.isPending}
              onToggleActive={(active) => setActive.mutate({ id: poll.id, active })}
              onToggleStopped={(stopped) => setStopped.mutate({ id: poll.id, stopped })}
              onMove={(groupId) => move.mutate({ id: poll.id, groupId })}
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
                    groupId: poll.groupId === null ? '' : String(poll.groupId),
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
          );

          // --------------------------------------------- guruh ichi
          if (openGroupId !== null) {
            const polls = byGroup.get(openGroupId) ?? [];

            if (polls.length === 0) {
              return (
                <Typography variant="body2" color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
                  {t('common.noData')}
                </Typography>
              );
            }

            return <Stack spacing={2}>{polls.map(renderPoll)}</Stack>;
          }

          // --------------------------------------------- guruhlar ro'yxati
          /*
             Guruhsiz so'rovnoma bo'lmaydi, shuning uchun sahifa guruhdan
             boshlanadi: guruh yo'q ekan, ko'rsatadigan narsa ham yo'q.
          */
          if (noGroups) {
            return (
              <Stack spacing={1} sx={{ alignItems: 'center', py: 6 }}>
                <Typography variant="body2" color="text.secondary">
                  {t('admin.noGroupsYet')}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {t('admin.groupHint')}
                </Typography>
                <Button
                  variant="contained"
                  sx={{ mt: 1 }}
                  onClick={() => setGroupDialog({ id: null, form: emptyGroupForm() })}
                >
                  {t('admin.addGroup')}
                </Button>
              </Stack>
            );
          }

          return (
            <Stack spacing={2}>
              {groups.data?.map((group) => (
                <GroupCard
                  key={group.id}
                  group={group}
                  to={`${listPath}/${group.id}`}
                  polls={byGroup.get(group.id) ?? []}
                  onEdit={() =>
                    setGroupDialog({
                      id: group.id,
                      form: {
                        name: group.name,
                        description: group.description ?? '',
                        displayOrder: String(group.displayOrder),
                        questionsPerAttempt:
                          group.questionsPerAttempt === null ? '' : String(group.questionsPerAttempt),
                      },
                    })
                  }
                  onDelete={() => setDeletingGroup({ id: group.id, name: group.name })}
                />
              ))}
            </Stack>
          );
        })()}
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

          {/* Bo'sh variant yo'q: har bir so'rovnoma qaysidir o'tkazishga tegishli. */}
          <TextField
            select
            label={t('admin.fieldGroup')}
            value={dialog.form.groupId}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, groupId: event.target.value } })
            }
            required
            fullWidth
          >
            {groups.data?.map((group) => (
              <MenuItem key={group.id} value={String(group.id)}>
                {group.name}
              </MenuItem>
            ))}
          </TextField>

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

            {quiz && (
              <DrawInfo
                group={groups.data?.find((group) => String(group.id) === dialog.form.groupId)}
                questions={dialog.form.questions}
              />
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

      {groupDialog && (
        <FormDialog
          open
          title={groupDialog.id === null ? t('admin.addGroup') : t('admin.editGroup')}
          busy={saveGroup.isPending}
          error={saveGroup.error}
          onClose={() => setGroupDialog(null)}
          onSubmit={() => saveGroup.mutate()}
        >
          <TextField
            label={t('admin.fieldName')}
            value={groupDialog.form.name}
            onChange={(event) =>
              setGroupDialog({
                ...groupDialog,
                form: { ...groupDialog.form, name: event.target.value },
              })
            }
            required
            fullWidth
          />
          <TextField
            label={t('admin.fieldDescription')}
            value={groupDialog.form.description}
            onChange={(event) =>
              setGroupDialog({
                ...groupDialog,
                form: { ...groupDialog.form, description: event.target.value },
              })
            }
            multiline
            rows={2}
            fullWidth
          />
          <TextField
            label={t('admin.fieldOrder')}
            type="number"
            value={groupDialog.form.displayOrder}
            onChange={(event) =>
              setGroupDialog({
                ...groupDialog,
                form: { ...groupDialog.form, displayOrder: event.target.value },
              })
            }
            fullWidth
          />

          {quiz && (
            <DrawSizeField
              value={groupDialog.form.questionsPerAttempt}
              onChange={(questionsPerAttempt) =>
                setGroupDialog({
                  ...groupDialog,
                  form: { ...groupDialog.form, questionsPerAttempt },
                })
              }
            />
          )}
        </FormDialog>
      )}

      <ConfirmDialog
        open={deletingGroup !== null}
        title={deletingGroup?.name ?? ''}
        message={t('admin.deleteGroupHint')}
        busy={removeGroup.isPending}
        onCancel={() => setDeletingGroup(null)}
        onConfirm={() => removeGroup.mutate()}
      />
    </AdminPage>
  );
}

/**
 * Test guruhida har bir foydalanuvchiga nechta savol berilishi.
 *
 * <p>Son testda emas, guruhda belgilanadi: guruhdagi barcha testlar unga
 * bo'ysunadi. Guruh yaratilayotganda testlarda hali savol yo'q, shuning uchun
 * son savollar bilan solishtirilmaydi - testda kamroq savol bo'lsa bori beriladi.
 */
function DrawSizeField({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const { t } = useTranslation();
  const count = value === '' ? null : Number(value);

  return (
    <Box>
      <Typography variant="subtitle2">{t('admin.drawTitle')}</Typography>

      <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1, alignItems: 'center', mt: 1 }}>
        <Chip
          label={t('admin.drawAll')}
          color={count === null ? 'primary' : 'default'}
          variant={count === null ? 'filled' : 'outlined'}
          onClick={() => onChange('')}
        />
        {DRAW_PRESETS.map((preset) => (
          <Chip
            key={preset}
            label={preset}
            color={count === preset ? 'primary' : 'default'}
            variant={count === preset ? 'filled' : 'outlined'}
            onClick={() => onChange(String(preset))}
          />
        ))}
        <TextField
          size="small"
          type="number"
          label={t('admin.drawCustom')}
          value={value}
          // Faqat butun son: savolning yarmini berib bo'lmaydi.
          onChange={(event) => onChange(event.target.value.replace(/\D/g, ''))}
          slotProps={{ htmlInput: { min: 1, max: 500, step: 1 } }}
          sx={{ width: 130 }}
        />
      </Stack>

      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
        {count === null ? t('admin.drawAllHint') : t('admin.drawHint', { count })}
      </Typography>
    </Box>
  );
}

/**
 * Test shaklida: guruh sozlamasi bo'yicha foydalanuvchiga nechta savol tushishi.
 *
 * <p>Son guruhda turadi, savollar esa shu yerda kiritiladi - administrator
 * 200 ta savol kiritayotib, ulardan nechtasi berilishini ko'rib tursin.
 */
function DrawInfo({
  group,
  questions,
}: {
  group: PollGroupResponse | undefined;
  questions: EditorQuestion[];
}) {
  const { t } = useTranslation();
  if (!group || group.questionsPerAttempt === null) return null;

  const count = group.questionsPerAttempt;
  const total = questions.filter((question) => question.text.trim() !== '').length;

  return (
    <Alert severity="info" sx={{ mb: 2 }}>
      {total < count
        ? t('admin.drawTestShort', { group: group.name, count, total })
        : t('admin.drawTestInfo', { group: group.name, count, total })}
    </Alert>
  );
}

/**
 * Guruhlar ro'yxatidagi bitta kartochka.
 *
 * <p>Butun kartochka havola: guruh ichiga kirish eng ko'p bajariladigan amal,
 * shuning uchun uni kichkina tugmaga bog'lab qo'yish noqulay bo'lardi.
 * Tahrirlash va o'chirish tugmalari havolaning ustida turadi va bosilganda
 * o'tishni to'xtatadi.
 */
function GroupCard({
  group,
  to,
  polls,
  onEdit,
  onDelete,
}: {
  group: PollGroupResponse;
  to: string;
  polls: PollResponse[];
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { t } = useTranslation();
  const open = polls.filter((poll) => poll.status === 'OPEN').length;

  return (
    <Card
      variant="outlined"
      component={RouterLink}
      to={to}
      sx={{
        display: 'block',
        textDecoration: 'none',
        color: 'inherit',
        borderLeft: `4px solid ${brand[500]}`,
        backgroundImage: nestedWash,
        '&:hover': { transform: 'translateY(-2px)', boxShadow: hoverShadow },
      }}
    >
      <CardContent>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start' }}>
          <Box
            aria-hidden
            sx={{
              flexShrink: 0,
              width: 42,
              height: 42,
              borderRadius: 2.5,
              display: 'grid',
              placeItems: 'center',
              color: '#ffffff',
              backgroundImage: `linear-gradient(135deg, ${brand[700]}, ${brand[500]})`,
              '& svg': { fontSize: 22 },
            }}
          >
            <FolderRounded />
          </Box>

          <Box sx={{ minWidth: 0, flexGrow: 1 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              {group.name}
            </Typography>
            {group.description && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {group.description}
              </Typography>
            )}
            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1, mt: 1.25 }}>
              <Chip size="small" label={t('admin.groupPollCount', { count: polls.length })} />
              {open > 0 && (
                <Chip size="small" color="success" label={t('admin.groupOpenCount', { count: open })} />
              )}
              {group.questionsPerAttempt !== null && (
                <Chip
                  size="small"
                  variant="outlined"
                  color="primary"
                  label={t('admin.drawChip', { count: group.questionsPerAttempt })}
                />
              )}
            </Stack>
          </Box>

          {/*
            Havola ichidagi tugmalar: bosilganda guruh ichiga o'tib
            ketmasligi uchun hodisa to'xtatiladi.
          */}
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ flexShrink: 0, alignItems: 'center' }}
            onClick={(event) => {
              event.preventDefault();
              event.stopPropagation();
            }}
          >
            <Button size="small" onClick={onEdit}>
              {t('common.edit')}
            </Button>
            <Button size="small" color="error" onClick={onDelete}>
              {t('common.delete')}
            </Button>
            <ChevronRightRounded sx={{ color: 'text.secondary' }} />
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}

function PollRow({
  poll,
  groups,
  busy,
  onToggleActive,
  onToggleStopped,
  onMove,
  onRestart,
  onStatistics,
  onEdit,
  onDelete,
}: {
  poll: PollResponse;
  groups: PollGroupResponse[];
  busy: boolean;
  onToggleActive: (active: boolean) => void;
  onToggleStopped: (stopped: boolean) => void;
  onMove: (groupId: number) => void;
  onRestart: () => void;
  onStatistics: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { t } = useTranslation();
  const stopped = poll.stoppedAt !== null;
  const [resultsOpen, setResultsOpen] = useState(poll.questions.length <= INLINE_RESULTS_LIMIT);

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
              {poll.questionsPerAttempt !== null && (
                <Chip
                  size="small"
                  variant="outlined"
                  color="primary"
                  label={t('admin.drawChip', { count: poll.questionsPerAttempt })}
                />
              )}
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
            sx={{ flexShrink: 0, flexWrap: 'wrap', alignItems: 'center', justifyContent: 'flex-end' }}
          >
            {/*
              Guruhni to'g'ridan-to'g'ri ro'yxatdan almashtirish: mavjud
              so'rovnomalarni tartibga solish uchun har birida to'liq
              tahrirlash shaklini ochish juda uzoq yo'l bo'lardi.
            */}
            <TextField
              select
              size="small"
              label={t('admin.moveToGroup')}
              value={poll.groupId === null ? '' : String(poll.groupId)}
              disabled={busy}
              onChange={(event) => onMove(Number(event.target.value))}
              sx={{ minWidth: 170, mr: 0.5 }}
            >
              {groups.map((group) => (
                <MenuItem key={group.id} value={String(group.id)}>
                  {group.name}
                </MenuItem>
              ))}
            </TextField>

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

        {poll.questions.length > INLINE_RESULTS_LIMIT && (
          <Button size="small" sx={{ mt: 1.5 }} onClick={() => setResultsOpen((open) => !open)}>
            {resultsOpen
              ? t('admin.hideQuestionResults')
              : t('admin.showQuestionResults', { count: poll.questions.length })}
          </Button>
        )}

        {resultsOpen && (
          <Stack spacing={2.5} sx={{ mt: 2 }}>
            {poll.questions.map((question) => (
              <QuestionResults key={question.id} question={question} />
            ))}
          </Stack>
        )}
      </CardContent>
    </Card>
  );
}

/** Bitta savolning natijalari - ustunlar bilan. */
function QuestionResults({ question }: { question: PollQuestionResponse }) {
  const { t } = useTranslation();

  return (
    /*
     * Har bir savol o'z bloki bo'lib turadi. Ilgari savollar bir-birining
     * ketidan oqib ketardi va qaysi variant qaysi savolniki ekani faqat
     * bo'shliqdan bilinardi.
     */
    <Box sx={{ ...questionBlockStyles }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap', mb: 1.5 }}>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
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
            <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.75 }}>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center', minWidth: 0 }}>
                <Typography variant="body2">{option.text}</Typography>
                {option.correct && (
                  <Chip size="small" color="success" label={t('admin.correctAnswer')} />
                )}
              </Stack>
              <Typography variant="body2" sx={{ fontWeight: 700, whiteSpace: 'nowrap' }}>
                {option.voteCount} · {option.percentage}%
              </Typography>
            </Stack>
            <LinearProgress variant="determinate" value={option.percentage} sx={{ height: 8 }} />
          </Box>
        ))}
      </Stack>
    </Box>
  );
}

/** Savol bloki: yengil fon va chapdagi rangli chekka. */
const questionBlockStyles = {
  p: 2,
  borderRadius: 3,
  backgroundImage: nestedWash,
  border: '1px solid rgba(148, 163, 184, 0.20)',
  borderLeft: `3px solid ${brand[500]}`,
} as const;

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
          <Box
            key={question.questionId}
            // To'g'ri javob ulushi yashil o'lchov, shuning uchun blokning
            // chekkasi ham yashil - savol natijalari bloklaridan farq qiladi.
            sx={{ ...questionBlockStyles, borderLeftColor: status.good }}
          >
            <Stack direction="row" spacing={1} sx={{ justifyContent: 'space-between', mb: 0.75 }}>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {question.text}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
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
              sx={{ height: 8 }}
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
                  {query.data.questionsPerAttempt !== null && (
                    <Chip
                      size="small"
                      variant="outlined"
                      color="primary"
                      label={t('admin.drawChip', { count: query.data.questionsPerAttempt })}
                    />
                  )}
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
