import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
  const [album, setAlbum] = useState<{ id: number; title: string } | null>(null);

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

                          <Button size="small" onClick={() => setAlbum({ id: item.id, title: item.title })}>
                            {t('admin.manageAlbum')}
                            {item.imageCount > 0 && ` (${item.imageCount})`}
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

      {album && (
        <AlbumDialog
          newsId={album.id}
          title={album.title}
          onClose={() => {
            setAlbum(null);
            refresh();
          }}
        />
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

/**
 * Albom oynasi: mavjud rasmlarni ko'rsatadi, yangilarini qo'shadi va o'chiradi.
 *
 * <p>Yangilik ma'lumoti oyna ochilganda alohida so'raladi - ro'yxatda
 * faqat rasmlar soni bo'ladi, rasmlarning o'zi emas.
 */
function AlbumDialog({
  newsId,
  title,
  onClose,
}: {
  newsId: number;
  title: string;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['admin', 'news', 'detail', newsId],
    queryFn: () => contentApi.adminNewsDetail(newsId),
  });

  function reload() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'news', 'detail', newsId] });
  }

  const upload = useMutation({
    mutationFn: (files: File[]) => contentApi.uploadNewsImages(newsId, files),
    onSuccess: reload,
  });

  const removeImage = useMutation({
    mutationFn: (imageId: number) => contentApi.deleteNewsImage(newsId, imageId),
    onSuccess: reload,
  });

  const images = query.data?.images ?? [];

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        {t('admin.album')}
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          {title}
        </Typography>
      </DialogTitle>

      <DialogContent>
        <Button component="label" variant="contained" disabled={upload.isPending}>
          {upload.isPending ? t('admin.uploadingImages') : t('admin.addImages')}
          {/* multiple - bir vaqtda bir nechta fayl tanlash uchun */}
          <input
            type="file"
            hidden
            multiple
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => {
              const files = Array.from(event.target.files ?? []);
              if (files.length > 0) upload.mutate(files);
              event.target.value = '';
            }}
          />
        </Button>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          {t('admin.imagesHint')}
        </Typography>

        <MutationError error={upload.error ?? removeImage.error} />

        <Box sx={{ mt: 3 }}>
          <QueryState isPending={query.isPending} error={query.error}>
            {images.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                {t('admin.noImages')}
              </Typography>
            ) : (
              <Box
                sx={{
                  display: 'grid',
                  gap: 2,
                  gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
                }}
              >
                {images.map((image) => (
                  <Box key={image.id} sx={{ border: 1, borderColor: 'divider', borderRadius: 2, overflow: 'hidden' }}>
                    <Box
                      component="img"
                      src={image.url}
                      alt={image.originalName}
                      sx={{ width: '100%', height: 120, objectFit: 'cover', display: 'block' }}
                    />
                    <Box sx={{ p: 1 }}>
                      <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
                        {image.originalName}
                      </Typography>
                      <Button
                        size="small"
                        color="error"
                        fullWidth
                        disabled={removeImage.isPending}
                        onClick={() => removeImage.mutate(image.id)}
                      >
                        {t('common.delete')}
                      </Button>
                    </Box>
                  </Box>
                ))}
              </Box>
            )}
          </QueryState>
        </Box>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>{t('common.close')}</Button>
      </DialogActions>
    </Dialog>
  );
}
