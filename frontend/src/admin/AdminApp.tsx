import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, NavLink, Route, Routes, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  AppBar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ScopedCssBaseline,
  ThemeProvider,
  Toolbar,
  Typography,
  createTheme,
} from '@mui/material';
import SpaceDashboardRounded from '@mui/icons-material/SpaceDashboardRounded';
import DescriptionRounded from '@mui/icons-material/DescriptionRounded';
import ListAltRounded from '@mui/icons-material/ListAltRounded';
import NewspaperRounded from '@mui/icons-material/NewspaperRounded';
import GroupsRounded from '@mui/icons-material/GroupsRounded';
import InfoRounded from '@mui/icons-material/InfoRounded';
import HomeRounded from '@mui/icons-material/HomeRounded';
import PollRounded from '@mui/icons-material/PollRounded';
import QuizRounded from '@mui/icons-material/QuizRounded';
import ManageAccountsRounded from '@mui/icons-material/ManageAccountsRounded';
import AccountBalanceRounded from '@mui/icons-material/AccountBalanceRounded';
import LaunchRounded from '@mui/icons-material/LaunchRounded';
import MenuRounded from '@mui/icons-material/MenuRounded';
import LogoutRounded from '@mui/icons-material/LogoutRounded';

import { useAuth } from '../auth/AuthContext';
import { setLightOnly } from '../lib/theme';
import { LanguageSwitcher } from '../components/LanguageSwitcher';
import { ProtectedRoute } from '../auth/ProtectedRoute';

import { DashboardPage } from './DashboardPage';
import { AdminComplaintsPage } from './ComplaintsPage';
import { AdminComplaintDetailPage } from './ComplaintDetailPage';
import { RegisterAdminPage } from './RegisterAdminPage';
import { UsersPage } from './UsersPage';
import { FacultiesPage } from './FacultiesPage';
import { NewsAdminPage } from './NewsAdminPage';
import { NewsDetailAdminPage } from './NewsDetailAdminPage';
import { StaffAdminPage } from './StaffAdminPage';
import { AboutAdminPage } from './AboutAdminPage';
import { HomeTextsAdminPage } from './HomeTextsAdminPage';
import { ProfilePage } from './ProfilePage';
import { PollsAdminPage, QuizzesAdminPage } from './PollsAdminPage';
import {
  brand,
  canvasBackground,
  chipTones,
  motion,
  noMotion,
  sidebarBackground,
  softShadow,
  status,
  tableHeadBackground,
} from './theme';

const DRAWER_WIDTH = 264;

/*
 * Admin uchun alohida MUI mavzusi. Ommaviy sayt bilan bir xil brand shkalasi
 * (index.css dagi --color-brand-*) ishlatiladi, shunda ikki qism bir tizimning
 * bo'lagi ekani seziladi: o'sha ko'k gradient, o'sha yumshoq soyalar.
 */
const theme = createTheme({
  palette: {
    primary: { main: brand[600], dark: brand[800], light: brand[400] },
    // Ma'noli ranglar loyihaning o'z tokenlariga bog'lanadi, MUI ning
    // standart yashil/qizillari emas.
    success: { main: status.good },
    warning: { main: status.warning },
    secondary: { main: status.serious },
    error: { main: status.critical },
    background: { default: 'transparent', paper: '#ffffff' },
    text: { primary: '#1e293b', secondary: '#64748b' },
    divider: 'rgba(148, 163, 184, 0.26)',
  },
  shape: { borderRadius: 14 },
  typography: {
    fontFamily: "'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif",
    button: { textTransform: 'none', fontWeight: 500 },
    h5: { fontWeight: 700, letterSpacing: '-0.02em' },
    h4: { fontWeight: 700, letterSpacing: '-0.02em' },
  },
  components: {
    /*
     * Kartochkalar hamma sahifada `variant="outlined"` bilan chaqiriladi -
     * MUI unga soyasiz, quruq ramka beradi. Ramkani yumshatib, o'rniga
     * ozgina soya qo'yiladi: kartochka fondan ajralib turadi, lekin
     * ekranda 20 tasi bo'lsa ham shovqin qilmaydi.
     */
    MuiPaper: {
      styleOverrides: {
        outlined: {
          border: '1px solid rgba(148, 163, 184, 0.22)',
          boxShadow: softShadow,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          transition: `${motion.quick}, ${motion.soft}`,
          ...noMotion,
        },
      },
    },
    /*
     * Bosiladigan elementlar javob qaytaradi: ustiga borilganda ozgina
     * ko'tariladi, bosilganda joyiga tushadi.
     */
    MuiButton: {
      styleOverrides: {
        root: {
          transition: `${motion.quick}, ${motion.soft}`,
          '&:hover': { transform: 'translateY(-1px)' },
          '&:active': { transform: 'translateY(0)' },
          '&.Mui-disabled': { transform: 'none' },
          ...noMotion,
        },
      },
      variants: [
        {
          props: { variant: 'contained', color: 'primary' },
          style: {
            boxShadow: '0 6px 16px -6px rgba(29, 78, 216, 0.6)',
            backgroundImage: `linear-gradient(135deg, ${brand[600]}, ${brand[500]})`,
            '&:hover': { boxShadow: '0 10px 22px -8px rgba(29, 78, 216, 0.7)' },
          },
        },
      ],
    },
    /*
     * Jadvallar admin panelining asosiy ekrani. Standart MUI jadvalida
     * sarlavha qatori oddiy matndan farq qilmaydi va ustunlar bir-biriga
     * qo'shilib ketadi - shuning uchun sarlavhaga o'z tasmasi beriladi.
     */
    MuiTableHead: {
      styleOverrides: {
        root: { backgroundImage: tableHeadBackground },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderBottomColor: 'rgba(148, 163, 184, 0.20)' },
        head: {
          fontSize: 11.5,
          fontWeight: 700,
          letterSpacing: '0.06em',
          textTransform: 'uppercase',
          color: '#475569',
          whiteSpace: 'nowrap',
          backgroundColor: 'transparent',
          // Ko'kimtir chegara sarlavha tasmasini ma'lumotdan qat'iy ajratadi.
          borderBottom: '2px solid rgba(37, 99, 235, 0.28)',
        },
        /*
         * Qatorlar nafas olsin va raqamlar ustma-ust tushsin: sana va
         * sonlar ustunida har xil kenglikdagi raqamlar qatorni "sakratib"
         * yuborardi.
         */
        body: {
          paddingTop: 14,
          paddingBottom: 14,
          fontSize: 14,
          lineHeight: 1.5,
          color: '#334155',
          fontVariantNumeric: 'tabular-nums',
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          // Oxirgi qatorning chizig'i kartochka chekkasi bilan qo'shaloq
          // bo'lib ko'rinardi.
          '&:last-of-type td': { borderBottom: 0 },
          '&.MuiTableRow-hover:hover': { backgroundColor: 'rgba(37, 99, 235, 0.05)' },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          transition: `${motion.quick}, ${motion.soft}`,
          '&:hover': { transform: 'scale(1.08)' },
          '&:active': { transform: 'scale(0.95)' },
          ...noMotion,
        },
      },
    },
    /*
     * Nishonlar quyuq to'ldirilgan emas, yumshoq: jadvalda o'nlab nishon
     * bo'lganda to'yingan ranglar ma'lumotni bosib ketadi.
     */
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 8, transition: `${motion.quick}, ${motion.soft}` },
        // Faqat bosiladigan nishon harakatlanadi - qolganlari yorliq, tugma emas.
        clickable: { '&:hover': { transform: 'translateY(-1px)' }, ...noMotion },
      },
      variants: chipTones.map((tone) => ({
        props: { variant: 'filled' as const, color: tone.color },
        style: {
          backgroundColor: tone.bg,
          color: tone.fg,
          '& .MuiChip-deleteIcon': { color: tone.fg },
        },
      })),
    },
    MuiTabs: {
      styleOverrides: {
        indicator: { height: 3, borderRadius: 3 },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: { fontWeight: 600, minHeight: 44 },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: brand[300] },
        },
        notchedOutline: { borderColor: 'rgba(148, 163, 184, 0.42)' },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 999, backgroundColor: 'rgba(148, 163, 184, 0.22)' },
        bar: { borderRadius: 999 },
      },
      /*
       * Gradient faqat asosiy rangga: so'rovnomalarda to'g'ri javob chizig'i
       * `color="success"` bilan yashil chiziladi va u yashilligicha qolishi
       * kerak.
       */
      variants: [
        {
          props: { color: 'primary' as const },
          style: {
            '& .MuiLinearProgress-bar': {
              backgroundImage: `linear-gradient(90deg, ${brand[700]}, ${brand[500]})`,
            },
          },
        },
      ],
    },
    MuiAvatar: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          color: '#ffffff',
          backgroundImage: `linear-gradient(135deg, ${brand[800]}, ${brand[500]})`,
        },
      },
    },
    MuiAccordion: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          '&::before': { display: 'none' },
          '&.Mui-expanded': { margin: 0 },
        },
      },
    },
  },
});

type MenuItem = { to: string; label: string; icon: ReactNode; end?: boolean };

function useMenuItems(): MenuItem[] {
  const { t } = useTranslation();
  const { isAdmin } = useAuth();

  const items: MenuItem[] = [
    { to: '/admin', label: t('admin.navDashboard'), icon: <SpaceDashboardRounded />, end: true },
    { to: '/admin/complaints', label: t('admin.navComplaints'), icon: <DescriptionRounded /> },
    /*
      Murojaatlar reyestri faqat shu yerda: ommaviy saytda ham, uning
      menyusida ham yo'q. Bu xodimlarning ish ro'yxati - backend ham uni
      faqat moderator va administratorga beradi.
    */
    { to: '/admin/register', label: t('admin.navRegister'), icon: <ListAltRounded /> },
    { to: '/admin/home', label: t('admin.navHomeTexts'), icon: <HomeRounded /> },
    { to: '/admin/news', label: t('admin.navNews'), icon: <NewspaperRounded /> },
    { to: '/admin/staff', label: t('admin.navStaff'), icon: <GroupsRounded /> },
    { to: '/admin/about', label: t('admin.navAbout'), icon: <InfoRounded /> },
    { to: '/admin/polls', label: t('admin.navPolls'), icon: <PollRounded /> },
    { to: '/admin/tests', label: t('admin.navTests'), icon: <QuizRounded /> },
  ];

  // Foydalanuvchilar va tuzilma faqat administratorga ochiq -
  // backend ham aynan shu chegarani qo'yadi.
  if (isAdmin) {
    items.push(
      { to: '/admin/users', label: t('admin.navUsers'), icon: <ManageAccountsRounded /> },
      { to: '/admin/faculties', label: t('admin.navFaculties'), icon: <AccountBalanceRounded /> },
    );
  }

  return items;
}

/*
 * Yon menyu to'q ko'k gradientda: ekranning chap chekkasi shu tufayli
 * "boshqaruv" hissini beradi va oq ish maydonini ramkalaydi. Ranglar
 * ommaviy saytning bosh ekrani (brand-900 -> brand-700) bilan bir xil.
 */
const navItemStyles = {
  mx: 1.25,
  my: 0.25,
  px: 1.5,
  borderRadius: 2.5,
  color: 'rgba(226, 232, 240, 0.78)',
  position: 'relative',
  transition: `background-color .18s ease, color .18s ease, ${motion.quick}`,
  '& .MuiListItemIcon-root': { color: 'inherit', minWidth: 36 },
  // Kichik siljish: bo'lim "chaqirayotgandek" bo'ladi.
  '&:hover': {
    bgcolor: 'rgba(255, 255, 255, 0.08)',
    color: '#ffffff',
    transform: 'translateX(3px)',
  },
  ...noMotion,
  '&.active': {
    bgcolor: 'rgba(255, 255, 255, 0.16)',
    color: '#ffffff',
    boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.12)',
    // Faol bo'limni rang bilan birga shakl ham ko'rsatadi.
    '&::before': {
      content: '""',
      position: 'absolute',
      left: 4,
      top: '50%',
      transform: 'translateY(-50%)',
      width: 3,
      height: 20,
      borderRadius: 3,
      bgcolor: '#93c5fd',
    },
  },
} as const;

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation();
  const items = useMenuItems();

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ gap: 1.5, py: 1 }}>
        <Box
          component="img"
          src="/brand/asti-logo-64.png"
          alt=""
          sx={{
            width: 38,
            height: 38,
            flexShrink: 0,
            p: 0.5,
            borderRadius: 2,
            bgcolor: 'rgba(255, 255, 255, 0.12)',
          }}
        />
        <Box sx={{ minWidth: 0 }}>
          <Typography
            variant="caption"
            noWrap
            sx={{ display: 'block', color: 'rgba(191, 219, 254, 0.75)' }}
          >
            {t('site.shortName')}
          </Typography>
          <Typography variant="subtitle2" noWrap sx={{ fontWeight: 700, color: '#ffffff' }}>
            {t('admin.title')}
          </Typography>
        </Box>
      </Toolbar>
      <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.12)' }} />

      <List sx={{ py: 1.5, flexGrow: 1 }}>
        {items.map((item) => (
          <ListItemButton
            key={item.to}
            component={NavLink}
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            sx={navItemStyles}
          >
            <ListItemIcon sx={{ '& svg': { fontSize: 20 } }}>{item.icon}</ListItemIcon>
            <ListItemText
              primary={item.label}
              slotProps={{ primary: { sx: { fontSize: 14, fontWeight: 500 } } }}
            />
          </ListItemButton>
        ))}
      </List>

      <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.12)' }} />
      <List sx={{ py: 1 }}>
        <ListItemButton component={Link} to="/" onClick={onNavigate} sx={navItemStyles}>
          <ListItemIcon sx={{ '& svg': { fontSize: 20 } }}>
            <LaunchRounded />
          </ListItemIcon>
          <ListItemText
            primary={t('admin.backToSite')}
            slotProps={{ primary: { sx: { fontSize: 14 } } }}
          />
        </ListItemButton>
      </List>
    </Box>
  );
}

const drawerPaperStyles = {
  width: DRAWER_WIDTH,
  boxSizing: 'border-box',
  border: 'none',
  color: '#e2e8f0',
  backgroundImage: sidebarBackground,
} as const;

export function AdminApp() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();

  // Admin panel o'z (yorug') ranglariga ega: saytda qorong'i rejim tanlangan
  // bo'lsa ham bu yerda u o'chadi va saytga qaytilganda tiklanadi.
  useEffect(() => {
    setLightOnly(true);
    return () => setLightOnly(false);
  }, []);

  return (
    <ThemeProvider theme={theme}>
      {/* Global emas, faqat shu bo'lim uchun: ommaviy saytdagi Tailwind
          uslublariga tegmasligi kerak. */}
      <ScopedCssBaseline
        sx={{
          minHeight: '100vh',
          bgcolor: '#f4f7fd',
          // Yumshoq ko'k yorug'lik: tekis kulrang fon "tugallanmagan"
          // ko'rinardi, gradient esa ish maydoniga chuqurlik beradi.
          backgroundImage: canvasBackground,
          backgroundAttachment: 'fixed',
        }}
      >
        <Box sx={{ display: 'flex' }}>
          <AppBar
            position="fixed"
            color="inherit"
            elevation={0}
            sx={{
              // Yarim shaffof + blur: sahifa aylanganda kontent panel ostidan
              // ko'rinib turadi, chegara esa uni ish maydonidan ajratadi.
              bgcolor: 'rgba(255, 255, 255, 0.82)',
              backdropFilter: 'blur(12px)',
              borderBottom: 1,
              borderColor: 'divider',
              width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
              ml: { md: `${DRAWER_WIDTH}px` },
            }}
          >
            <Toolbar sx={{ gap: 2 }}>
              <IconButton
                edge="start"
                onClick={() => setMobileOpen(true)}
                sx={{ display: { md: 'none' } }}
                aria-label={t('nav.menu')}
              >
                <MenuRounded />
              </IconButton>

              <Box sx={{ flexGrow: 1 }} />

              <Box
                sx={{
                  borderRadius: 2,
                  px: 0.5,
                  py: 0.25,
                  backgroundImage: `linear-gradient(135deg, ${brand[700]}, ${brand[500]})`,
                  boxShadow: '0 4px 12px -4px rgba(29, 78, 216, 0.55)',
                }}
              >
                <LanguageSwitcher compact />
              </Box>

              {/* Ism - profil sahifasiga o'tish havolasi: login va parol shu yerda o'zgaradi. */}
              <Box
                component={NavLink}
                to="/admin/profile"
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1,
                  px: 1,
                  py: 0.5,
                  borderRadius: 2,
                  textDecoration: 'none',
                  color: 'text.secondary',
                  transition: 'background-color .18s ease, color .18s ease',
                  '&:hover': { bgcolor: 'rgba(37, 99, 235, 0.08)', color: 'primary.main' },
                }}
              >
                <Box
                  aria-hidden
                  sx={{
                    width: 30,
                    height: 30,
                    borderRadius: '50%',
                    display: 'grid',
                    placeItems: 'center',
                    fontSize: 13,
                    fontWeight: 700,
                    color: '#ffffff',
                    backgroundImage: `linear-gradient(135deg, ${brand[800]}, ${brand[500]})`,
                  }}
                >
                  {(user?.fullName ?? '?').trim().charAt(0).toUpperCase()}
                </Box>
                <Typography variant="body2" noWrap sx={{ fontWeight: 500 }}>
                  {user?.fullName}
                </Typography>
              </Box>

              <Typography
                component="button"
                onClick={logout}
                variant="body2"
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 0.75,
                  border: 0,
                  px: 1,
                  py: 0.75,
                  borderRadius: 2,
                  bgcolor: 'transparent',
                  cursor: 'pointer',
                  color: 'text.secondary',
                  '& svg': { fontSize: 18 },
                  '&:hover': { bgcolor: 'rgba(220, 38, 38, 0.08)', color: '#b91c1c' },
                }}
              >
                <LogoutRounded />
                {t('nav.logout')}
              </Typography>
            </Toolbar>
          </AppBar>

          <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
            <Drawer
              variant="temporary"
              open={mobileOpen}
              onClose={() => setMobileOpen(false)}
              ModalProps={{ keepMounted: true }}
              sx={{
                display: { xs: 'block', md: 'none' },
                '& .MuiDrawer-paper': drawerPaperStyles,
              }}
            >
              <SidebarContent onNavigate={() => setMobileOpen(false)} />
            </Drawer>

            <Drawer
              variant="permanent"
              open
              sx={{
                display: { xs: 'none', md: 'block' },
                '& .MuiDrawer-paper': drawerPaperStyles,
              }}
            >
              <SidebarContent />
            </Drawer>
          </Box>

          <Box
            component="main"
            sx={{ flexGrow: 1, p: { xs: 2, md: 3 }, width: { md: `calc(100% - ${DRAWER_WIDTH}px)` } }}
          >
            <Toolbar />
            <Routes key={location.pathname.split('/')[2] ?? ''}>
              <Route index element={<DashboardPage />} />
              <Route path="complaints" element={<AdminComplaintsPage />} />
              <Route path="complaints/:id" element={<AdminComplaintDetailPage />} />
              <Route path="register" element={<RegisterAdminPage />} />
              <Route path="news" element={<NewsAdminPage />} />
              <Route path="news/:id" element={<NewsDetailAdminPage />} />
              <Route path="staff" element={<StaffAdminPage />} />
              <Route path="about" element={<AboutAdminPage />} />
              <Route path="home" element={<HomeTextsAdminPage />} />
              <Route path="profile" element={<ProfilePage />} />
              {/* Guruhsiz yo'l - guruhlar ro'yxati, guruh bilan - uning ichi. */}
              <Route path="polls" element={<PollsAdminPage />} />
              <Route path="polls/:groupId" element={<PollsAdminPage />} />
              <Route path="tests" element={<QuizzesAdminPage />} />
              <Route path="tests/:groupId" element={<QuizzesAdminPage />} />

              {/* Faqat administrator */}
              <Route element={<ProtectedRoute require="admin" />}>
                <Route path="users" element={<UsersPage />} />
                <Route path="faculties" element={<FacultiesPage />} />
              </Route>
            </Routes>
          </Box>
        </Box>
      </ScopedCssBaseline>
    </ThemeProvider>
  );
}
