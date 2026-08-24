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
import { contentApi, type SaveLinkPayload } from '../api/content';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

const EMPTY: SaveLinkPayload = {
  title: '',
  url: '',
  description: '',
  groupName: '',
  displayOrder: 100,
  active: true,
};

export function LinksAdminPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [dialog, setDialog] = useState<{ id: number | null; form: SaveLinkPayload } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; title: string } | null>(null);

  const query = useQuery({ queryKey: ['admin', 'links'], queryFn: contentApi.adminLinks });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'links'] });
    void queryClient.invalidateQueries({ queryKey: ['links'] });
  }

  const save = useMutation({
    mutationFn: () => {
      const { id, form } = dialog!;
      return id === null ? contentApi.createLink(form) : contentApi.updateLink(id, form);
    },
    onSuccess: () => {
      setDialog(null);
      refresh();
    },
  });

  const remove = useMutation({
    mutationFn: () => contentApi.deleteLink(deleting!.id),
    onSuccess: () => {
      setDeleting(null);
      refresh();
    },
  });

  return (
    <AdminPage
      title={t('admin.linksTitle')}
      action={
        <Button variant="contained" onClick={() => setDialog({ id: null, form: EMPTY })}>
          {t('admin.addLink')}
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
                  <TableCell>{t('admin.fieldUrl')}</TableCell>
                  <TableCell>{t('admin.fieldGroup')}</TableCell>
                  <TableCell>{t('admin.fieldOrder')}</TableCell>
                  <TableCell>{t('admin.colState')}</TableCell>
                  <TableCell align="right">{t('common.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {query.data?.map((link) => (
                  <TableRow key={link.id} hover>
                    <TableCell>{link.title}</TableCell>
                    <TableCell sx={{ maxWidth: 280 }}>
                      <Typography variant="caption" color="text.secondary" noWrap component="div">
                        {link.url}
                      </Typography>
                    </TableCell>
                    <TableCell>{link.groupName ?? '—'}</TableCell>
                    <TableCell>{link.displayOrder}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={link.active ? t('common.active') : t('common.inactive')}
                        color={link.active ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end' }}>
                        <Button
                          size="small"
                          onClick={() =>
                            setDialog({
                              id: link.id,
                              form: {
                                title: link.title,
                                url: link.url,
                                description: link.description ?? '',
                                groupName: link.groupName ?? '',
                                displayOrder: link.displayOrder,
                                active: link.active,
                              },
                            })
                          }
                        >
                          {t('common.edit')}
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          onClick={() => setDeleting({ id: link.id, title: link.title })}
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
          title={dialog.id === null ? t('admin.addLink') : t('admin.editLink')}
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
            label={t('admin.fieldUrl')}
            value={dialog.form.url}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, url: event.target.value } })
            }
            placeholder="https://"
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
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <TextField
              label={t('admin.fieldGroup')}
              value={dialog.form.groupName}
              onChange={(event) =>
                setDialog({ ...dialog, form: { ...dialog.form, groupName: event.target.value } })
              }
              fullWidth
            />
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
          </Stack>
          <FormControlLabel
            control={
              <Switch
                checked={dialog.form.active ?? true}
                onChange={(event) =>
                  setDialog({ ...dialog, form: { ...dialog.form, active: event.target.checked } })
                }
              />
            }
            label={t('common.active')}
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
