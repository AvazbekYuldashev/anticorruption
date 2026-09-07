import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Avatar,
  Box,
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
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import {
  contentApi,
  type SaveStaffPayload,
  type SaveStaffTranslation,
} from '../api/content';
import type { StaffTranslation } from '../api/types';
import { LANGUAGES } from '../i18n';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';
import { RichTextField } from './RichTextField';

const EMPTY: SaveStaffPayload = {
  fullName: '',
  position: '',
  academicDegree: '',
  biography: '',
  phone: '',
  email: '',
  receptionHours: '',
  displayOrder: 100,
  active: true,
};

/** Asosiy til: uning matni tarjima emas, xodim yozuvining o'zida turadi. */
const BASE_LANGUAGE = 'uz';

/** Faqat tarjima qilinadigan maydonlar. Telefon, email va surat tilga bog'liq emas. */
interface TranslationForm {
  fullName: string;
  position: string;
  academicDegree: string;
  biography: string;
  receptionHours: string;
}

const EMPTY_TRANSLATION: TranslationForm = {
  fullName: '',
  position: '',
  academicDegree: '',
  biography: '',
  receptionHours: '',
};

type TranslationMap = Record<string, TranslationForm>;

function toTranslationMap(translations: StaffTranslation[] = []): TranslationMap {
  const map: TranslationMap = {};

  for (const language of LANGUAGES) {
    if (language.code === BASE_LANGUAGE) continue;
    const found = translations.find((item) => item.languageCode === language.code);
    map[language.code] = {
      fullName: found?.fullName ?? '',
      position: found?.position ?? '',
      academicDegree: found?.academicDegree ?? '',
      biography: found?.biography ?? '',
      receptionHours: found?.receptionHours ?? '',
    };
  }
  return map;
}

/** Bo'sh varaq serverga yuborilmaydi. */
function toPayload(map: TranslationMap): SaveStaffTranslation[] {
  return Object.entries(map)
    .filter(([, value]) => Object.values(value).some((field) => field.trim() !== ''))
    .map(([languageCode, value]) => ({ languageCode, ...value }));
}

export function StaffAdminPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [dialog, setDialog] = useState<{
    id: number | null;
    form: SaveStaffPayload;
    translations: TranslationMap;
  } | null>(null);
  const [deleting, setDeleting] = useState<{ id: number; name: string } | null>(null);
  const [activeLanguage, setActiveLanguage] = useState<string>(
    LANGUAGES.find((language) => language.code !== BASE_LANGUAGE)!.code,
  );

  const query = useQuery({ queryKey: ['admin', 'staff'], queryFn: contentApi.adminStaff });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'staff'] });
    void queryClient.invalidateQueries({ queryKey: ['staff'] });
  }

  const save = useMutation({
    mutationFn: () => {
      const { id, form, translations } = dialog!;
      const payload: SaveStaffPayload = { ...form, translations: toPayload(translations) };

      return id === null ? contentApi.createStaff(payload) : contentApi.updateStaff(id, payload);
    },
    onSuccess: () => {
      setDialog(null);
      refresh();
    },
  });

  const uploadPhoto = useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => contentApi.uploadStaffPhoto(id, file),
    onSuccess: refresh,
  });

  const remove = useMutation({
    mutationFn: () => contentApi.deleteStaff(deleting!.id),
    onSuccess: () => {
      setDeleting(null);
      refresh();
    },
  });

  return (
    <AdminPage
      title={t('admin.staffTitle')}
      action={
        <Button variant="contained" onClick={() => setDialog({ id: null, form: EMPTY, translations: toTranslationMap() })}>
          {t('admin.addStaff')}
        </Button>
      }
    >
      <MutationError error={uploadPhoto.error ?? remove.error} />

      <QueryState isPending={query.isPending} error={query.error}>
        <Paper variant="outlined" sx={{ mt: 2 }}>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('admin.colName')}</TableCell>
                  <TableCell>{t('admin.fieldPosition')}</TableCell>
                  <TableCell>{t('admin.fieldReception')}</TableCell>
                  <TableCell>{t('admin.fieldOrder')}</TableCell>
                  <TableCell>{t('admin.colState')}</TableCell>
                  <TableCell align="right">{t('common.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {query.data?.map((member) => (
                  <TableRow key={member.id} hover>
                    <TableCell>
                      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                        <Avatar src={member.photoUrl ?? undefined} sx={{ width: 36, height: 36 }}>
                          {member.fullName.charAt(0)}
                        </Avatar>
                        <Box>
                          <Typography variant="body2">{member.fullName}</Typography>
                          {member.academicDegree && (
                            <Typography variant="caption" color="text.secondary">
                              {member.academicDegree}
                            </Typography>
                          )}
                        </Box>
                      </Stack>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {member.position}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {member.receptionHours ?? '—'}
                      </Typography>
                    </TableCell>
                    <TableCell>{member.displayOrder}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={member.active ? t('common.active') : t('common.inactive')}
                        color={member.active ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end' }}>
                        <Button
                          size="small"
                          onClick={() =>
                            setDialog({
                              id: member.id,
                              form: {
                                fullName: member.fullName,
                                position: member.position,
                                academicDegree: member.academicDegree ?? '',
                                biography: member.biography ?? '',
                                phone: member.phone ?? '',
                                email: member.email ?? '',
                                receptionHours: member.receptionHours ?? '',
                                displayOrder: member.displayOrder,
                                active: member.active,
                              },
                              translations: toTranslationMap(member.translations),
                            })
                          }
                        >
                          {t('common.edit')}
                        </Button>
                        <Button size="small" component="label">
                          {t('admin.uploadPhoto')}
                          <input
                            type="file"
                            hidden
                            accept="image/jpeg,image/png,image/webp"
                            onChange={(event) => {
                              const file = event.target.files?.[0];
                              if (file) uploadPhoto.mutate({ id: member.id, file });
                              event.target.value = '';
                            }}
                          />
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          onClick={() => setDeleting({ id: member.id, name: member.fullName })}
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
          title={dialog.id === null ? t('admin.addStaff') : t('admin.editStaff')}
          busy={save.isPending}
          error={save.error}
          onClose={() => setDialog(null)}
          onSubmit={() => save.mutate()}
        >
          <TextField
            label={t('admin.colName')}
            value={dialog.form.fullName}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, fullName: event.target.value } })
            }
            required
            fullWidth
          />
          <TextField
            label={t('admin.fieldPosition')}
            value={dialog.form.position}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, position: event.target.value } })
            }
            required
            fullWidth
          />
          <TextField
            label={t('admin.fieldDegree')}
            value={dialog.form.academicDegree}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, academicDegree: event.target.value } })
            }
            fullWidth
          />
          <RichTextField
            label={t('admin.fieldBiography')}
            value={dialog.form.biography ?? ''}
            onChange={(biography) => setDialog({ ...dialog, form: { ...dialog.form, biography } })}
            helperText={t('admin.biographyHint')}
            minRows={4}
          />
          <Stack direction="row" spacing={2}>
            <TextField
              label={t('staff.phone')}
              value={dialog.form.phone}
              onChange={(event) =>
                setDialog({ ...dialog, form: { ...dialog.form, phone: event.target.value } })
              }
              placeholder="+998901234567"
              fullWidth
            />
            <TextField
              label={t('staff.email')}
              type="email"
              value={dialog.form.email}
              onChange={(event) =>
                setDialog({ ...dialog, form: { ...dialog.form, email: event.target.value } })
              }
              fullWidth
            />
          </Stack>
          <TextField
            label={t('admin.fieldReception')}
            value={dialog.form.receptionHours}
            onChange={(event) =>
              setDialog({ ...dialog, form: { ...dialog.form, receptionHours: event.target.value } })
            }
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
                  checked={dialog.form.active ?? true}
                  onChange={(event) =>
                    setDialog({ ...dialog, form: { ...dialog.form, active: event.target.checked } })
                  }
                />
              }
              label={t('common.active')}
            />
          </Stack>

          {/*
            Tarjimalar. Faqat matn: telefon, email, surat va tartib tilga
            bog'liq emas va yuqorida bir marta kiritiladi. Bo'sh qoldirilgan
            maydon saytda asosiy tildagi matnni ko'rsatadi.
          */}
          <Box sx={{ mt: 1 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              {t('admin.newsTranslations')}
            </Typography>

            <Tabs
              value={activeLanguage}
              onChange={(_, value: string) => setActiveLanguage(value)}
              variant="scrollable"
              scrollButtons="auto"
            >
              {LANGUAGES.filter((language) => language.code !== BASE_LANGUAGE).map((language) => (
                <Tab key={language.code} value={language.code} label={language.name} />
              ))}
            </Tabs>

            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              {t('admin.translationFallbackHint')}
            </Typography>

            <Stack spacing={2} sx={{ mt: 2 }}>
              {(
                [
                  ['fullName', t('admin.colName')],
                  ['position', t('admin.fieldPosition')],
                  ['academicDegree', t('admin.fieldDegree')],
                  ['receptionHours', t('admin.fieldReception')],
                ] as const
              ).map(([field, label]) => (
                <TextField
                  key={field}
                  label={label}
                  value={dialog.translations[activeLanguage]?.[field] ?? ''}
                  onChange={(event) =>
                    setDialog({
                      ...dialog,
                      translations: {
                        ...dialog.translations,
                        [activeLanguage]: {
                          ...(dialog.translations[activeLanguage] ?? EMPTY_TRANSLATION),
                          [field]: event.target.value,
                        },
                      },
                    })
                  }
                  fullWidth
                />
              ))}

              <RichTextField
                label={t('admin.fieldBiography')}
                value={dialog.translations[activeLanguage]?.biography ?? ''}
                onChange={(biography) =>
                  setDialog({
                    ...dialog,
                    translations: {
                      ...dialog.translations,
                      [activeLanguage]: {
                        ...(dialog.translations[activeLanguage] ?? EMPTY_TRANSLATION),
                        biography,
                      },
                    },
                  })
                }
                minRows={3}
              />
            </Stack>
          </Box>
        </FormDialog>
      )}

      <ConfirmDialog
        open={deleting !== null}
        title={deleting?.name ?? ''}
        busy={remove.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => remove.mutate()}
      />
    </AdminPage>
  );
}
