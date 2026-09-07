import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { Box, Button, Card, CardContent, LinearProgress, Stack, Typography } from '@mui/material';
import { complaintsApi } from '../api/complaints';
import { statsApi } from '../api/stats';
import type { StatItem } from '../api/types';
import { formatDate, formatNumber } from '../lib/format';
import { AdminPage, QueryState, StatusChip } from './common';

function Tile({ label, value }: { label: string; value: string }) {
  return (
    <Card variant="outlined" sx={{ flex: '1 1 160px' }}>
      <CardContent>
        <Typography variant="h4" color="primary" sx={{ fontWeight: 600 }}>
          {value}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {label}
        </Typography>
      </CardContent>
    </Card>
  );
}

function Breakdown({ title, items }: { title: string; items: StatItem[] }) {
  const visible = items.filter((item) => item.count > 0);
  const max = Math.max(...visible.map((item) => item.count), 1);

  return (
    <Card variant="outlined" sx={{ flex: '1 1 320px' }}>
      <CardContent>
        <Typography variant="subtitle2" sx={{ mb: 2 }}>
          {title}
        </Typography>
        {visible.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            —
          </Typography>
        ) : (
          <Stack spacing={1.5}>
            {visible.map((item) => (
              <Box key={item.key}>
                <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2">{item.label}</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {formatNumber(item.count)}
                  </Typography>
                </Stack>
                <LinearProgress
                  variant="determinate"
                  value={(item.count / max) * 100}
                  sx={{ height: 6, borderRadius: 3 }}
                />
              </Box>
            ))}
          </Stack>
        )}
      </CardContent>
    </Card>
  );
}

export function DashboardPage() {
  const { t } = useTranslation();

  const stats = useQuery({ queryKey: ['stats', 'public'], queryFn: statsApi.publicStats });

  // Boshqaruv panelida eng muhimi - hali hech kim qaramagan murojaatlar.
  const unassigned = useQuery({
    queryKey: ['admin', 'complaints', 'unassigned'],
    queryFn: () => complaintsApi.search({ unassigned: true, size: 5 }),
  });

  return (
    <AdminPage title={t('admin.dashboardTitle')}>
      <QueryState isPending={stats.isPending} error={stats.error}>
        {stats.data && (
          <Stack spacing={3}>
            <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
              <Tile label={t('home.statTotal')} value={formatNumber(stats.data.total)} />
              <Tile label={t('home.statOpen')} value={formatNumber(stats.data.open)} />
              <Tile label={t('home.statResolved')} value={formatNumber(stats.data.resolved)} />
              <Tile label={t('home.statLast30')} value={formatNumber(stats.data.last30Days)} />
              <Tile
                label={t('stats.avgDays')}
                value={
                  stats.data.averageResolutionDays === null
                    ? t('stats.noAvg')
                    : t('stats.days', { count: stats.data.averageResolutionDays })
                }
              />
            </Stack>

            <Card variant="outlined">
              <CardContent>
                <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="subtitle2">{t('admin.unassignedOnly')}</Typography>
                  <Button component={Link} to="/admin/complaints" size="small">
                    {t('admin.complaintsTitle')}
                  </Button>
                </Stack>

                <QueryState isPending={unassigned.isPending} error={unassigned.error}>
                  {unassigned.data?.content.length === 0 ? (
                    <Typography variant="body2" color="text.secondary">
                      {t('common.noData')}
                    </Typography>
                  ) : (
                    <Stack spacing={1}>
                      {unassigned.data?.content.map((complaint) => (
                        <Stack
                          key={complaint.id}
                          component={Link}
                          to={`/admin/complaints/${complaint.id}`}
                          direction="row"
                          spacing={2}
                         
                          sx={{ alignItems: 'center',
                            p: 1.5,
                            borderRadius: 2,
                            textDecoration: 'none',
                            color: 'inherit',
                            '&:hover': { bgcolor: 'action.hover' },
                          }}
                        >
                          <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                            {complaint.trackingCode}
                          </Typography>
                          <Typography variant="body2" sx={{ flexGrow: 1 }} noWrap>
                            {complaint.title}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                            {formatDate(complaint.createdAt)}
                          </Typography>
                          <StatusChip status={complaint.status} label={complaint.statusLabel} />
                        </Stack>
                      ))}
                    </Stack>
                  )}
                </QueryState>
              </CardContent>
            </Card>

            <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
              <Breakdown title={t('stats.byStatus')} items={stats.data.byStatus} />
              <Breakdown title={t('stats.byCategory')} items={stats.data.byCategory} />
              <Breakdown title={t('stats.byFaculty')} items={stats.data.byFaculty} />
              <Breakdown title={t('stats.byReporter')} items={stats.data.byReporterType} />
            </Stack>
          </Stack>
        )}
      </QueryState>
    </AdminPage>
  );
}
