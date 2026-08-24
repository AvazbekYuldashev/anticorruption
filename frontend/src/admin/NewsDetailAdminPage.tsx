import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
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
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { contentApi, type SaveNewsPayload } from '../api/content';
import { Gallery } from '../components/Gallery';
import { formatDate, formatDateTime } from '../lib/format';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

/**
 * Yangilikning admin ko'rinishi: saytdagidek chiqadi, lekin qoralama
 * holatidagilar ham ko'rinadi va shu yerning o'zida boshqariladi.
 *
 * <p>Albom boshqaruvi ham shu yerda: ro'yxatdagi qatorga beshta tugma
 * sig'masdi, bu yerda esa rasm to'ri to'liq ko'rinadi.
 */
export function NewsDetailAdminPage() {
  const { t } = useTranslation();
  const { id = '' } = useParams();
  const newsId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState<SaveNewsPayload | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const query = useQuery({
    queryKey: ['admin', 'news', 'detail', newsId],
    queryFn: () => contentApi.adminNewsDetail(newsId),
    retry: false,
  });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'news'] });
    void queryClient.invalidateQueries({ queryKey: ['news'] });
  }

  const save = useMutation({
    mutationFn: () => contentApi.updateNews(newsId, editing!),
    onSuccess: () => {
      setEditing(null);
      refresh();
    },
  });

  const publish = useMutation({
    mutationFn: (published: boolean) => contentApi.publishNews(newsId, published),
    onSuccess: refresh,
  });

  const uploadCover = useMutation({
    mutationFn: (file: File) => contentApi.uploadNewsCover(newsId, file),
    onSuccess: refresh,
  });

  const uploadImages = useMutation({
    mutationFn: (files: File[]) => contentApi.uploadNewsImages(newsId, files),
    onSuccess: refresh,
  });

  const removeImage = useMutation({
    mutationFn: (imageId: number) => contentApi.deleteNewsImage(newsId, imageId),
    onSuccess: refresh,
  });

  const remove = useMutation({
    mutationFn: () => contentApi.deleteNews(newsId),
    onSuccess: () => {
      refresh();
      navigate('/admin/news', { replace: true });
    },
  });

  const news = query.data;

  return (
    <AdminPage
      title={t('admin.newsDetail')}
      action={
        <Button component={Link} to="/admin/news" size="small">
          ← {t('admin.newsTitle')}
        </Button>
      }
    >
      <QueryState isPending={query.isPending} error={query.error}>
        {news && (
          <Stack spacing={3}>
            {/* --- Boshqaruv --- */}
            <Card variant="outlined">
              <CardContent>
                <Stack
                  direction={{ xs: 'column', md: 'row' }}
                  spacing={2}
                  sx={{ justifyContent: 'space-between', alignItems: { md: 'flex-start' } }}
                >
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="h6">{news.title}</Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                      /{news.slug}
                    </Typography>
                    <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap' }}>
                      <Chip
                        size="small"
                        label={news.published ? t('admin.published') : t('admin.draft')}
                        color={news.published ? 'success' : 'default'}
                      />
                      <Chip size="small" variant="outlined" label={formatDate(news.publishedAt)} />
                      <Chip
                        size="small"
                        variant="outlined"
                        label={t('news.views', { count: news.viewCount })}
                      />
                    </Stack>
                  </Box>

                  <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                    <Button
                      size="small"
                      variant="contained"
                      onClick={() =>
                        setEditing({
                          title: news.title,
                          summary: news.summary ?? '',
                          body: news.body,
                          published: news.published,
                        })
                      }
                    >
                      {t('common.edit')}
                    </Button>
                    <Button
                      size="small"
                      disabled={publish.isPending}
                      onClick={() => publish.mutate(!news.published)}
                    >
                      {news.published ? t('admin.unpublish') : t('admin.publish')}
                    </Button>
                    {news.published && (
                      <Button
                        size="small"
                        component="a"
                        href={`/news/${news.slug}`}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {t('admin.openOnSite')}
                      </Button>
                    )}
                    <Button size="small" color="error" onClick={() => setConfirmDelete(true)}>
                      {t('common.delete')}
                    </Button>
                  </Stack>
                </Stack>

                {!news.published && (
                  <Alert severity="info" sx={{ mt: 2 }}>
                    {t('admin.draftHint')}
                  </Alert>
                )}

                <MutationError error={publish.error ?? remove.error} />
              </CardContent>
            </Card>

            {/* --- Muqova --- */}
            <Card variant="outlined">
              <CardContent>
                <Stack
                  direction="row"
                  spacing={2}
                  sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
                >
                  <Typography variant="subtitle2">{t('admin.cover')}</Typography>
                  <Button size="small" component="label" disabled={uploadCover.isPending}>
                    {news.coverImageUrl ? t('admin.replaceCover') : t('admin.uploadCover')}
                    <input
                      type="file"
                      hidden
                      accept="image/jpeg,image/png,image/webp"
                      onChange={(event) => {
                        const file = event.target.files?.[0];
                        if (file) uploadCover.mutate(file);
                        event.target.value = '';
                      }}
                    />
                  </Button>
                </Stack>

                {news.coverImageUrl ? (
                  <Box
                    component="img"
                    src={news.coverImageUrl}
                    alt=""
                    sx={{ width: '100%', maxHeight: 320, objectFit: 'cover', borderRadius: 2 }}
                  />
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    {t('admin.noCover')}
                  </Typography>
                )}

                <MutationError error={uploadCover.error} />
              </CardContent>
            </Card>

            {/* --- Matn --- */}
            <Card variant="outlined">
              <CardContent>
                {news.summary && (
                  <>
                    <Typography variant="subtitle2" gutterBottom>
                      {t('admin.fieldSummary')}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      {news.summary}
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                  </>
                )}

                <Typography variant="subtitle2" gutterBottom>
                  {t('admin.fieldBody')}
                </Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
                  {news.body}
                </Typography>

                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
                  {t('track.updated')}: {formatDateTime(news.updatedAt)}
                </Typography>
              </CardContent>
            </Card>

            {/* --- Albom --- */}
            <Card variant="outlined">
              <CardContent>
                <Stack
                  direction="row"
                  spacing={2}
                  sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}
                >
                  <Typography variant="subtitle2">{t('admin.album')}</Typography>
                  <Button
                    size="small"
                    variant="contained"
                    component="label"
                    disabled={uploadImages.isPending}
                  >
                    {uploadImages.isPending ? t('admin.uploadingImages') : t('admin.addImages')}
                    {/* multiple - bir vaqtda bir nechta fayl tanlash uchun */}
                    <input
                      type="file"
                      hidden
                      multiple
                      accept="image/jpeg,image/png,image/webp"
                      onChange={(event) => {
                        const files = Array.from(event.target.files ?? []);
                        if (files.length > 0) uploadImages.mutate(files);
                        event.target.value = '';
                      }}
                    />
                  </Button>
                </Stack>

                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                  {t('admin.imagesHint')}
                </Typography>

                <MutationError error={uploadImages.error ?? removeImage.error} />

                {news.images.length === 0 ? (
                  <Typography variant="body2" color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                    {t('admin.noImages')}
                  </Typography>
                ) : (
                  // Saytdagi bilan bir xil galereya: rasm bosilsa kattalashadi,
                  // ustidagi tugma esa uni o'chiradi.
                  <Gallery
                    images={news.images}
                    onDelete={(image) => removeImage.mutate(image.id)}
                    deleteDisabled={removeImage.isPending}
                  />
                )}
              </CardContent>
            </Card>
          </Stack>
        )}
      </QueryState>

      {editing && (
        <FormDialog
          open
          maxWidth="md"
          title={t('admin.editNews')}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(null)}
          onSubmit={() => save.mutate()}
        >
          <TextField
            label={t('admin.fieldNewsTitle')}
            value={editing.title}
            onChange={(event) => setEditing({ ...editing, title: event.target.value })}
            required
            fullWidth
          />
          <TextField
            label={t('admin.fieldSummary')}
            value={editing.summary}
            onChange={(event) => setEditing({ ...editing, summary: event.target.value })}
            multiline
            rows={2}
            fullWidth
          />
          <TextField
            label={t('admin.fieldBody')}
            value={editing.body}
            onChange={(event) => setEditing({ ...editing, body: event.target.value })}
            required
            multiline
            rows={12}
            fullWidth
          />
        </FormDialog>
      )}

      <ConfirmDialog
        open={confirmDelete}
        title={news?.title ?? ''}
        busy={remove.isPending}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => remove.mutate()}
      />
    </AdminPage>
  );
}
