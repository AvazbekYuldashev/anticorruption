import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  FormControlLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { complaintsApi } from '../api/complaints';
import { referenceApi } from '../api/reference';
import { usersApi } from '../api/users';
import type { ComplaintResponse, ComplaintStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { formatDate, formatDateTime, formatFileSize } from '../lib/format';
import { AdminPage, MutationError, QueryState, StatusChip } from './common';

/**
 * Backenddagi qoidaning nusxasi: qaysi holatdan qaysi holatga o'tish mumkin.
 *
 * <p>Bu yerda faqat ro'yxatni cheklash uchun - haqiqiy tekshiruv baribir
 * backendda. Shu tufayli xodim aniq amalga oshmaydigan variantni tanlab,
 * keyin xatolik olib o'tirmaydi.
 */
const ALLOWED_TRANSITIONS: Record<ComplaintStatus, ComplaintStatus[]> = {
  NEW: ['IN_REVIEW', 'NEED_INFO', 'RESOLVED', 'REJECTED'],
  IN_REVIEW: ['NEED_INFO', 'RESOLVED', 'REJECTED'],
  NEED_INFO: ['IN_REVIEW', 'RESOLVED', 'REJECTED'],
  RESOLVED: [],
  REJECTED: [],
};

function Row({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={{ sm: 2 }} sx={{ py: 1 }}>
      <Typography variant="body2" color="text.secondary" sx={{ width: 200, flexShrink: 0 }}>
        {label}
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Stack>
  );
}

export function AdminComplaintDetailPage() {
  const { t } = useTranslation();
  const { id = '' } = useParams();
  const complaintId = Number(id);
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();

  const query = useQuery({
    queryKey: ['admin', 'complaint', complaintId],
    queryFn: () => complaintsApi.detail(complaintId),
    retry: false,
  });

  return (
    <AdminPage
      title={t('admin.complaintDetail')}
      action={
        <Button component={Link} to="/admin/complaints" size="small">
          ← {t('admin.complaintsTitle')}
        </Button>
      }
    >
      <QueryState isPending={query.isPending} error={query.error}>
        {query.data && (
          <Stack spacing={3}>
            <ComplaintOverview complaint={query.data} />

            <Stack direction={{ xs: 'column', lg: 'row' }} spacing={3} sx={{ alignItems: 'flex-start' }}>
              <Box sx={{ flex: '1 1 0', width: '100%' }}>
                <StatusForm
                  complaint={query.data}
                  onDone={() =>
                    queryClient.invalidateQueries({ queryKey: ['admin', 'complaint', complaintId] })
                  }
                />
              </Box>
              <Box sx={{ flex: '1 1 0', width: '100%' }}>
                <Stack spacing={3}>
                  {isAdmin && (
                    <AssignForm
                      complaint={query.data}
                      onDone={() =>
                        queryClient.invalidateQueries({
                          queryKey: ['admin', 'complaint', complaintId],
                        })
                      }
                    />
                  )}
                  <RegisterVisibilityForm
                    complaint={query.data}
                    onDone={() =>
                      queryClient.invalidateQueries({
                        queryKey: ['admin', 'complaint', complaintId],
                      })
                    }
                  />
                </Stack>
              </Box>
            </Stack>

            <HistoryCard complaint={query.data} />
          </Stack>
        )}
      </QueryState>
    </AdminPage>
  );
}

// ---------------------------------------------------------------- ma'lumot

function ComplaintOverview({ complaint }: { complaint: ComplaintResponse }) {
  const { t } = useTranslation();

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
              {complaint.trackingCode}
            </Typography>
            <Typography variant="h6" sx={{ mt: 0.5 }}>
              {complaint.title}
            </Typography>
          </Box>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            {complaint.anonymous && <Chip size="small" label={t('admin.anonymous')} />}
            <StatusChip status={complaint.status} label={complaint.statusLabel} />
          </Stack>
        </Stack>

        <Typography variant="body2" sx={{ mt: 2, whiteSpace: 'pre-line' }}>
          {complaint.description}
        </Typography>

        <Divider sx={{ my: 2 }} />

        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          {t('admin.complaintInfo')}
        </Typography>
        <Row label={t('track.category')} value={complaint.categoryLabel} />
        <Row label={t('track.faculty')} value={complaint.facultyName} />
        <Row label={t('track.department')} value={complaint.departmentName} />
        <Row label={t('track.subject')} value={complaint.subjectName} />
        <Row label={t('submit.fieldPosition')} value={complaint.accusedPositionLabel} />
        <Row
          label={t('submit.fieldIncidentDate')}
          value={complaint.incidentDate ? formatDate(complaint.incidentDate) : null}
        />
        <Row label={t('submit.fieldIncidentPlace')} value={complaint.incidentPlace} />
        <Row label={t('track.submitted')} value={formatDateTime(complaint.createdAt)} />
        <Row
          label={t('track.closed')}
          value={complaint.closedAt ? formatDateTime(complaint.closedAt) : null}
        />

        <Divider sx={{ my: 2 }} />

        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          {t('admin.reporterInfo')}
        </Typography>
        {complaint.anonymous ? (
          <Typography variant="body2" color="text.secondary">
            {t('submit.anonymousHint')}
          </Typography>
        ) : (
          <>
            <Row label={t('submit.fieldReporterType')} value={complaint.reporterTypeLabel} />
            <Row label={t('submit.fieldReporterName')} value={complaint.reporterName} />
            <Row label={t('submit.fieldReporterEmail')} value={complaint.reporterEmail} />
            <Row label={t('submit.fieldReporterPhone')} value={complaint.reporterPhone} />
            <Row
              label={t('submit.fieldCourseYear')}
              value={complaint.courseYear ? String(complaint.courseYear) : null}
            />
            <Row label={t('submit.fieldGroupName')} value={complaint.groupName} />
            <Row label={t('submit.fieldStudyForm')} value={complaint.studyFormLabel} />
          </>
        )}
        {/* Anonim murojaatda ham maqom ko'rsatiladi - u shaxsni oshkor qilmaydi. */}
        {complaint.anonymous && (
          <Row label={t('submit.fieldReporterType')} value={complaint.reporterTypeLabel} />
        )}

        {complaint.attachments.length > 0 && (
          <>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              {t('admin.attachments')}
            </Typography>
            <Stack spacing={0.5}>
              {complaint.attachments.map((attachment) => (
                <Typography key={attachment.id} variant="body2">
                  <a href={attachment.downloadUrl} target="_blank" rel="noopener noreferrer">
                    {attachment.originalName}
                  </a>{' '}
                  <Typography component="span" variant="caption" color="text.secondary">
                    ({formatFileSize(attachment.sizeBytes)})
                  </Typography>
                </Typography>
              ))}
            </Stack>
          </>
        )}
      </CardContent>
    </Card>
  );
}

// ---------------------------------------------------------------- holat

function StatusForm({
  complaint,
  onDone,
}: {
  complaint: ComplaintResponse;
  onDone: () => void;
}) {
  const { t } = useTranslation();
  const reference = useQuery({ queryKey: ['reference'], queryFn: referenceApi.all });

  const allowed = ALLOWED_TRANSITIONS[complaint.status];
  const [status, setStatus] = useState('');
  const [note, setNote] = useState('');
  const [officialResponse, setOfficialResponse] = useState(complaint.officialResponse ?? '');

  const mutation = useMutation({
    mutationFn: () =>
      complaintsApi.updateStatus(complaint.id, {
        status,
        note: note.trim() || undefined,
        officialResponse: officialResponse.trim() || undefined,
      }),
    onSuccess: () => {
      setStatus('');
      setNote('');
      onDone();
    },
  });

  const statusOptions = (reference.data?.statuses ?? []).filter((option) =>
    allowed.includes(option.value as ComplaintStatus),
  );

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle2" sx={{ mb: 2 }}>
          {t('admin.changeStatus')}
        </Typography>

        {allowed.length === 0 ? (
          <Alert severity="info">{t('admin.noTransitions')}</Alert>
        ) : (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              mutation.mutate();
            }}
          >
            <Stack spacing={2.5}>
              <TextField
                select
                size="small"
                label={t('admin.newStatus')}
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                required
                fullWidth
              >
                {statusOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>

              <TextField
                size="small"
                label={t('admin.internalNote')}
                helperText={t('admin.internalNoteHint')}
                value={note}
                onChange={(event) => setNote(event.target.value)}
                multiline
                rows={2}
                fullWidth
              />

              <TextField
                size="small"
                label={t('admin.officialResponse')}
                helperText={t('admin.officialResponseHint')}
                value={officialResponse}
                onChange={(event) => setOfficialResponse(event.target.value)}
                multiline
                rows={4}
                fullWidth
              />

              <Button type="submit" variant="contained" disabled={!status || mutation.isPending}>
                {t('admin.applyStatus')}
              </Button>
            </Stack>

            <MutationError error={mutation.error} />
          </form>
        )}
      </CardContent>
    </Card>
  );
}

// ---------------------------------------------------------------- mas'ul xodim

function AssignForm({ complaint, onDone }: { complaint: ComplaintResponse; onDone: () => void }) {
  const { t } = useTranslation();

  /*
   * Xodimlar ro'yxati faqat administratorga ochiq (backenddagi qoida),
   * shuning uchun bu blok ham faqat administratorga ko'rsatiladi.
   * Moderator murojaatni holatini o'zgartirish orqali o'ziga oladi -
   * backend biriktirilmagan murojaatni avtomatik unga bog'laydi.
   */
  const moderators = useQuery({
    queryKey: ['admin', 'users', 'MODERATOR'],
    queryFn: () => usersApi.list('MODERATOR', 0, 100),
  });
  const admins = useQuery({
    queryKey: ['admin', 'users', 'ADMIN'],
    queryFn: () => usersApi.list('ADMIN', 0, 100),
  });

  const [assigneeId, setAssigneeId] = useState(
    complaint.assignee ? String(complaint.assignee.id) : '',
  );

  const mutation = useMutation({
    mutationFn: () => complaintsApi.assign(complaint.id, assigneeId ? Number(assigneeId) : null),
    onSuccess: onDone,
  });

  const staff = [...(moderators.data?.content ?? []), ...(admins.data?.content ?? [])];

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle2" sx={{ mb: 2 }}>
          {t('admin.assignTitle')}
        </Typography>

        <form
          onSubmit={(event) => {
            event.preventDefault();
            mutation.mutate();
          }}
        >
          <Stack spacing={2}>
            <TextField
              select
              size="small"
              value={assigneeId}
              onChange={(event) => setAssigneeId(event.target.value)}
              fullWidth
            >
              <MenuItem value="">{t('admin.assignNobody')}</MenuItem>
              {staff.map((member) => (
                <MenuItem key={member.id} value={String(member.id)}>
                  {member.fullName} — {member.roleLabel}
                </MenuItem>
              ))}
            </TextField>

            <Button type="submit" variant="outlined" disabled={mutation.isPending}>
              {t('common.save')}
            </Button>
          </Stack>

          <MutationError error={mutation.error} />
        </form>
      </CardContent>
    </Card>
  );
}

// ---------------------------------------------------------------- reyestr

function RegisterVisibilityForm({
  complaint,
  onDone,
}: {
  complaint: ComplaintResponse;
  onDone: () => void;
}) {
  const { t } = useTranslation();

  const mutation = useMutation({
    mutationFn: (hidden: boolean) => complaintsApi.setRegisterVisibility(complaint.id, hidden),
    onSuccess: onDone,
  });

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          {t('register.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {t('admin.hideHint')}
        </Typography>

        <FormControlLabel
          control={
            <Switch
              checked={complaint.hiddenFromRegister}
              disabled={mutation.isPending}
              onChange={(event) => mutation.mutate(event.target.checked)}
            />
          }
          label={
            <Typography variant="body2">
              {complaint.hiddenFromRegister ? t('admin.hiddenFromRegister') : t('admin.showInRegister')}
            </Typography>
          }
        />

        <MutationError error={mutation.error} />
      </CardContent>
    </Card>
  );
}

// ---------------------------------------------------------------- tarix

function HistoryCard({ complaint }: { complaint: ComplaintResponse }) {
  const { t } = useTranslation();

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle2" sx={{ mb: 2 }}>
          {t('admin.history')}
        </Typography>
        <Stack spacing={2}>
          {complaint.history.map((entry) => (
            <Box key={entry.id} sx={{ borderLeft: 2, borderColor: 'primary.light', pl: 2 }}>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                {entry.oldStatusLabel && (
                  <Typography variant="caption" color="text.secondary">
                    {entry.oldStatusLabel} →
                  </Typography>
                )}
                <StatusChip status={entry.newStatus} label={entry.newStatusLabel} />
                <Typography variant="caption" color="text.secondary">
                  {formatDateTime(entry.changedAt)}
                </Typography>
                {entry.changedBy && (
                  <Typography variant="caption" color="text.secondary">
                    · {entry.changedBy}
                  </Typography>
                )}
              </Stack>
              {entry.note && (
                <Typography variant="body2" sx={{ mt: 0.5 }}>
                  {entry.note}
                </Typography>
              )}
            </Box>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}
