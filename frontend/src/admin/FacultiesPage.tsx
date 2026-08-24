import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button,
  Chip,
  FormControlLabel,
  IconButton,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { universityApi } from '../api/university';
import type { DepartmentResponse, FacultyResponse } from '../api/types';
import { AdminPage, ConfirmDialog, FormDialog, MutationError, QueryState } from './common';

interface UnitForm {
  name: string;
  code: string;
  active: boolean;
}

const EMPTY_UNIT: UnitForm = { name: '', code: '', active: true };

export function FacultiesPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const query = useQuery({ queryKey: ['admin', 'faculties'], queryFn: universityApi.faculties });

  const [facultyDialog, setFacultyDialog] = useState<{ id: number | null; form: UnitForm } | null>(
    null,
  );
  const [departmentDialog, setDepartmentDialog] = useState<{
    facultyId: number;
    id: number | null;
    form: UnitForm;
  } | null>(null);
  const [deleting, setDeleting] = useState<
    { kind: 'faculty' | 'department'; id: number; name: string } | null
  >(null);

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'faculties'] });
    // Murojaat shakli ham shu ro'yxatdan to'ldiriladi.
    void queryClient.invalidateQueries({ queryKey: ['reference'] });
  }

  const saveFaculty = useMutation({
    mutationFn: () => {
      const { id, form } = facultyDialog!;
      return id === null
        ? universityApi.createFaculty(form)
        : universityApi.updateFaculty(id, form);
    },
    onSuccess: () => {
      setFacultyDialog(null);
      refresh();
    },
  });

  const saveDepartment = useMutation({
    mutationFn: () => {
      const { facultyId, id, form } = departmentDialog!;
      return id === null
        ? universityApi.createDepartment(facultyId, form)
        : universityApi.updateDepartment(id, form);
    },
    onSuccess: () => {
      setDepartmentDialog(null);
      refresh();
    },
  });

  const remove = useMutation({
    mutationFn: () =>
      deleting!.kind === 'faculty'
        ? universityApi.deleteFaculty(deleting!.id)
        : universityApi.deleteDepartment(deleting!.id),
    onSuccess: () => {
      setDeleting(null);
      refresh();
    },
  });

  return (
    <AdminPage
      title={t('admin.facultiesTitle')}
      description={t('admin.facultiesIntro')}
      action={
        <Button
          variant="contained"
          onClick={() => setFacultyDialog({ id: null, form: EMPTY_UNIT })}
        >
          {t('admin.addFaculty')}
        </Button>
      }
    >
      {/* O'chirish taqiqlanganda backend sababini tushuntirib beradi. */}
      <MutationError error={remove.error} />

      <QueryState isPending={query.isPending} error={query.error}>
        <Stack spacing={1} sx={{ mt: 2 }}>
          {query.data?.map((faculty) => (
            <FacultyRow
              key={faculty.id}
              faculty={faculty}
              onEdit={() =>
                setFacultyDialog({
                  id: faculty.id,
                  form: { name: faculty.name, code: faculty.code, active: faculty.active },
                })
              }
              onDelete={() =>
                setDeleting({ kind: 'faculty', id: faculty.id, name: faculty.name })
              }
              onAddDepartment={() =>
                setDepartmentDialog({ facultyId: faculty.id, id: null, form: EMPTY_UNIT })
              }
              onEditDepartment={(department) =>
                setDepartmentDialog({
                  facultyId: faculty.id,
                  id: department.id,
                  form: {
                    name: department.name,
                    code: department.code,
                    active: department.active,
                  },
                })
              }
              onDeleteDepartment={(department) =>
                setDeleting({ kind: 'department', id: department.id, name: department.name })
              }
            />
          ))}

          {query.data?.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
              {t('common.noData')}
            </Typography>
          )}
        </Stack>
      </QueryState>

      {/* Fakultet oynasi */}
      {facultyDialog && (
        <FormDialog
          open
          title={facultyDialog.id === null ? t('admin.addFaculty') : t('admin.editFaculty')}
          busy={saveFaculty.isPending}
          error={saveFaculty.error}
          onClose={() => setFacultyDialog(null)}
          onSubmit={() => saveFaculty.mutate()}
        >
          <UnitFields
            form={facultyDialog.form}
            onChange={(form) => setFacultyDialog({ ...facultyDialog, form })}
          />
        </FormDialog>
      )}

      {/* Kafedra oynasi */}
      {departmentDialog && (
        <FormDialog
          open
          title={departmentDialog.id === null ? t('admin.addDepartment') : t('admin.editDepartment')}
          busy={saveDepartment.isPending}
          error={saveDepartment.error}
          onClose={() => setDepartmentDialog(null)}
          onSubmit={() => saveDepartment.mutate()}
        >
          <UnitFields
            form={departmentDialog.form}
            onChange={(form) => setDepartmentDialog({ ...departmentDialog, form })}
          />
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

function UnitFields({ form, onChange }: { form: UnitForm; onChange: (form: UnitForm) => void }) {
  const { t } = useTranslation();

  return (
    <>
      <TextField
        label={t('admin.fieldName')}
        value={form.name}
        onChange={(event) => onChange({ ...form, name: event.target.value })}
        required
        fullWidth
      />
      <TextField
        label={t('admin.fieldCode')}
        value={form.code}
        onChange={(event) => onChange({ ...form, code: event.target.value.toUpperCase() })}
        required
        fullWidth
        slotProps={{ htmlInput: { maxLength: 20, style: { textTransform: 'uppercase' } } }}
      />
      <FormControlLabel
        control={
          <Switch
            checked={form.active}
            onChange={(event) => onChange({ ...form, active: event.target.checked })}
          />
        }
        label={t('common.active')}
      />
    </>
  );
}

function FacultyRow({
  faculty,
  onEdit,
  onDelete,
  onAddDepartment,
  onEditDepartment,
  onDeleteDepartment,
}: {
  faculty: FacultyResponse;
  onEdit: () => void;
  onDelete: () => void;
  onAddDepartment: () => void;
  onEditDepartment: (department: DepartmentResponse) => void;
  onDeleteDepartment: (department: DepartmentResponse) => void;
}) {
  const { t } = useTranslation();

  return (
    <Accordion variant="outlined" disableGutters>
      <AccordionSummary>
        <Stack
          direction="row"
          spacing={2}
         
          sx={{ alignItems: 'center', flexWrap: 'wrap', width: '100%', pr: 2 }}
         
        >
          <Chip size="small" label={faculty.code} />
          <Typography sx={{ flexGrow: 1 }}>{faculty.name}</Typography>
          {!faculty.active && <Chip size="small" label={t('common.inactive')} color="default" />}
          <Typography variant="caption" color="text.secondary">
            {t('admin.departments')}: {faculty.departments.length}
          </Typography>
        </Stack>
      </AccordionSummary>

      <AccordionDetails>
        <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
          <Button size="small" onClick={onEdit}>
            {t('common.edit')}
          </Button>
          <Button size="small" color="error" onClick={onDelete}>
            {t('common.delete')}
          </Button>
          <Button size="small" variant="outlined" onClick={onAddDepartment}>
            {t('admin.addDepartment')}
          </Button>
        </Stack>

        {faculty.departments.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            {t('admin.noDepartments')}
          </Typography>
        ) : (
          <Stack spacing={0.5}>
            {faculty.departments.map((department) => (
              <Stack
                key={department.id}
                direction="row"
                spacing={2}
               
                sx={{ alignItems: 'center', py: 0.5, borderBottom: 1, borderColor: 'divider' }}
              >
                <Chip size="small" label={department.code} variant="outlined" />
                <Typography variant="body2" sx={{ flexGrow: 1 }}>
                  {department.name}
                </Typography>
                {!department.active && (
                  <Chip size="small" label={t('common.inactive')} />
                )}
                <IconButton size="small" onClick={() => onEditDepartment(department)}>
                  <Typography variant="caption">{t('common.edit')}</Typography>
                </IconButton>
                <IconButton size="small" color="error" onClick={() => onDeleteDepartment(department)}>
                  <Typography variant="caption">{t('common.delete')}</Typography>
                </IconButton>
              </Stack>
            ))}
          </Stack>
        )}
      </AccordionDetails>
    </Accordion>
  );
}
