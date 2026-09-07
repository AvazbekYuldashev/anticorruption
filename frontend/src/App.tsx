import { Suspense, lazy } from 'react';
import { Route, Routes } from 'react-router-dom';

import { Layout } from './components/Layout';
import { Spinner } from './components/ui';
import { ProtectedRoute } from './auth/ProtectedRoute';

/*
 * Bosh sahifa va 404 darrov kerak - ular asosiy bo'lakda qoladi.
 * Qolgan sahifalar alohida bo'laklarga ajratiladi va faqat ochilganda
 * yuklanadi: birinchi ochilishda yuklanadigan kod shuncha kichik bo'ladi.
 */
import { HomePage } from './pages/HomePage';
import { NotFoundPage } from './pages/SimplePages';

const SubmitComplaintPage = lazy(() =>
  import('./pages/SubmitComplaintPage').then((m) => ({ default: m.SubmitComplaintPage })),
);
const TrackComplaintPage = lazy(() =>
  import('./pages/TrackComplaintPage').then((m) => ({ default: m.TrackComplaintPage })),
);
const PublicRegisterPage = lazy(() =>
  import('./pages/PublicRegisterPage').then((m) => ({ default: m.PublicRegisterPage })),
);
const NewsListPage = lazy(() =>
  import('./pages/NewsPages').then((m) => ({ default: m.NewsListPage })),
);
const NewsDetailPage = lazy(() =>
  import('./pages/NewsPages').then((m) => ({ default: m.NewsDetailPage })),
);
const StaffPage = lazy(() => import('./pages/StaffPage').then((m) => ({ default: m.StaffPage })));
const AboutPage = lazy(() => import('./pages/AboutPage').then((m) => ({ default: m.AboutPage })));
const PollsPage = lazy(() => import('./pages/PollsPage').then((m) => ({ default: m.PollsPage })));
const PollDetailsPage = lazy(() =>
  import('./pages/PollsPage').then((m) => ({ default: m.PollDetailsPage })),
);
const QuizzesPage = lazy(() =>
  import('./pages/PollsPage').then((m) => ({ default: m.QuizzesPage })),
);
const QuizDetailsPage = lazy(() =>
  import('./pages/PollsPage').then((m) => ({ default: m.QuizDetailsPage })),
);
const StatsPage = lazy(() => import('./pages/StatsPage').then((m) => ({ default: m.StatsPage })));
const LoginPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.LoginPage })));
const MyComplaintsPage = lazy(() =>
  import('./pages/MyComplaintsPage').then((m) => ({ default: m.MyComplaintsPage })),
);
const MyComplaintDetailPage = lazy(() =>
  import('./pages/MyComplaintsPage').then((m) => ({ default: m.MyComplaintDetailPage })),
);

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
      <Route
        element={
          <Suspense fallback={<Spinner />}>
            <Layout />
          </Suspense>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="submit" element={<SubmitComplaintPage />} />
        <Route path="track" element={<TrackComplaintPage />} />
        <Route path="news" element={<NewsListPage />} />
        <Route path="news/:slug" element={<NewsDetailPage />} />
        <Route path="staff" element={<StaffPage />} />
        <Route path="about" element={<AboutPage />} />
        <Route path="polls" element={<PollsPage />} />
        <Route path="polls/:id" element={<PollDetailsPage />} />
        <Route path="tests" element={<QuizzesPage />} />
        <Route path="tests/:id" element={<QuizDetailsPage />} />
        <Route path="stats" element={<StatsPage />} />
        <Route path="login" element={<LoginPage />} />

        {/* Murojaatlar reyestri - faqat xodimlar uchun */}
        <Route element={<ProtectedRoute require="staff" />}>
          <Route path="register" element={<PublicRegisterPage />} />
        </Route>

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
