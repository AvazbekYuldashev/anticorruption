import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { Box, Button, Card, CardContent, LinearProgress, Stack, Typography } from '@mui/material';
import InboxRounded from '@mui/icons-material/InboxRounded';
import HourglassTopRounded from '@mui/icons-material/HourglassTopRounded';
import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import CalendarMonthRounded from '@mui/icons-material/CalendarMonthRounded';
import TimerRounded from '@mui/icons-material/TimerRounded';
import DonutLargeRounded from '@mui/icons-material/DonutLargeRounded';
import CategoryRounded from '@mui/icons-material/CategoryRounded';
import SchoolRounded from '@mui/icons-material/SchoolRounded';
import BadgeRounded from '@mui/icons-material/BadgeRounded';
import { complaintsApi } from '../api/complaints';
import { statsApi } from '../api/stats';
import type { StatItem } from '../api/types';
import { formatDate, formatNumber } from '../lib/format';
import { AdminPage, QueryState, StatusChip } from './common';
import { breakdownAccents, hoverShadow, tones, type ToneName } from './theme';

function Tile({
  label,
  value,
  tone,
  icon,
}: {
  label: string;
  value: string;
  tone: ToneName;
  icon: ReactNode;
}) {
  const palette = tones[tone];

  return (
    <Card
      variant="outlined"
      sx={{
        flex: '1 1 180px',
        position: 'relative',
        overflow: 'hidden',
        backgroundImage: palette.wash,
        '&:hover': { transform: 'translateY(-2px)', boxShadow: hoverShadow },
      }}
    >
      {/* Yuqoridagi rangli chiziq - kartochkalar qatorini bir-biridan ajratadi. */}
      <Box
        aria-hidden
        sx={{ position: 'absolute', top: 0, left: 0, right: 0, height: 4, backgroundImage: palette.accent }}
      />
      <CardContent sx={{ pt: 2.75 }}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: palette.value, lineHeight: 1.15 }}>
              {value}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
              {label}
            </Typography>
          </Box>
          <Box
            aria-hidden
            sx={{
              flexShrink: 0,
              width: 38,
              height: 38,
              borderRadius: '50%',
              display: 'grid',
              placeItems: 'center',
              backgroundImage: palette.accent,
              color: palette.icon,
              boxShadow: '0 6px 14px -6px rgba(15, 23, 42, 0.45)',
              '& svg': { fontSize: 20 },
            }}
          >
            {icon}
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}

function Breakdown({
  title,
  items,
  index,
  icon,
}: {
  title: string;
  items: StatItem[];
  /** Bezak rangini tanlaydi - qiymatga emas, kartochka tartibiga bog'liq. */
  index: number;
  icon: ReactNode;
}) {
  const visible = items.filter((item) => item.count > 0);
  const max = Math.max(...visible.map((item) => item.count), 1);
  const accent = breakdownAccents[index % breakdownAccents.length];

  return (
    <Card variant="outlined" sx={{ flex: '1 1 320px', '&:hover': { boxShadow: hoverShadow } }}>
      <CardContent>
        <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center', mb: 2 }}>
          <Box
            aria-hidden
            sx={{
              width: 32,
              height: 32,
              borderRadius: 2,
              display: 'grid',
              placeItems: 'center',
              bgcolor: accent.tint,
              color: accent.chip,
              '& svg': { fontSize: 18 },
            }}
          >
            {icon}
          </Box>
          <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
            {title}
          </Typography>
        </Stack>

        {visible.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            —
          </Typography>
        ) : (
          <Stack spacing={1.75}>
            {visible.map((item) => (
              <Box key={item.key}>
                <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.75 }}>
                  <Typography variant="body2">{item.label}</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    {formatNumber(item.count)}
                  </Typography>
                </Stack>
                <LinearProgress
                  variant="determinate"
                  value={(item.count / max) * 100}
                  sx={{
                    height: 8,
                    borderRadius: 4,
                    bgcolor: 'rgba(148, 163, 184, 0.22)',
                    '& .MuiLinearProgress-bar': { borderRadius: 4, backgroundImage: accent.bar },
                  }}
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
              <Tile
                label={t('home.statTotal')}
                value={formatNumber(stats.data.total)}
                tone="brand"
                icon={<InboxRounded />}
              />
              <Tile
                label={t('home.statOpen')}
                value={formatNumber(stats.data.open)}
                tone="warning"
                icon={<HourglassTopRounded />}
              />
              <Tile
                label={t('home.statResolved')}
                value={formatNumber(stats.data.resolved)}
                tone="good"
                icon={<CheckCircleRounded />}
              />
              <Tile
                label={t('home.statLast30')}
                value={formatNumber(stats.data.last30Days)}
                tone="indigo"
                icon={<CalendarMonthRounded />}
              />
              <Tile
                label={t('stats.avgDays')}
                value={
                  stats.data.averageResolutionDays === null
                    ? t('stats.noAvg')
                    : t('stats.days', { count: stats.data.averageResolutionDays })
                }
                tone="slate"
                icon={<TimerRounded />}
              />
            </Stack>

            <Card variant="outlined">
              <CardContent>
                <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                    {t('admin.unassignedOnly')}
                  </Typography>
                  <Button component={Link} to="/admin/complaints" size="small">
                    {t('admin.complaintsTitle')}
                  </Button>
                </Stack>

                <QueryState isPending={unassigned.isPending} error={unassigned.error}>
                  {unassigned.data?.content.length === 0 ? (
                    /* Bo'sh ro'yxat bu yerda yomon xabar emas - hamma murojaat
                       biriktirilgan degani. Shuning uchun quruq matn emas,
                       tinch ko'rinishdagi belgi bilan ko'rsatiladi. */
                    <Stack spacing={1} sx={{ alignItems: 'center', py: 4 }}>
                      <Box
                        aria-hidden
                        sx={{
                          width: 48,
                          height: 48,
                          borderRadius: '50%',
                          display: 'grid',
                          placeItems: 'center',
                          bgcolor: 'rgba(12, 163, 12, 0.10)',
                          color: '#0ca30c',
                          '& svg': { fontSize: 26 },
                        }}
                      >
                        <CheckCircleRounded />
                      </Box>
                      <Typography variant="body2" color="text.secondary">
                        {t('common.noData')}
                      </Typography>
                    </Stack>
                  ) : (
                    <Stack spacing={1}>
                      {unassigned.data?.content.map((complaint) => (
                        <Stack
                          key={complaint.id}
                          component={Link}
                          to={`/admin/complaints/${complaint.id}`}
                          direction="row"
                          spacing={2}
                          sx={{
                            alignItems: 'center',
                            p: 1.5,
                            borderRadius: 2,
                            textDecoration: 'none',
                            color: 'inherit',
                            border: '1px solid transparent',
                            transition: 'background-color .18s ease, border-color .18s ease',
                            '&:hover': {
                              bgcolor: 'rgba(37, 99, 235, 0.06)',
                              borderColor: 'rgba(37, 99, 235, 0.22)',
                            },
                          }}
                        >
                          <Typography
                            variant="caption"
                            sx={{
                              fontFamily: 'monospace',
                              px: 1,
                              py: 0.25,
                              borderRadius: 1,
                              bgcolor: 'rgba(148, 163, 184, 0.16)',
                              color: 'text.secondary',
                              whiteSpace: 'nowrap',
                            }}
                          >
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
              <Breakdown
                title={t('stats.byStatus')}
                items={stats.data.byStatus}
                index={0}
                icon={<DonutLargeRounded />}
              />
              <Breakdown
                title={t('stats.byCategory')}
                items={stats.data.byCategory}
                index={1}
                icon={<CategoryRounded />}
              />
              <Breakdown
                title={t('stats.byFaculty')}
                items={stats.data.byFaculty}
                index={2}
                icon={<SchoolRounded />}
              />
              <Breakdown
                title={t('stats.byReporter')}
                items={stats.data.byReporterType}
                index={3}
                icon={<BadgeRounded />}
              />
            </Stack>
          </Stack>
        )}
      </QueryState>
    </AdminPage>
  );
}
