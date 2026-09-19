import { beforeEach, describe, expect, it } from 'vitest';
import i18next from '../i18n';
import { applySiteTexts, defaultSiteText, SITE_TEXT_KEYS } from './siteTexts';

describe('bosh sahifa matnlari', () => {
  beforeEach(async () => {
    localStorage.clear();
    applySiteTexts({});
    await i18next.changeLanguage('uz');
  });

  it('administrator yozgan matn tarjima ustidan chiqadi', () => {
    applySiteTexts({ uz: { 'home.heroTitle': 'Korrupsiyaga yo\'l yo\'q' } });

    expect(i18next.t('home.heroTitle')).toBe('Korrupsiyaga yo\'l yo\'q');
    // Boshqa tillarga tegmaydi
    expect(i18next.t('home.heroTitle', { lng: 'ru' })).toBe(defaultSiteText('ru', 'home.heroTitle'));
  });

  it('matn olib tashlansa asl holiga qaytadi', () => {
    applySiteTexts({ uz: { 'home.heroTitle': 'Vaqtinchalik sarlavha' } });
    applySiteTexts({});

    // Aniq matn bilan solishtiriladi: i18next tarjima faylining o'zini o'zgartirsa,
    // "asl matn" ham buzilib, ikkalasi bir-biriga teng chiqib qolardi.
    expect(i18next.t('home.heroTitle')).toBe('Korrupsiya haqida xabar bering');
    expect(defaultSiteText('uz', 'home.heroTitle')).toBe('Korrupsiya haqida xabar bering');
  });

  it('tahrirlanadigan har bir kalitning barcha tillarda asl matni bor', () => {
    for (const language of ['uz', 'uz-cyrl', 'ru', 'en']) {
      for (const key of SITE_TEXT_KEYS) {
        expect(defaultSiteText(language, key), `${language}: ${key}`).not.toBe('');
      }
    }
  });
});
