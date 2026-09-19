import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import {
  Box,
  Button,
  Card,
  MenuItem,
  Paper,
  Stack,
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
import { codePill, listSecondaryText, panelWash } from './theme';

interface Filters {
  code: string;
  status: string;
  category: string;
  facultyId: string;
}

const EMPTY: Filters = { code: '', status: '', category: '', facultyId: '' };

/**
 * Murojaatlar reyestri.
 *
 * <p>Ilgari ommaviy saytning sahifasi edi va xodimlarga sayt menyusidan
 * ochilardi. Endi faqat admin panelida: reyestr xodimlarning ish ro'yxati,
 * uni tashrifchilarga mo'ljallangan qismda ushlab turishning ma'nosi yo'q.
 * Backend ham uni faqat moderator va administratorga beradi.
 *
 * <p>"Murojaatlar" bo'limidan farqi: bu yerda sarlavha, matn va mas'ul xodim
 * yo'q - faqat oldindan belgilangan qiymatlar (kod, holat, tur, fakultet)
 * va sanalar. Reyestr murojaatchini konstruksiyasi bo'yicha oshkor qilmaydi.
 *
 * <p>Ommaviy sahifadagi raqamli kartochkalar bu yerga ko'chirilmadi: aynan
 * o'sha raqamlar bir qadam narida - boshqaruv panelida turibdi.
 */
export function RegisterAdminPage() {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<Filters>(EMPTY);
  const [applied, setApplied] = useState<Filters>(EMPTY);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const reference = useQuery({ queryKey: ['reference'], queryFn: referenceApi.all });

  const query = useQuery({
    queryKey: ['admin', 'register', applied, page, size],
    queryFn: () =>
      complaintsApi.register({
        code: applied.code.trim() || undefined,
        status: applied.status || undefined,
        category: applied.category || undefined,
        facultyId: applied.facultyId ? Number(applied.facultyId) : null,
        page,
        size,
      }),
    // Sahifa almashganda jadval bo'shab qolmasin - eski natija turaveradi.
    placeholderData: keepPreviousData,
  });

  function apply() {
    setPage(0);
    setApplied(draft);
  }

  function reset() {
    setDraft(EMPTY);
    setApplied(EMPTY);
    setPage(0);
  }

  return (
    <AdminPage title={t('register.title')} description={t('register.intro')}>
      {/* Boshqaruv paneli: yengil ko'kimtir fon uni pastdagi oq jadvaldan ajratadi. */}
      <Card variant="outlined" sx={{ p: 2, mb: 3, backgroundImage: panelWash }}>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            apply();
          }}
        >
          <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
            <TextField
              size="small"
              label={t('register.filterCode')}
              placeholder={t('register.searchPlaceholder')}
              value={draft.code}
              // Kuzatuv kodlari katta harf bilan beriladi - kichik harfda
              // yozilgan kod ham topilishi kerak.
              onChange={(event) => setDraft({ ...draft, code: event.target.value.toUpperCase() })}
              sx={{ flex: '2 1 240px' }}
            />

            <TextField
              select
              size="small"
              label={t('register.filterStatus')}
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
              label={t('register.filterCategory')}
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
              label={t('register.filterFaculty')}
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
          </Stack>

          <Stack direction="row" spacing={2} sx={{ alignItems: 'center', mt: 2 }}>
            <Button type="submit" variant="contained" size="small">
              {t('common.filter')}
            </Button>
            <Button size="small" onClick={reset}>
              {t('common.reset')}
            </Button>
          </Stack>
        </form>
      </Card>

      <QueryState isPending={query.isPending} error={query.error}>
        {query.data && (
          <Paper variant="outlined">
            <Box sx={{ px: 2, py: 1.5, borderBottom: 1, borderColor: 'divider' }}>
              <Typography sx={listSecondaryText}>
                {t('register.found', { count: query.data.totalElements })}
              </Typography>
            </Box>

            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>{t('register.colCode')}</TableCell>
                    <TableCell>{t('register.colStatus')}</TableCell>
                    <TableCell>{t('register.colCategory')}</TableCell>
                    <TableCell>{t('register.colFaculty')}</TableCell>
                    <TableCell>{t('register.colReporter')}</TableCell>
                    <TableCell align="right">{t('register.colDate')}</TableCell>
                    <TableCell align="right">{t('register.colClosed')}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {query.data.content.map((entry) => (
                    <TableRow key={entry.trackingCode} hover>
                      <TableCell sx={{ whiteSpace: 'nowrap' }}>
                        <Box component="span" sx={codePill}>
                          {entry.trackingCode}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <StatusChip status={entry.status} label={entry.statusLabel} />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{entry.categoryLabel}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {entry.facultyName || '—'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {entry.reporterTypeLabel ?? '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                        <Typography sx={listSecondaryText}>{formatDate(entry.createdAt)}</Typography>
                      </TableCell>
                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                        <Typography sx={listSecondaryText}>
                          {entry.closedAt ? formatDate(entry.closedAt) : '—'}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}

                  {query.data.content.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                        <Typography variant="body2" color="text.secondary">
                          {t('register.empty')}
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
