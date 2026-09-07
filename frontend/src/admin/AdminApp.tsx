import { useState } from 'react';
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
  ListItemText,
  ScopedCssBaseline,
  ThemeProvider,
  Toolbar,
  Typography,
  createTheme,
} from '@mui/material';

import { useAuth } from '../auth/AuthContext';
import { LanguageSwitcher } from '../components/LanguageSwitcher';
import { ProtectedRoute } from '../auth/ProtectedRoute';

import { DashboardPage } from './DashboardPage';
import { AdminComplaintsPage } from './ComplaintsPage';
import { AdminComplaintDetailPage } from './ComplaintDetailPage';
import { UsersPage } from './UsersPage';
import { FacultiesPage } from './FacultiesPage';
import { NewsAdminPage } from './NewsAdminPage';
import { NewsDetailAdminPage } from './NewsDetailAdminPage';
import { StaffAdminPage } from './StaffAdminPage';
import { AboutAdminPage } from './AboutAdminPage';
import { ProfilePage } from './ProfilePage';
import { PollsAdminPage, QuizzesAdminPage } from './PollsAdminPage';

const DRAWER_WIDTH = 240;

/*
 * Admin uchun alohida MUI mavzusi. Ommaviy sayt bilan bir xil asosiy rang
 * ishlatiladi, shunda ikki qism bir tizimning bo'lagi ekani seziladi.
 */
const theme = createTheme({
  palette: {
    primary: { main: '#1d4ed8' },
    background: { default: '#f8fafc' },
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: "'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif",
    button: { textTransform: 'none', fontWeight: 500 },
  },
});

function useMenuItems() {
  const { t } = useTranslation();
  const { isAdmin } = useAuth();

  const items = [
    { to: '/admin', label: t('admin.navDashboard'), end: true },
    { to: '/admin/complaints', label: t('admin.navComplaints') },
    /*
      Murojaatlar reyestri bu yerda yo'q: u admin panelining sahifasi emas,
      saytning o'z sahifasi va xodimlarga asosiy menyudan ochiq. Ikkala joyda
      turgani ortiqcha takror edi.
    */
    { to: '/admin/news', label: t('admin.navNews') },
    { to: '/admin/staff', label: t('admin.navStaff') },
    { to: '/admin/about', label: t('admin.navAbout') },
    { to: '/admin/polls', label: t('admin.navPolls') },
    { to: '/admin/tests', label: t('admin.navTests') },
  ];

  // Foydalanuvchilar va tuzilma faqat administratorga ochiq -
  // backend ham aynan shu chegarani qo'yadi.
  if (isAdmin) {
    items.push(
      { to: '/admin/users', label: t('admin.navUsers') },
      { to: '/admin/faculties', label: t('admin.navFaculties') },
    );
  }

  return items;
}

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation();
  const items = useMenuItems();

  return (
    <Box>
      <Toolbar sx={{ gap: 1.5 }}>
        <Box
          component="img"
          src="/brand/asti-logo-64.png"
          alt=""
          sx={{ width: 32, height: 32, flexShrink: 0 }}
        />
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
            {t('site.shortName')}
          </Typography>
          <Typography variant="subtitle2" noWrap sx={{ fontWeight: 600 }}>
            {t('admin.title')}
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <List sx={{ py: 1 }}>
        {items.map((item) => (
          <ListItemButton
            key={item.to}
            component={NavLink}
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            sx={{
              mx: 1,
              borderRadius: 2,
              '&.active': { bgcolor: 'primary.main', color: 'common.white' },
            }}
          >
            <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontSize: 14 } } }} />
          </ListItemButton>
        ))}
      </List>
      <Divider />
      <List>
        <ListItemButton component={Link} to="/" onClick={onNavigate} sx={{ mx: 1, borderRadius: 2 }}>
          <ListItemText
            primary={t('admin.backToSite')}
            slotProps={{ primary: { color: 'text.secondary', sx: { fontSize: 14 } } }}
          />
        </ListItemButton>
      </List>
    </Box>
  );
}

export function AdminApp() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();

  return (
    <ThemeProvider theme={theme}>
      {/* Global emas, faqat shu bo'lim uchun: ommaviy saytdagi Tailwind
          uslublariga tegmasligi kerak. */}
      <ScopedCssBaseline sx={{ bgcolor: 'background.default', minHeight: '100vh' }}>
        <Box sx={{ display: 'flex' }}>
          <AppBar
            position="fixed"
            color="inherit"
            elevation={0}
            sx={{
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
                <Box component="span" sx={{ fontSize: 20, lineHeight: 1 }}>
                  ☰
                </Box>
              </IconButton>

              <Box sx={{ flexGrow: 1 }} />

              <Box sx={{ bgcolor: 'primary.main', borderRadius: 2, px: 0.5, py: 0.25 }}>
                <LanguageSwitcher compact />
              </Box>

              {/* Ism - profil sahifasiga o'tish havolasi: login va parol shu yerda o'zgaradi. */}
              <Typography
                component={NavLink}
                to="/admin/profile"
                variant="body2"
                noWrap
                sx={{
                  color: 'text.secondary',
                  textDecoration: 'none',
                  '&:hover': { color: 'primary.main' },
                }}
              >
                {user?.fullName}
              </Typography>
              <Typography
                component="button"
                onClick={logout}
                variant="body2"
                sx={{
                  border: 0,
                  bgcolor: 'transparent',
                  cursor: 'pointer',
                  color: 'text.secondary',
                  '&:hover': { color: 'text.primary' },
                }}
              >
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
                '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
              }}
            >
              <SidebarContent onNavigate={() => setMobileOpen(false)} />
            </Drawer>

            <Drawer
              variant="permanent"
              open
              sx={{
                display: { xs: 'none', md: 'block' },
                '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
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
              <Route path="news" element={<NewsAdminPage />} />
              <Route path="news/:id" element={<NewsDetailAdminPage />} />
              <Route path="staff" element={<StaffAdminPage />} />
              <Route path="about" element={<AboutAdminPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="polls" element={<PollsAdminPage />} />
              <Route path="tests" element={<QuizzesAdminPage />} />

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
