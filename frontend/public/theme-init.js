/*
 * Rejimni sahifa chizilishidan oldin qo'yadi.
 *
 * Bu skript <head> da, React yuklanishidan oldin ishlaydi: aks holda qorong'i
 * rejimni tanlagan tashrifchi har safar bir lahza oq sahifani ko'rib qolardi.
 * Qoida src/lib/theme.ts dagi bilan bir xil. Admin panel faqat yorug'.
 */
(function () {
  try {
    if (location.pathname.indexOf('/admin') === 0) return;
    var stored = localStorage.getItem('anticorruption.theme');
    var dark =
      stored === 'dark' ||
      (stored !== 'light' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
    if (dark) {
      document.documentElement.classList.add('dark');
      document.documentElement.style.colorScheme = 'dark';
    }
  } catch {
    /* localStorage taqiqlangan bo'lsa yorug' rejim qoladi */
  }
})();
