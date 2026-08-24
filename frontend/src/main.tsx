import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import './i18n';
import './index.css';
import { App } from './App';
import { AuthProvider } from './auth/AuthContext';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Ma'lumotlar sekin o'zgaradi: har bir fokusda qayta so'ramaymiz.
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      // 404 va 403 da qayta urinish ma'nosiz - faqat bir marta qayta urinamiz.
      retry: 1,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
