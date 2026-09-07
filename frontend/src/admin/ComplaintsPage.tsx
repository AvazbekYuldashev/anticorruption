import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import {
  Button,
  Card,
  Chip,
  FormControlLabel,
  MenuItem,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { complaintsApi } from '../api/complaints';
import { referenceApi } from '../api/reference';
import { formatDate } from '../lib/format';
import { AdminPage, QueryState, StatusChip } from './common';

interface Filters {
  query: string;
  status: string;
  category: string;
  facultyId: string;
  reporterType: string;
  unassigned: boolean;
}

const EMPTY: Filters = {
  query: '',
  status: '',
  category: '',
  facultyId: '',
  reporterType: '',
  unassigned: false,
};

export function AdminComplaintsPage() {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<Filters>(EMPTY);
  const [applied, setApplied] = useState<Filters>(EMPTY);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const reference = useQuery({ queryKey: ['reference'], queryFn: referenceApi.all });

  const query = useQuery({
    queryKey: ['admin', 'complaints', applied, page, size],
    queryFn: () =>
      complaintsApi.search({
        query: applied.query || undefined,
        status: applied.status || undefined,
        category: applied.category || undefined,
        facultyId: applied.facultyId ? Number(applied.facultyId) : null,
        reporterType: applied.reporterType || undefined,
        unassigned: applied.unassigned || undefined,
        page,
        size,
      }),
    placeholderData: keepPreviousData,
  });

  function apply() {
    setPage(0);
    setApplied(draft);
  }

  return (
    <AdminPage title={t('admin.complaintsTitle')}>
      <Card variant="outlined" sx={{ p: 2, mb: 3 }}>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            apply();
          }}
        >
          <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
            <TextField
              size="small"
              label={t('common.search')}
              placeholder={t('admin.searchPlaceholder')}
              value={draft.query}
              onChange={(event) => setDraft({ ...draft, query: event.target.value })}
              sx={{ flex: '2 1 260px' }}
            />

            <TextField
              select
              size="small"
              label={t('admin.colStatus')}
              value={draft.status}
              onChange={(event) => setDraft({ ...draft, status: event.target.value })}
              sx={{ flex: '1 1 160px' }}
            >
              <MenuItem value="">{t('common.all')}</MenuItem>
              {(reference.data?.statuses ?? []).map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              size="small"
              label={t('admin.colCategory')}
              value={draft.category}
              onChange={(event) => setDraft({ ...draft, category: event.target.value })}
              sx={{ flex: '1 1 200px' }}
            >
              <MenuItem value="">{t('common.all')}</MenuItem>
              {(reference.data?.categories ?? []).map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              size="small"
              label={t('admin.colFaculty')}
              value={draft.facultyId}
              onChange={(event) => setDraft({ ...draft, facultyId: event.target.value })}
              sx={{ flex: '1 1 200px' }}
            >
              <MenuItem value="">{t('common.all')}</MenuItem>
              {(reference.data?.faculties ?? []).map((faculty) => (
                <MenuItem key={faculty.id} value={String(faculty.id)}>
                  {faculty.name}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              size="small"
              label={t('admin.colReporter')}
              value={draft.reporterType}
              onChange={(event) => setDraft({ ...draft, reporterType: event.target.value })}
              sx={{ flex: '1 1 180px' }}
            >
              <MenuItem value="">{t('common.all')}</MenuItem>
              {(reference.data?.reporterTypes ?? []).map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>
          </Stack>

          <Stack direction="row" spacing={2} sx={{ alignItems: 'center', flexWrap: 'wrap', mt: 2 }}>
            <FormControlLabel
              control={
                <Switch
                  size="small"
                  checked={draft.unassigned}
                  onChange={(event) => setDraft({ ...draft, unassigned: event.target.checked })}
                />
              }
              label={<Typography variant="body2">{t('admin.unassignedOnly')}</Typography>}
            />
            <Button type="submit" variant="contained" size="small">
              {t('common.filter')}
            </Button>
            <Button
              size="small"
              onClick={() => {
                setDraft(EMPTY);
                setApplied(EMPTY);
                setPage(0);
              }}
            >
              {t('common.reset')}
            </Button>
          </Stack>
        </form>
      </Card>

      <QueryState isPending={query.isPending} error={query.error}>
        {query.data && (
          <Paper variant="outlined">
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>{t('admin.colCode')}</TableCell>
                    <TableCell>{t('admin.colTitle')}</TableCell>
                    <TableCell>{t('admin.colCategory')}</TableCell>
                    <TableCell>{t('admin.colFaculty')}</TableCell>
                    <TableCell>{t('admin.colStatus')}</TableCell>
                    <TableCell>{t('admin.colAssignee')}</TableCell>
                    <TableCell align="right">{t('admin.colDate')}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {query.data.content.map((complaint) => (
                    <TableRow
                      key={complaint.id}
                      hover
                      component={Link}
                      to={`/admin/complaints/${complaint.id}`}
                      sx={{ textDecoration: 'none', cursor: 'pointer' }}
                    >
                      <TableCell sx={{ fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
                        {complaint.trackingCode}
                      </TableCell>
                      <TableCell sx={{ maxWidth: 320 }}>
                        <Typography variant="body2" noWrap>
                          {complaint.title}
                        </Typography>
                        {complaint.anonymous && (
                          <Chip size="small" label={t('admin.anonymous')} sx={{ mt: 0.5 }} />
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {complaint.categoryLabel}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {complaint.facultyName ?? '—'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <StatusChip status={complaint.status} label={complaint.statusLabel} />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {complaint.assigneeName ?? '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                        <Typography variant="caption" color="text.secondary">
                          {formatDate(complaint.createdAt)}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}

                  {query.data.content.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                        <Typography variant="body2" color="text.secondary">
                          {t('common.noData')}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            <TablePagination
              component="div"
              count={query.data.totalElements}
              page={page}
              rowsPerPage={size}
              onPageChange={(_, next) => setPage(next)}
              onRowsPerPageChange={(event) => {
                setSize(Number(event.target.value));
                setPage(0);
              }}
              rowsPerPageOptions={[10, 20, 50]}
            />
          </Paper>
        )}
      </QueryState>
    </AdminPage>
  );
}
