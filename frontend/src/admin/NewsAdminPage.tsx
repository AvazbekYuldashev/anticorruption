import { useState } from 'react';
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
} from '@mui/material';
import { contentApi, type SaveNewsPayload } from '../api/content';
import { formatDate } from '../lib/format';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

const EMPTY: SaveNewsPayload = { title: '', summary: '', body: '', published: false };

export function NewsAdminPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [dialog, setDialog] = useState<{ id: number | null; form: SaveNewsPayload } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; title: string } | null>(null);

  const query = useQuery({
    queryKey: ['admin', 'news', page, size],
    queryFn: () => contentApi.adminNews(page, size),
    placeholderData: keepPreviousData,
  });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'news'] });
    void queryClient.invalidateQueries({ queryKey: ['news'] });
  }

  const save = useMutation({
    mutationFn: () => {
      const { id, form } = dialog!;
      return id === null ? contentApi.createNews(form) : contentApi.updateNews(id, form);
    },
    onSuccess: () => {
      setDialog(null);
      refresh();
    },
  });

  const publish = useMutation({
    mutationFn: ({ id, published }: { id: number; published: boolean }) =>
      contentApi.publishNews(id, published),
    onSuccess: refresh,
  });

  const uploadCover = useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => contentApi.uploadNewsCover(id, file),
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
        <Button variant="contained" onClick={() => setDialog({ id: null, form: EMPTY })}>
          {t('admin.addNews')}
        </Button>
      }
    >
      <MutationError error={publish.error ?? uploadCover.error ?? remove.error} />

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
                    <TableRow key={item.id} hover>
                      <TableCell sx={{ maxWidth: 420 }}>
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
                          <Box>
                            <Typography variant="body2">{item.title}</Typography>
                            <Typography variant="caption" color="text.secondary">
                              /{item.slug}
                            </Typography>
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
                        <Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                          <Button
                            size="small"
                            onClick={() =>
                              void contentApi.adminNewsDetail(item.id).then((detail) =>
                                setDialog({
                                  id: detail.id,
                                  form: {
                                    title: detail.title,
                                    summary: detail.summary ?? '',
                                    body: detail.body,
                                    published: detail.published,
                                  },
                                }),
                              )
                            }
                          >
                            {t('common.edit')}
                          </Button>

                          <Button
                            size="small"
                            onClick={() =>
                              publish.mutate({ id: item.id, published: !item.published })
                            }
                          >
                            {item.published ? t('admin.unpublish') : t('admin.publish')}
                          </Button>

                          <Button size="small" component="label">
                            {t('admin.uploadCover')}
                            <input
                              type="file"
                              hidden
                              accept="image/jpeg,image/png,image/webp"
                              onChange={(event) => {
                                const file = event.target.files?.[0];
                                if (file) uploadCover.mutate({ id: item.id, file });
                                event.target.value = '';
                              }}
                            />
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
          title={dialog.id === null ? t('admin.addNews') : t('admin.editNews')}
          busy={save.isPending}
          error={save.error}
          onClose={() => setDialog(null)}
          onSubmit={() => save.mutate()}
        >
          <TextField
            label={t('admin.fieldNewsTitle')}
            value={dialog.form.title}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, title: event.target.value } })
            }
            required
            fullWidth
          />
          <TextField
            label={t('admin.fieldSummary')}
            value={dialog.form.summary}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, summary: event.target.value } })
            }
            multiline
            rows={2}
            fullWidth
          />
          <TextField
            label={t('admin.fieldBody')}
            value={dialog.form.body}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, body: event.target.value } })
            }
            required
            multiline
            rows={12}
            fullWidth
          />
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
