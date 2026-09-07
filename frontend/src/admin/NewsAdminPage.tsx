import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Button,
  Chip,
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
  MenuItem,
} from '@mui/material';
import { contentApi, type SaveNewsPayload } from '../api/content';
import { LANGUAGES } from '../i18n';
import { formatDate } from '../lib/format';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

/**
 * Yaratishda faqat sarlavha va qisqa mazmun so'raladi. Mazmun bloklari
 * tafsilot sahifasida qo'shiladi - u yerda "+" bilan uzluksiz ishlash qulay.
 */
const EMPTY: SaveNewsPayload = {
  title: '',
  summary: '',
  published: false,
  language: 'uz',
  blocks: [],
};

export function NewsAdminPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [dialog, setDialog] = useState<{ form: SaveNewsPayload } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; title: string } | null>(null);
  const [params, setParams] = useSearchParams();

  /*
   * Tafsilot sahifasidagi "tarjima qo'shish" tugmasi shu sahifaga
   * ?translationOf=12&language=ru ko'rinishida qaytaradi. Shunda yaratish
   * oynasi darrov ochiladi va yangi yozuv o'sha maqolaning guruhiga ulanadi.
   */
  useEffect(() => {
    const translationOf = params.get('translationOf');
    if (!translationOf) return;

    setDialog({
      form: {
        ...EMPTY,
        language: params.get('language') ?? 'uz',
        translationOf: Number(translationOf),
      },
    });
    setParams({}, { replace: true });
  }, [params, setParams]);

  const query = useQuery({
    queryKey: ['admin', 'news', page, size],
    queryFn: () => contentApi.adminNews(page, size),
    placeholderData: keepPreviousData,
  });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'news'] });
    void queryClient.invalidateQueries({ queryKey: ['news'] });
  }

  const create = useMutation({
    mutationFn: () => contentApi.createNews(dialog!.form),
    onSuccess: (created) => {
      setDialog(null);
      refresh();
      // Yaratilgandan keyin darhol tafsilot sahifasiga: muqova va albom
      // aynan o'sha yerda qo'shiladi.
      navigate(`/admin/news/${created.id}`);
    },
  });

  const publish = useMutation({
    mutationFn: ({ id, published }: { id: number; published: boolean }) =>
      contentApi.publishNews(id, published),
    onSuccess: refresh,
  });

  const remove = useMutation({
    mutationFn: () => contentApi.deleteNews(deleting!.id),
    onSuccess: () => {
      setDeleting(null);
      refresh();
    },
  });

  return (
    <AdminPage
      title={t('admin.newsTitle')}
      action={
        <Button variant="contained" onClick={() => setDialog({ form: EMPTY })}>
          {t('admin.addNews')}
        </Button>
      }
    >
      <MutationError error={publish.error ?? remove.error} />

      <QueryState isPending={query.isPending} error={query.error}>
        {query.data && (
          <Paper variant="outlined" sx={{ mt: 2 }}>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>{t('admin.fieldNewsTitle')}</TableCell>
                    <TableCell>{t('admin.colStatus')}</TableCell>
                    <TableCell>{t('admin.colDate')}</TableCell>
                    <TableCell align="right">{t('common.actions')}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {query.data.content.map((item) => (
                    <TableRow
                      key={item.id}
                      hover
                      // Qatorning istalgan joyiga bosilsa tafsilot sahifasi ochiladi.
                      onClick={() => navigate(`/admin/news/${item.id}`)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell sx={{ maxWidth: 460 }}>
                        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                          {item.coverImageUrl && (
                            <img
                              src={item.coverImageUrl}
                              alt=""
                              style={{
                                width: 56,
                                height: 40,
                                objectFit: 'cover',
                                borderRadius: 6,
                              }}
                            />
                          )}
                          <Box sx={{ minWidth: 0 }}>
                            <Typography variant="body2">{item.title}</Typography>
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mt: 0.25 }}>
                              <Typography variant="caption" color="text.secondary">
                                /{item.slug}
                              </Typography>
                              <Chip size="small" label={item.languageCode} />
                              {item.imageCount > 0 && (
                                <Chip
                                  size="small"
                                  variant="outlined"
                                  label={t('news.imageCount', { count: item.imageCount })}
                                />
                              )}
                            </Stack>
                          </Box>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={item.published ? t('admin.published') : t('admin.draft')}
                          color={item.published ? 'success' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {formatDate(item.publishedAt)}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        {/*
                          Tugmalar qator ichida: bosilganda qatorning o'z
                          harakati (tafsilotga o'tish) ishlamasligi kerak.
                        */}
                        <Stack
                          direction="row"
                          spacing={0.5}
                          sx={{ justifyContent: 'flex-end' }}
                          onClick={(event) => event.stopPropagation()}
                        >
                          <Button
                            size="small"
                            onClick={() => publish.mutate({ id: item.id, published: !item.published })}
                          >
                            {item.published ? t('admin.unpublish') : t('admin.publish')}
                          </Button>
                          <Button
                            size="small"
                            color="error"
                            onClick={() => setDeleting({ id: item.id, title: item.title })}
                          >
                            {t('common.delete')}
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}

                  {query.data.content.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} align="center" sx={{ py: 5 }}>
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

      {dialog && (
        <FormDialog
          open
          maxWidth="md"
          title={t('admin.addNews')}
          busy={create.isPending}
          error={create.error}
          onClose={() => setDialog(null)}
          onSubmit={() => create.mutate()}
        >
          <TextField
            label={t('admin.fieldNewsTitle')}
            value={dialog.form.title}
            onChange={(event) =>
              setDialog({ form: { ...dialog.form, title: event.target.value } })
            }
            required
            fullWidth
          />
          <TextField
            label={t('admin.fieldSummary')}
            value={dialog.form.summary}
            onChange={(event) =>
              setDialog({ form: { ...dialog.form, summary: event.target.value } })
            }
            multiline
            rows={2}
            fullWidth
          />
          <TextField
            select
            label={t('admin.fieldNewsLanguage')}
            value={dialog.form.language ?? 'uz'}
            onChange={(event) =>
              setDialog({ form: { ...dialog.form, language: event.target.value } })
            }
            helperText={
              dialog.form.translationOf
                ? t('admin.newsTranslationHint')
                : t('admin.newsLanguageHint')
            }
            fullWidth
          >
            {LANGUAGES.map((language) => (
              <MenuItem key={language.code} value={language.code}>
                {language.name}
              </MenuItem>
            ))}
          </TextField>
          <Typography variant="caption" color="text.secondary">
            {t('admin.contentHint')}
          </Typography>
        </FormDialog>
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
