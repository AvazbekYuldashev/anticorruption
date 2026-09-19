import { createContext, useContext, useRef, useState } from 'react';
import type { ChangeEvent, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
  IconButton,
  Stack,
  Tab,
  Tabs,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import type { SxProps, Theme } from '@mui/material';
import { homeBannerApi, type HomeBannerImage } from '../api/homeBanner';
import { siteTextsApi, type SiteTextItem } from '../api/siteTexts';
import { LANGUAGES } from '../i18n';
import { downscaleImage } from '../lib/images';
import {
  SITE_TEXT_FIELDS,
  SITE_TEXT_KEYS,
  applySiteTexts,
  defaultSiteText,
  type SiteTexts,
} from '../lib/siteTexts';
import { AdminPage, ConfirmDialog, MutationError, QueryState } from './common';
import { brand, heroShadow, softShadow } from './theme';

/*
 * Bosh sahifa matnlari va banner foni.
 *
 * Har bir bo'lim saytdagidek chizilgan ko'rinish bilan keladi, maydonlar esa
 * ko'rinish ostida saytdagi joyiga mos tartibda turadi: "Qanday ishlaydi"ning
 * uchta kartochkasi ostida uchta ustun. Maydon tanlanganda ko'rinishdagi joyi
 * yoritiladi, ko'rinishdagi matn bosilganda esa o'sha maydonga o'tiladi -
 * administrator qaysi matn saytning qayerida turishini taxmin qilib o'tirmaydi.
 */

/** Institut va markaz nomi sayt tepasida va pastida ham chiqadi - bu haqda eslatiladi. */
const SHARED_NAME_KEYS = new Set(['site.institute', 'site.name']);

/** Albomdagi rasmlar chegarasi - backenddagi HomeBannerService.MAX_IMAGES bilan bir xil. */
const MAX_BANNER_IMAGES = 10;

/** Tanlangan joyning rangi - oltin: ko'k ko'rinish ustida yaqqol ajralib turadi. */
const SPOT_COLOR = '#fbbf24';

interface Editor {
  /** Joriy tildagi matn: bo'sh bo'lsa asl matn. */
  shown: (key: string) => string;
  focused: string | null;
  /** Ko'rinishdagi matn bosilganda o'sha maydonga o'tadi. */
  pick: (key: string) => void;
}

const EditorContext = createContext<Editor | null>(null);

function useEditor(): Editor {
  const editor = useContext(EditorContext);
  if (!editor) throw new Error('Ko\'rinish tahrirlash oynasidan tashqarida ishlatildi');
  return editor;
}

export function HomeTextsAdminPage() {
  const { t } = useTranslation();
  const texts = useQuery({ queryKey: ['admin', 'siteTexts'], queryFn: siteTextsApi.admin });

  return (
    <AdminPage title={t('admin.homeTextsTitle')} description={t('admin.homeTextsHint')}>
      <QueryState isPending={texts.isPending} error={texts.error}>
        {texts.data && <HomeTextsForm saved={texts.data} />}
      </QueryState>
    </AdminPage>
  );
}

/**
 * Har bir til uchun barcha maydonlar to'liq matn bilan to'ldiriladi: o'zgartirilmagan
 * joyda asl matn turadi. Administrator bo'sh varaqdan emas, saytda ko'rib turgan
 * matnidan boshlab tahrirlaydi.
 */
function toForms(saved: SiteTexts): SiteTexts {
  const forms: SiteTexts = {};
  for (const { code } of LANGUAGES) {
    forms[code] = {};
    for (const key of SITE_TEXT_KEYS) {
      forms[code][key] = saved[code]?.[key] ?? defaultSiteText(code, key);
    }
  }
  return forms;
}

/** Faqat asl matndan farq qiladigan matnlar yuboriladi - qolgani asl holicha qoladi. */
function changedTexts(forms: SiteTexts): SiteTextItem[] {
  const items: SiteTextItem[] = [];
  for (const { code } of LANGUAGES) {
    for (const key of SITE_TEXT_KEYS) {
      const value = (forms[code]?.[key] ?? '').trim();
      if (value !== '' && value !== defaultSiteText(code, key).trim()) {
        items.push({ key, language: code, value });
      }
    }
  }
  return items;
}

function HomeTextsForm({ saved }: { saved: SiteTexts }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [forms, setForms] = useState<SiteTexts>(() => toForms(saved));
  const [active, setActive] = useState<string>(LANGUAGES[0].code);
  const [focused, setFocused] = useState<string | null>(null);
  const inputs = useRef<Record<string, HTMLInputElement | HTMLTextAreaElement | null>>({});

  const banner = useQuery({ queryKey: ['admin', 'homeBanner'], queryFn: homeBannerApi.admin });

  const save = useMutation({
    mutationFn: () => siteTextsApi.save(changedTexts(forms)),
    onSuccess: (data) => {
      // Sayt va admin panel bir zumda yangi matnni ko'rsin - qayta yuklamasdan.
      queryClient.setQueryData(['siteTexts'], data);
      queryClient.setQueryData(['admin', 'siteTexts'], data);
      applySiteTexts(data);
    },
  });

  const values = forms[active];
  // Saqlanmagan o'zgarish bormi - serverdagi holat bilan solishtiriladi.
  const dirty = JSON.stringify(changedTexts(forms)) !== JSON.stringify(changedTexts(toForms(saved)));

  function setValue(key: string, value: string) {
    save.reset();
    setForms((current) => ({ ...current, [active]: { ...current[active], [key]: value } }));
  }

  function resetLanguage() {
    save.reset();
    setForms((current) => ({
      ...current,
      [active]: Object.fromEntries(SITE_TEXT_KEYS.map((key) => [key, defaultSiteText(active, key)])),
    }));
  }

  const editor: Editor = {
    shown: (key) => values[key]?.trim() || defaultSiteText(active, key),
    focused,
    pick: (key) => {
      const input = inputs.current[key];
      input?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      input?.focus({ preventScroll: true });
    },
  };

  /** Bitta matn maydoni: o'zgargan bo'lsa asl matnga qaytarish tugmasi bilan. */
  const field = (key: string) => {
    const definition = SITE_TEXT_FIELDS[key];
    const value = values[key] ?? '';
    const original = defaultSiteText(active, key);
    const changed = value.trim() !== '' && value.trim() !== original.trim();

    return (
      <Box key={key} sx={{ minWidth: 0 }}>
        <TextField
          label={t(`admin.siteTextLabels.${definition.label}`)}
          value={value}
          onChange={(event) => setValue(key, event.target.value)}
          onFocus={() => setFocused(key)}
          onBlur={() => setFocused((current) => (current === key ? null : current))}
          inputRef={(element: HTMLInputElement | HTMLTextAreaElement | null) => {
            inputs.current[key] = element;
          }}
          multiline={definition.multiline}
          minRows={definition.multiline ? 3 : undefined}
          slotProps={{ htmlInput: { maxLength: 1000 } }}
          helperText={
            value.trim() === ''
              ? t('admin.homeTextsEmptyHint')
              : SHARED_NAME_KEYS.has(key)
                ? t('admin.homeTextsNameHint')
                : undefined
          }
          sx={focused === key ? { '& .MuiOutlinedInput-notchedOutline': { borderColor: SPOT_COLOR } } : undefined}
          fullWidth
        />
        {changed && (
          <Button size="small" sx={{ mt: 0.5 }} onClick={() => setValue(key, original)}>
            {t('admin.homeTextsReset')}
          </Button>
        )}
      </Box>
    );
  };

  return (
    <EditorContext.Provider value={editor}>
      <Stack spacing={3} sx={{ maxWidth: 1100 }}>
        {/*
          Til va saqlash doim ko'rinib tursin: sahifa uzun, administrator esa
          pastdagi bo'limni tahrirlab turib ham saqlay olishi kerak.
        */}
        <Box
          sx={{
            position: 'sticky',
            top: 72,
            zIndex: 5,
            px: { xs: 1.5, md: 2.5 },
            py: 1,
            borderRadius: 3,
            bgcolor: 'rgba(255, 255, 255, 0.92)',
            backdropFilter: 'blur(8px)',
            border: '1px solid rgba(148, 163, 184, 0.22)',
            boxShadow: softShadow,
          }}
        >
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={1.5}
            sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
          >
            <Tabs
              value={active}
              onChange={(_, value: string) => setActive(value)}
              variant="scrollable"
              scrollButtons="auto"
            >
              {LANGUAGES.map((language) => (
                <Tab key={language.code} value={language.code} label={language.name} />
              ))}
            </Tabs>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexShrink: 0 }}>
              {dirty && !save.isPending && (
                <Chip size="small" color="warning" variant="outlined" label={t('admin.homeTextsUnsaved')} />
              )}
              <Button variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>
                {save.isPending ? t('common.saving') : t('common.save')}
              </Button>
            </Stack>
          </Stack>
        </Box>

        {save.isSuccess && <Alert severity="success">{t('admin.homeTextsSaved')}</Alert>}
        <MutationError error={save.error} />

        <Alert severity="info">{t('admin.homeTextsHowToUse')}</Alert>

        {/* ------------------------------------------------ bosh banner */}
        <SectionCard title={t('admin.homeTextsGroupHero')}>
          <HeroPreview images={banner.data ?? []} />
          <FieldGrid columns={{ xs: 1, sm: 2 }}>
            {field('site.institute')}
            {field('site.name')}
          </FieldGrid>
          {field('home.heroTitle')}
          {field('home.heroText')}
          <FieldGrid columns={{ xs: 1, sm: 2 }}>
            {field('home.ctaSubmit')}
            {field('home.ctaTrack')}
          </FieldGrid>

          <BannerImagesEditor images={banner.data ?? []} loading={banner.isPending} />
        </SectionCard>

        {/* ------------------------------------------------ raqamlar */}
        <SectionCard title={t('admin.homeTextsGroupStats')}>
          <StatsPreview />
          {field('home.statsTitle')}
          <FieldGrid columns={{ xs: 1, sm: 2, lg: 4 }}>
            {field('home.statTotal')}
            {field('home.statResolved')}
            {field('home.statOpen')}
            {field('home.statLast30')}
          </FieldGrid>
        </SectionCard>

        {/* ------------------------------------------------ qanday ishlaydi */}
        <SectionCard title={t('admin.homeTextsGroupHow')}>
          <HowPreview />
          {field('home.howTitle')}
          <FieldGrid columns={{ xs: 1, md: 3 }}>
            {[1, 2, 3].map((step) => (
              <Stack key={step} spacing={2}>
                {field(`home.step${step}Title`)}
                {field(`home.step${step}Text`)}
              </Stack>
            ))}
          </FieldGrid>
        </SectionCard>

        {/* ------------------------------------------------ yangiliklar va so'rovnoma */}
        <SectionCard title={t('admin.homeTextsGroupSections')}>
          <NewsPollPreview />
          <FieldGrid columns={{ xs: 1, md: 3 }}>
            {field('home.newsTitle')}
            {field('home.newsMore')}
            {field('home.pollTitle')}
          </FieldGrid>
        </SectionCard>

        <Box>
          <Button disabled={save.isPending} onClick={resetLanguage}>
            {t('admin.homeTextsResetAll')}
          </Button>
        </Box>
      </Stack>
    </EditorContext.Provider>
  );
}

// ==================================================================== tartib

function SectionCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Stack
      spacing={2.5}
      sx={{
        p: { xs: 2, md: 3 },
        borderRadius: 4,
        bgcolor: 'background.paper',
        border: '1px solid rgba(148, 163, 184, 0.22)',
        boxShadow: softShadow,
      }}
    >
      <Typography variant="h6" sx={{ fontWeight: 700 }}>
        {title}
      </Typography>
      {children}
    </Stack>
  );
}

function FieldGrid({
  columns,
  children,
}: {
  columns: { xs: number; sm?: number; md?: number; lg?: number };
  children: ReactNode;
}) {
  const template = Object.fromEntries(
    Object.entries(columns).map(([breakpoint, count]) => [breakpoint, `repeat(${count}, minmax(0, 1fr))`]),
  );
  return <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: template }}>{children}</Box>;
}

/** Ko'rinish uchun ramka - "saytda shunday ko'rinadi" degan yozuv bilan. */
function PreviewFrame({ children, sx }: { children: ReactNode; sx?: SxProps<Theme> }) {
  const { t } = useTranslation();
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.75 }}>
        {t('admin.homeTextsPreview')}
      </Typography>
      <Box sx={[{ borderRadius: 3, overflow: 'hidden' }, ...(Array.isArray(sx) ? sx : [sx])]}>
        {children}
      </Box>
    </Box>
  );
}

/**
 * Ko'rinishdagi bitta matn.
 *
 * <p>Bosilsa o'sha maydonga o'tadi; maydon tanlanganda esa shu joy oltin
 * ramka bilan yoritiladi.
 */
function Spot({ textKey, sx }: { textKey: string; sx?: SxProps<Theme> }) {
  const { t } = useTranslation();
  const { shown, focused, pick } = useEditor();
  const active = focused === textKey;

  return (
    <Tooltip title={t('admin.homeTextsClickToEdit')} placement="top" disableInteractive>
      <Box
        component="span"
        role="button"
        tabIndex={-1}
        onClick={() => pick(textKey)}
        sx={[
          {
            display: 'inline-block',
            cursor: 'pointer',
            borderRadius: 1,
            outline: '2px dashed transparent',
            outlineOffset: 3,
            transition: 'outline-color .15s, box-shadow .15s, background-color .15s',
            '&:hover': { outlineColor: 'rgba(251, 191, 36, 0.7)' },
          },
          active && {
            outlineStyle: 'solid',
            outlineColor: SPOT_COLOR,
            boxShadow: '0 0 0 6px rgba(251, 191, 36, 0.28)',
          },
          ...(Array.isArray(sx) ? sx : [sx]),
        ]}
      >
        {shown(textKey)}
      </Box>
    </Tooltip>
  );
}

// ==================================================================== ko'rinishlar

/** Bosh banner - yuklangan rasm bilan, saytdagi qoraytiruvchi qatlam bilan. */
function HeroPreview({ images }: { images: HomeBannerImage[] }) {
  const [index, setIndex] = useState(0);
  const current = images.length === 0 ? null : images[index % images.length];

  return (
    <PreviewFrame sx={{ boxShadow: heroShadow }}>
      <Box
        sx={{
          position: 'relative',
          px: { xs: 2, sm: 4 },
          py: { xs: 4, sm: 6 },
          textAlign: 'center',
          color: '#ffffff',
          backgroundImage: `linear-gradient(135deg, ${brand[900]}, ${brand[800]} 50%, ${brand[700]})`,
        }}
      >
        {current && (
          <>
            <Box
              aria-hidden
              sx={{
                position: 'absolute',
                inset: 0,
                backgroundImage: `url(${current.url})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
              }}
            />
            <Box
              aria-hidden
              sx={{
                position: 'absolute',
                inset: 0,
                backgroundImage:
                  'linear-gradient(135deg, rgba(23, 37, 84, 0.9), rgba(23, 37, 84, 0.7) 50%, rgba(30, 64, 175, 0.55))',
              }}
            />
          </>
        )}

        <Stack spacing={0.75} sx={{ position: 'relative', alignItems: 'center' }}>
          <Spot
            textKey="site.institute"
            sx={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.18em', textTransform: 'uppercase', color: brand[100] }}
          />
          <Spot
            textKey="site.name"
            sx={{ fontSize: 11, letterSpacing: '0.14em', textTransform: 'uppercase', color: brand[200] }}
          />
          <Spot
            textKey="home.heroTitle"
            sx={{ pt: 2, fontSize: { xs: 20, sm: 28 }, fontWeight: 700, lineHeight: 1.25 }}
          />
          <Spot textKey="home.heroText" sx={{ maxWidth: 580, fontSize: 14, color: brand[100] }} />

          <Stack direction="row" sx={{ pt: 1.5, justifyContent: 'center', flexWrap: 'wrap', gap: 1.5 }}>
            <Spot
              textKey="home.ctaSubmit"
              sx={{
                px: 2.5,
                py: 1,
                borderRadius: 2.5,
                fontSize: 13,
                fontWeight: 600,
                color: brand[900],
                backgroundImage: 'linear-gradient(135deg, #fcd34d, #f59e0b)',
              }}
            />
            <Spot
              textKey="home.ctaTrack"
              sx={{
                px: 2.5,
                py: 1,
                borderRadius: 2.5,
                fontSize: 13,
                fontWeight: 600,
                border: '1px solid rgba(255, 255, 255, 0.4)',
                bgcolor: 'rgba(255, 255, 255, 0.1)',
              }}
            />
          </Stack>
        </Stack>

        {images.length > 1 && (
          <Stack direction="row" spacing={0.75} sx={{ position: 'relative', justifyContent: 'center', mt: 3 }}>
            {images.map((image, i) => (
              <Box
                key={image.id}
                component="button"
                type="button"
                onClick={() => setIndex(i)}
                sx={{
                  height: 8,
                  width: i === index % images.length ? 24 : 8,
                  p: 0,
                  border: 0,
                  borderRadius: 4,
                  cursor: 'pointer',
                  bgcolor: i === index % images.length ? '#ffffff' : 'rgba(255, 255, 255, 0.5)',
                  transition: 'all .2s',
                }}
              />
            ))}
          </Stack>
        )}
      </Box>
    </PreviewFrame>
  );
}

/** Markazdagi sarlavha ostidagi qisqa chiziq - saytdagi bo'lim sarlavhasi. */
function CenteredTitle({ textKey }: { textKey: string }) {
  return (
    <Box sx={{ textAlign: 'center', mb: 2.5 }}>
      <Spot textKey={textKey} sx={{ fontSize: { xs: 18, sm: 22 }, fontWeight: 600, color: '#0f172a' }} />
      <Box sx={{ mx: 'auto', mt: 1, height: 4, width: 48, borderRadius: 2, bgcolor: brand[500] }} />
    </Box>
  );
}

const pageBackground = { bgcolor: '#f5f8fc', border: '1px solid rgba(148, 163, 184, 0.25)', p: { xs: 2, sm: 3 } };

function StatsPreview() {
  const tiles = [
    { key: 'home.statTotal', value: 128, bg: brand[50], color: brand[700] },
    { key: 'home.statResolved', value: 96, bg: '#ecfdf5', color: '#047857' },
    { key: 'home.statOpen', value: 24, bg: '#fffbeb', color: '#b45309' },
    { key: 'home.statLast30', value: 17, bg: '#f1f5f9', color: '#334155' },
  ];

  return (
    <PreviewFrame sx={pageBackground}>
      <CenteredTitle textKey="home.statsTitle" />
      <Box sx={{ display: 'grid', gap: 1.5, gridTemplateColumns: { xs: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' } }}>
        {tiles.map((tile) => (
          <Box
            key={tile.key}
            sx={{ p: 2, textAlign: 'center', borderRadius: 3, bgcolor: '#ffffff', border: '1px solid #e2e8f0' }}
          >
            <Box
              sx={{
                mx: 'auto',
                mb: 1.25,
                width: 48,
                height: 48,
                borderRadius: '50%',
                display: 'grid',
                placeItems: 'center',
                fontWeight: 700,
                bgcolor: tile.bg,
                color: tile.color,
              }}
            >
              {tile.value}
            </Box>
            <Spot textKey={tile.key} sx={{ fontSize: 13, fontWeight: 500, color: '#334155' }} />
          </Box>
        ))}
      </Box>
    </PreviewFrame>
  );
}

function HowPreview() {
  return (
    <PreviewFrame sx={pageBackground}>
      <CenteredTitle textKey="home.howTitle" />
      <Box sx={{ display: 'grid', gap: 2.5, pt: 1.5, gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' } }}>
        {[1, 2, 3].map((step) => (
          <Box
            key={step}
            sx={{ position: 'relative', p: 2.5, pt: 3.5, borderRadius: 3, bgcolor: '#ffffff', border: '1px solid #e2e8f0' }}
          >
            <Box
              sx={{
                position: 'absolute',
                top: -16,
                left: 20,
                width: 34,
                height: 34,
                borderRadius: '50%',
                display: 'grid',
                placeItems: 'center',
                fontSize: 14,
                fontWeight: 700,
                color: '#ffffff',
                bgcolor: brand[600],
              }}
            >
              {step}
            </Box>
            <Spot textKey={`home.step${step}Title`} sx={{ fontWeight: 600, color: '#0f172a' }} />
            <Box sx={{ mt: 1 }}>
              <Spot textKey={`home.step${step}Text`} sx={{ fontSize: 14, lineHeight: 1.6, color: '#475569' }} />
            </Box>
          </Box>
        ))}
      </Box>
    </PreviewFrame>
  );
}

function NewsPollPreview() {
  const { t } = useTranslation();

  const header = (titleKey: string, link: ReactNode) => (
    <Stack
      direction="row"
      sx={{ alignItems: 'center', justifyContent: 'space-between', gap: 1, pb: 1.25, mb: 1.5, borderBottom: '1px solid #e2e8f0' }}
    >
      <Spot textKey={titleKey} sx={{ fontSize: 17, fontWeight: 600, color: '#0f172a' }} />
      <Box sx={{ fontSize: 13, fontWeight: 500, color: brand[600] }}>{link}</Box>
    </Stack>
  );

  const placeholder = (
    <Box sx={{ height: 56, borderRadius: 3, bgcolor: '#ffffff', border: '1px solid #e2e8f0' }} />
  );

  return (
    <PreviewFrame sx={pageBackground}>
      <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' } }}>
        <Box>
          {header(
            'home.newsTitle',
            <>
              <Spot textKey="home.newsMore" /> →
            </>,
          )}
          {placeholder}
        </Box>
        <Box>
          {header('home.pollTitle', <>{t('polls.title')} →</>)}
          {placeholder}
        </Box>
      </Box>
    </PreviewFrame>
  );
}

// ==================================================================== banner foni

/**
 * Banner fonining rasmlari: yuklash, tartib va o'chirish.
 *
 * <p>Matnlardan farqli, rasmlar yuklanishi bilan saqlanadi - "Saqlash" tugmasi
 * ularga tegishli emas. Katta surat yuklashdan oldin brauzerda kichraytiriladi.
 */
function BannerImagesEditor({ images, loading }: { images: HomeBannerImage[]; loading: boolean }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const fileInput = useRef<HTMLInputElement>(null);
  const [deleting, setDeleting] = useState<HomeBannerImage | null>(null);

  function refresh(data: HomeBannerImage[]) {
    queryClient.setQueryData(['admin', 'homeBanner'], data);
    // Saytdagi banner ham yangilansin.
    void queryClient.invalidateQueries({ queryKey: ['homeBanner'] });
  }

  const upload = useMutation({
    mutationFn: async (files: File[]) => homeBannerApi.upload(await Promise.all(files.map((file) => downscaleImage(file)))),
    onSuccess: refresh,
  });

  const reorder = useMutation({ mutationFn: homeBannerApi.reorder, onSuccess: refresh });

  const remove = useMutation({
    mutationFn: (id: number) => homeBannerApi.remove(id),
    onSuccess: (data) => {
      setDeleting(null);
      refresh(data);
    },
  });

  function onFiles(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = '';
    if (files.length > 0) upload.mutate(files);
  }

  function move(index: number, delta: number) {
    const ids = images.map((image) => image.id);
    const target = index + delta;
    if (target < 0 || target >= ids.length) return;
    [ids[index], ids[target]] = [ids[target], ids[index]];
    reorder.mutate(ids);
  }

  const busy = upload.isPending || reorder.isPending || remove.isPending;
  const full = images.length >= MAX_BANNER_IMAGES;

  return (
    <Box sx={{ pt: 1, borderTop: '1px solid rgba(148, 163, 184, 0.25)' }}>
      <Typography variant="subtitle1" sx={{ fontWeight: 700, mt: 1.5 }}>
        {t('admin.bannerImagesTitle')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
        {t('admin.bannerImagesHint', { max: MAX_BANNER_IMAGES })}
      </Typography>

      <MutationError error={upload.error ?? reorder.error ?? remove.error} />

      {!loading && images.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {t('admin.bannerImagesEmpty')}
        </Typography>
      )}

      <Box
        sx={{
          display: 'grid',
          gap: 1.5,
          gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', sm: 'repeat(3, minmax(0, 1fr))', md: 'repeat(5, minmax(0, 1fr))' },
          mb: 2,
        }}
      >
        {images.map((image, index) => (
          <Box
            key={image.id}
            sx={{ borderRadius: 2.5, overflow: 'hidden', border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}
          >
            <Box
              sx={{
                position: 'relative',
                aspectRatio: '16 / 9',
                backgroundImage: `url(${image.url})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
              }}
            >
              <Chip
                size="small"
                label={index + 1}
                sx={{ position: 'absolute', top: 6, left: 6, bgcolor: 'rgba(255,255,255,0.9)', fontWeight: 700 }}
              />
            </Box>
            <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', px: 0.5 }}>
              <Stack direction="row">
                <IconButton
                  size="small"
                  disabled={busy || index === 0}
                  onClick={() => move(index, -1)}
                  aria-label={t('admin.moveUp')}
                >
                  <Typography variant="caption">←</Typography>
                </IconButton>
                <IconButton
                  size="small"
                  disabled={busy || index === images.length - 1}
                  onClick={() => move(index, 1)}
                  aria-label={t('admin.moveDown')}
                >
                  <Typography variant="caption">→</Typography>
                </IconButton>
              </Stack>
              <IconButton
                size="small"
                color="error"
                disabled={busy}
                onClick={() => setDeleting(image)}
                aria-label={t('common.delete')}
              >
                <Typography variant="caption">×</Typography>
              </IconButton>
            </Stack>
          </Box>
        ))}
      </Box>

      <input
        ref={fileInput}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        multiple
        hidden
        onChange={onFiles}
      />
      <Button variant="outlined" disabled={busy || full} onClick={() => fileInput.current?.click()}>
        {upload.isPending ? t('admin.bannerImagesUploading') : t('admin.bannerImagesUpload')}
      </Button>
      {full && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          {t('admin.bannerImagesFull', { max: MAX_BANNER_IMAGES })}
        </Typography>
      )}

      <ConfirmDialog
        open={deleting !== null}
        title={deleting?.originalName ?? ''}
        message={t('admin.bannerImagesDeleteHint')}
        busy={remove.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => {
          if (deleting) remove.mutate(deleting.id);
        }}
      />
    </Box>
  );
}
