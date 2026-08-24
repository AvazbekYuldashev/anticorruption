import { Suspense, lazy } from 'react';
import { Route, Routes } from 'react-router-dom';

import { Layout } from './components/Layout';
import { Spinner } from './components/ui';
import { ProtectedRoute } from './auth/ProtectedRoute';

import { HomePage } from './pages/HomePage';
import { SubmitComplaintPage } from './pages/SubmitComplaintPage';
import { TrackComplaintPage } from './pages/TrackComplaintPage';
import { PublicRegisterPage } from './pages/PublicRegisterPage';
import { NewsDetailPage, NewsListPage } from './pages/NewsPages';
import { StaffPage } from './pages/StaffPage';
import { PollsPage } from './pages/PollsPage';
import { StatsPage } from './pages/StatsPage';
import { LinksPage, NotFoundPage, StaticPageView } from './pages/SimplePages';
import { LoginPage, SignUpPage } from './pages/AuthPages';
import { MyComplaintDetailPage, MyComplaintsPage } from './pages/MyComplaintsPage';

/*
 * Admin panel alohida bo'lakka ajratilgan: u MUI ga tayanadi va oddiy
 * tashrifchiga umuman kerak emas. Lazy yuklash tufayli ommaviy saytni
 * ochgan odam bu kodni yuklab olmaydi.
 */
const AdminApp = lazy(() =>
  import('./admin/AdminApp').then((module) => ({ default: module.AdminApp })),
);

export function App() {
  return (
    <Routes>
      {/* Ommaviy sayt */}
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="submit" element={<SubmitComplaintPage />} />
        <Route path="track" element={<TrackComplaintPage />} />
        <Route path="register" element={<PublicRegisterPage />} />
        <Route path="news" element={<NewsListPage />} />
        <Route path="news/:slug" element={<NewsDetailPage />} />
        <Route path="staff" element={<StaffPage />} />
        <Route path="polls" element={<PollsPage />} />
        <Route path="stats" element={<StatsPage />} />
        <Route path="links" element={<LinksPage />} />
        <Route path="pages/:slug" element={<StaticPageView />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignUpPage />} />

        {/* Tizimga kirgan foydalanuvchi uchun */}
        <Route element={<ProtectedRoute />}>
          <Route path="my" element={<MyComplaintsPage />} />
          <Route path="my/:id" element={<MyComplaintDetailPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* Admin panel - o'z tartibi (layout) bilan */}
      <Route element={<ProtectedRoute require="staff" />}>
        <Route
          path="/admin/*"
          element={
            <Suspense fallback={<Spinner />}>
              <AdminApp />
            </Suspense>
          }
        />
      </Route>
    </Routes>
  );
}
