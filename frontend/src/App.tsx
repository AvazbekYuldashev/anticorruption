import { Suspense, lazy, useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';

import { Layout } from './components/Layout';
import { Spinner } from './components/ui';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { siteTextsApi } from './api/siteTexts';
import { applySiteTexts } from './lib/siteTexts';

/**
 * Administrator yozgan bosh sahifa matnlarini yuklab, tarjimalar ustidan qo'yadi.
 *
 * <p>Ilova boshida bir marta: sayt ham, admin panel ham bir xil nomni ko'rsin.
 */
function useSiteTexts() {
  const query = useQuery({ queryKey: ['siteTexts'], queryFn: siteTextsApi.all, staleTime: 5 * 60_000 });

  useEffect(() => {
    if (query.data) applySiteTexts(query.data);
  }, [query.data]);
}

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
/*
 * Shaxsiy kabinet - o'z tartibi (yon menyu) bilan. Faqat tizimga kirgan
 * odamga kerak, shuning uchun u ham alohida bo'lakka ajratilgan.
 */
const CabinetLayout = lazy(() =>
  import('./cabinet/CabinetLayout').then((m) => ({ default: m.CabinetLayout })),
);
const CabinetOverviewPage = lazy(() =>
  import('./cabinet/OverviewPage').then((m) => ({ default: m.CabinetOverviewPage })),
);
const CabinetComplaintsPage = lazy(() =>
  import('./cabinet/ComplaintsPage').then((m) => ({ default: m.CabinetComplaintsPage })),
);
const CabinetComplaintDetailPage = lazy(() =>
  import('./cabinet/ComplaintsPage').then((m) => ({ default: m.CabinetComplaintDetailPage })),
);
const CabinetProfilePage = lazy(() =>
  import('./cabinet/ProfilePage').then((m) => ({ default: m.CabinetProfilePage })),
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
  useSiteTexts();

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

        {/*
          Murojaatlar reyestri bu yerda yo'q: u faqat admin panelida
          (/admin/register). Ommaviy saytda "/register" oddiy 404 beradi.
        */}

        {/*
          Shaxsiy kabinet. Murojaatlar "/my/complaints" da, tafsilot esa
          uning ichida: shu tufayli "/my/profile" bilan to'qnashuv bo'lmaydi.
          Ilgari tafsilot "/my/:id" edi va profil sahifasi qo'shilishi bilan
          ikki yo'l bir-birining ustiga tushardi.
        */}
        <Route element={<ProtectedRoute />}>
          <Route path="my" element={<CabinetLayout />}>
            <Route index element={<CabinetOverviewPage />} />
            <Route path="complaints" element={<CabinetComplaintsPage />} />
            <Route path="complaints/:id" element={<CabinetComplaintDetailPage />} />
            <Route path="profile" element={<CabinetProfilePage />} />
          </Route>
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
