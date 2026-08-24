import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Chip,
  FormControlLabel,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { contentApi, type SavePagePayload } from '../api/content';
import { formatDate } from '../lib/format';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

const EMPTY: SavePagePayload = {
  slug: '',
  title: '',
  body: '',
  displayOrder: 100,
  published: true,
};

export function PagesAdminPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [dialog, setDialog] = useState<{ id: number | null; form: SavePagePayload } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; title: string } | null>(null);

  const query = useQuery({ queryKey: ['admin', 'pages'], queryFn: contentApi.adminPages });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'pages'] });
    // Menyu ham shu ro'yxatdan yig'iladi.
    void queryClient.invalidateQueries({ queryKey: ['pages'] });
  }

  const save = useMutation({
    mutationFn: () => {
      const { id, form } = dialog!;
      return id === null ? contentApi.createPage(form) : contentApi.updatePage(id, form);
    },
    onSuccess: () => {
      setDialog(null);
      refresh();
    },
  });

  const remove = useMutation({
    mutationFn: () => contentApi.deletePage(deleting!.id),
    onSuccess: () => {
      setDeleting(null);
      refresh();
    },
  });

  return (
    <AdminPage
      title={t('admin.pagesTitle')}
      action={
        <Button variant="contained" onClick={() => setDialog({ id: null, form: EMPTY })}>
          {t('admin.addPage')}
        </Button>
      }
    >
      <MutationError error={remove.error} />

      <QueryState isPending={query.isPending} error={query.error}>
        <Paper variant="outlined" sx={{ mt: 2 }}>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('admin.fieldNewsTitle')}</TableCell>
                  <TableCell>{t('admin.fieldSlug')}</TableCell>
                  <TableCell>{t('admin.fieldOrder')}</TableCell>
                  <TableCell>{t('admin.colStatus')}</TableCell>
                  <TableCell>{t('admin.colDate')}</TableCell>
                  <TableCell align="right">{t('common.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {query.data?.map((page) => (
                  <TableRow key={page.id} hover>
                    <TableCell>{page.title}</TableCell>
                    <TableCell>
                      <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                        /{page.slug}
                      </Typography>
                    </TableCell>
                    <TableCell>{page.displayOrder}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={page.published ? t('admin.published') : t('admin.draft')}
                        color={page.published ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {formatDate(page.updatedAt)}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end' }}>
                        <Button
                          size="small"
                          onClick={() =>
                            setDialog({
                              id: page.id,
                              form: {
                                slug: page.slug,
                                title: page.title,
                                body: page.body ?? '',
                                displayOrder: page.displayOrder,
                                published: page.published,
                              },
                            })
                          }
                        >
                          {t('common.edit')}
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          onClick={() => setDeleting({ id: page.id, title: page.title })}
                        >
                          {t('common.delete')}
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}

                {query.data?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ py: 5 }}>
                      <Typography variant="body2" color="text.secondary">
                        {t('common.noData')}
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      </QueryState>

      {dialog && (
        <FormDialog
          open
          maxWidth="md"
          title={dialog.id === null ? t('admin.addPage') : t('admin.editPage')}
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
            label={t('admin.fieldSlug')}
            helperText={t('admin.slugHint')}
            value={dialog.form.slug}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, slug: event.target.value } })
            }
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
            rows={14}
            fullWidth
          />
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <TextField
              label={t('admin.fieldOrder')}
              type="number"
              value={dialog.form.displayOrder ?? 100}
              onChange={(event) =>
                setDialog({
                  ...dialog,
                  form: { ...dialog.form, displayOrder: Number(event.target.value) },
                })
              }
              sx={{ width: 140 }}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={dialog.form.published ?? true}
                  onChange={(event) =>
                    setDialog({
                      ...dialog,
                      form: { ...dialog.form, published: event.target.checked },
                    })
                  }
                />
              }
              label={t('admin.published')}
            />
          </Stack>
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
