import { useEffect, useState } from 'react';
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
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { contentApi } from '../api/content';
import { LANGUAGES } from '../i18n';
import { formatDate, formatDateTime } from '../lib/format';
import { AdminPage, ConfirmDialog, MutationError, QueryState } from './common';
import {
  NewsBlockEditor,
  toEditorBlocks,
  toSavePayload,
  type EditorBlock,
} from './NewsBlockEditor';

/**
 * Yangilikning admin ko'rinishi va muharriri.
 *
 * <p>Sarlavha, qisqa mazmun va bloklar shu sahifaning o'zida tahrirlanadi:
 * blok qo'shish uzluksiz jarayon, uni oyna ichiga siqib bo'lmaydi.
 * O'zgarishlar faqat "Saqlash" bosilganda serverga yoziladi.
 */
export function NewsDetailAdminPage() {
  const { t } = useTranslation();
  const { id = '' } = useParams();
  const newsId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [language, setLanguage] = useState('uz');
  const [blocks, setBlocks] = useState<EditorBlock[]>([]);
  const [dirty, setDirty] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const query = useQuery({
    queryKey: ['admin', 'news', 'detail', newsId],
    queryFn: () => contentApi.adminNewsDetail(newsId),
    retry: false,
  });

  // Server ma'lumoti kelganda muharrir maydonlarini to'ldiramiz.
  // Saqlanmagan o'zgarish bo'lsa tegmaymiz - foydalanuvchi yozganini yo'qotmasin.
  const loaded = query.data;
  useEffect(() => {
    if (!loaded || dirty) return;
    setTitle(loaded.title);
    setSummary(loaded.summary ?? '');
    setLanguage(loaded.languageCode);
    setBlocks(toEditorBlocks(loaded.blocks));
  }, [loaded, dirty]);

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'news'] });
    void queryClient.invalidateQueries({ queryKey: ['news'] });
  }

  const save = useMutation({
    mutationFn: () =>
      contentApi.updateNews(newsId, {
        title,
        summary,
        language,
        blocks: toSavePayload(blocks),
      }),
    onSuccess: () => {
      setDirty(false);
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
            {/* --- Holat va amallar --- */}
            <Card variant="outlined">
              <CardContent>
                <Stack
                  direction={{ xs: 'column', md: 'row' }}
                  spacing={2}
                  sx={{ justifyContent: 'space-between' }}
                >
                  <Box sx={{ minWidth: 0 }}>
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

            {/* --- Muharrir --- */}
            <Card variant="outlined">
              <CardContent>
                <Stack spacing={2.5}>
                  <TextField
                    label={t('admin.fieldNewsTitle')}
                    value={title}
                    onChange={(event) => {
                      setTitle(event.target.value);
                      setDirty(true);
                    }}
                    required
                    fullWidth
                  />
                  <TextField
                    label={t('admin.fieldSummary')}
                    value={summary}
                    onChange={(event) => {
                      setSummary(event.target.value);
                      setDirty(true);
                    }}
                    multiline
                    rows={2}
                    fullWidth
                  />

                  <TextField
                    select
                    label={t('admin.fieldNewsLanguage')}
                    value={language}
                    onChange={(event) => {
                      setLanguage(event.target.value);
                      setDirty(true);
                    }}
                    sx={{ maxWidth: 260 }}
                  >
                    {LANGUAGES.map((item) => (
                      <MenuItem key={item.code} value={item.code}>
                        {item.name}
                      </MenuItem>
                    ))}
                  </TextField>

                  <Divider />

                  {/*
                    Tarjimalar. Har bir til alohida maqola bo'lgani uchun
                    ular shu yerdan ochiladi yoki shu yerdan yaratiladi -
                    muharrir qaysi tillar tayyor ekanini bir qarashda ko'radi.
                  */}
                  <Box>
                    <Typography variant="subtitle2" sx={{ mb: 1 }}>
                      {t('admin.newsTranslations')}
                    </Typography>
                    <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
                      {LANGUAGES.filter((item) => item.code !== news?.languageCode).map((item) => {
                        const existing = news?.translations.find(
                          (translation) => translation.languageCode === item.code,
                        );

                        return existing ? (
                          <Chip
                            key={item.code}
                            label={`${item.name}: ${existing.title}`}
                            color={existing.published ? 'success' : 'default'}
                            variant="outlined"
                            onClick={() => navigate(`/admin/news/${existing.id}`)}
                          />
                        ) : (
                          <Chip
                            key={item.code}
                            label={t('admin.newsAddTranslation', { language: item.name })}
                            variant="outlined"
                            onClick={() =>
                              navigate(`/admin/news?translationOf=${newsId}&language=${item.code}`)
                            }
                          />
                        );
                      })}
                    </Stack>
                  </Box>

                  <Box>
                    <Typography variant="subtitle2" sx={{ mb: 1 }}>
                      {t('admin.content')}
                    </Typography>
                    <NewsBlockEditor
                      value={blocks}
                      onChange={(next) => {
                        setBlocks(next);
                        setDirty(true);
                      }}
                    />
                  </Box>

                  <MutationError error={save.error} />

                  <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                    <Button
                      variant="contained"
                      disabled={save.isPending || !dirty}
                      onClick={() => save.mutate()}
                    >
                      {t('common.save')}
                    </Button>
                    {dirty && (
                      <Typography variant="caption" color="warning.main">
                        {t('admin.unsavedHint')}
                      </Typography>
                    )}
                    <Box sx={{ flexGrow: 1 }} />
                    <Typography variant="caption" color="text.secondary">
                      {t('track.updated')}: {formatDateTime(news.updatedAt)}
                    </Typography>
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          </Stack>
        )}
      </QueryState>

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
