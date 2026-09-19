import type { ReactNode } from 'react';
import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material';
import { errorMessage, fieldErrors } from '../lib/errors';
import { brand, heroBackground, heroShadow, noMotion } from './theme';

/*
 * Sarlavha tasmasidagi yoylar: o'ngdan chapga uzluksiz suzadi.
 *
 * Chalkashlikning sababi shu edi: TO'LIQ doira tasmada IKKITA yoy beradi -
 * chap va o'ng cheti. Ular diametr masofada turadi, doiralar orasi esa
 * boshqa masofada, natijada yoylar goh zich, goh siyrak tushib, "yarmi
 * boshqa doiraniki" bo'lib ko'rinardi.
 *
 * Yechim: har bir element faqat BITTA yoy chizadi - doiraning chap cheti
 * (qolgan uchta chekka shaffof). Shunda ekrandagi har bir chiziq alohida
 * elementga tegishli va oraliq aynan `HERO_ARC_GAP` ga teng bo'ladi.
 *
 * Yoylar bitta o'ramda turadi va o'ram butunligicha siljiydi: siljish
 * aynan bitta oraliqqa teng, shuning uchun bir sikldan keyin har bir yoy
 * oldingisining o'rniga tushadi - halqa uzilmaydi.
 */
const HERO_ARC_SIZE = 340;
const HERO_ARC_GAP = 380;
const HERO_ARC_CYCLE = 11;

/**
 * Yoylar tasmaning O'NG YARMIDA turadi va markazga yetganda yo'qoladi -
 * chap uchigacha bormaydi.
 *
 * <p>Gradient o'ngdan chapga hisoblanadi: 0% - o'ng uchi, 100% - chap uchi.
 * Shunday qilib chiziqlar o'ng chekkada to'liq, 32% dan so'na boshlaydi va
 * 52% da, ya'ni tasma markazidan sal o'tib, butunlay tugaydi.
 *
 * <p>Foizlar tasma kengligiga nisbatan, shuning uchun ekran kengligi
 * qanday bo'lsa ham chiziqlar sarlavha turgan chap yarmga o'tmaydi.
 */
const HERO_ARC_FADE = 'linear-gradient(to left, #000 0%, #000 32%, transparent 52%)';

/** Eng keng ekranni ham qoplaydigan miqdor; ortiqchasi qirqib tashlanadi. */
const HERO_ARC_COUNT = 10;

/** Sarlavha va o'ng tomondagi asosiy amal tugmasi. */
export function AdminPage({
  title,
  description,
  action,
  children,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <Box
      sx={{
        /*
         * Sahifa "paydo bo'ladi", tayyor holda otilib chiqmaydi: bo'lim
         * almashganda ko'z yangi mazmunni topib olishga ulguradi.
         */
        '@keyframes adminPageEnter': {
          from: { opacity: 0, transform: 'translateY(10px)' },
          to: { opacity: 1, transform: 'none' },
        },
        animation: 'adminPageEnter .3s cubic-bezier(.2, .8, .3, 1) both',
        ...noMotion,
      }}
    >
      <Box
        sx={{
          position: 'relative',
          overflow: 'hidden',
          borderRadius: 4,
          mb: 3,
          px: { xs: 2.5, md: 3.5 },
          py: { xs: 2.25, md: 2.5 },
          color: '#ffffff',
          backgroundImage: heroBackground,
          boxShadow: heroShadow,
        }}
      >
        {/*
          Bezak qatlami. Tasmaning o'ng yarmi ko'pincha bo'sh qoladi (sahifada
          tavsif yoki tugma bo'lmasligi mumkin), shuning uchun u yalang'och
          gradient bo'lib qolmasligi kerak.

          Yorug'lik ham, halqalar ham bitta nuqtadan - o'ng chekkaning
          o'rtasidan - tarqaladi. Aks holda bezak tasodifiy dog'ga o'xshaydi.
        */}
        <Box
          aria-hidden
          sx={{
            position: 'absolute',
            inset: 0,
            /*
             * Yorug'lik keng yoyilgan: tor gradientning aylana cheti
             * ko'rinib qolardi va harakatlanayotgan yoylar bilan kesishib,
             * ortiqcha shakl hosil qilardi.
             */
            backgroundImage:
              'radial-gradient(620px 460px at 100% 50%, rgba(255,255,255,0.13), transparent 80%), ' +
              'radial-gradient(420px 300px at 2% 140%, rgba(147,197,253,0.13), transparent 78%)',
          }}
        />
        {/* Niqob qatlami: tasma balandligi bo'yicha yoylarni o'rtada so'ndiradi. */}
        <Box
          aria-hidden
          sx={{
            position: 'absolute',
            inset: 0,
            overflow: 'hidden',
            WebkitMaskImage: HERO_ARC_FADE,
            maskImage: HERO_ARC_FADE,
          }}
        >
          <Box
            sx={{
              position: 'absolute',
              top: '50%',
              left: 0,
              height: HERO_ARC_SIZE,
              // Markazlash chetga surish bilan - `transform` siljish uchun bo'sh qoladi.
              mt: `${-HERO_ARC_SIZE / 2}px`,
              width: `calc(100% + ${HERO_ARC_GAP}px)`,

              '@keyframes heroArcDrift': {
                from: { transform: 'translateX(0)' },
                to: { transform: `translateX(${-HERO_ARC_GAP}px)` },
              },
              animation: `heroArcDrift ${HERO_ARC_CYCLE}s linear infinite`,
              ...noMotion,
            }}
          >
            {Array.from({ length: HERO_ARC_COUNT }, (_, index) => (
              <Box
                key={index}
                sx={{
                  position: 'absolute',
                  top: 0,
                  left: index * HERO_ARC_GAP,
                  width: HERO_ARC_SIZE,
                  height: HERO_ARC_SIZE,
                  borderRadius: '50%',
                  /*
                   * Faqat chap chekka ko'rinadi. Tasma doiraning o'rta
                   * qismini kesib oladi, ya'ni yoyning eng yumshoq joyini -
                   * chekkalar bilan tutashgan burchaklar ko'rinmaydi.
                   */
                  border: '1px solid transparent',
                  // Chiziq kalta bo'lgani uchun quyuqroq: aks holda
                  // tasmaning yorug' o'ng yarmida ko'zga ilinmaydi.
                  borderLeftColor: 'rgba(255, 255, 255, 0.32)',
                }}
              />
            ))}
          </Box>
        </Box>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{
            position: 'relative',
            alignItems: { sm: 'center' },
            justifyContent: 'space-between',
          }}
        >
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              {title}
            </Typography>
            {/* Ommaviy saytdagi bo'lim sarlavhalari bilan bir xil urg'u chizig'i. */}
            <Box
              aria-hidden
              sx={{
                mt: 1,
                width: 44,
                height: 4,
                borderRadius: 2,
                backgroundImage: `linear-gradient(90deg, ${brand[300]}, rgba(255,255,255,0.75))`,
              }}
            />
            {description && (
              <Typography
                variant="body2"
                sx={{ mt: 1.5, maxWidth: 720, color: 'rgba(226, 232, 240, 0.86)' }}
              >
                {description}
              </Typography>
            )}
          </Box>
          {/*
            Amal tugmalari to'q fonda turadi, shuning uchun brend ranglari bu
            yerda ishlamaydi: to'ldirilgani oq bo'ladi, chizmalisi esa oq
            ramka bilan. Har bir sahifada tugma turini o'zgartirmaslik uchun
            uslub shu yerda, slot darajasida beriladi.
          */}
          {action && (
            <Box
              sx={{
                flexShrink: 0,
                '& .MuiButton-contained': {
                  backgroundImage: 'none',
                  backgroundColor: '#ffffff',
                  color: brand[700],
                  boxShadow: '0 8px 18px -10px rgba(2, 6, 23, 0.8)',
                  '&:hover': {
                    backgroundColor: brand[50],
                    transform: 'translateY(-2px)',
                    boxShadow: '0 14px 26px -12px rgba(2, 6, 23, 0.9)',
                  },
                  '&.Mui-disabled': {
                    backgroundColor: 'rgba(255, 255, 255, 0.35)',
                    color: 'rgba(255, 255, 255, 0.7)',
                  },
                },
                '& .MuiButton-outlined': {
                  color: '#ffffff',
                  borderColor: 'rgba(255, 255, 255, 0.55)',
                  '&:hover': {
                    borderColor: '#ffffff',
                    backgroundColor: 'rgba(255, 255, 255, 0.12)',
                  },
                },
                '& .MuiButton-text': { color: '#ffffff' },
              }}
            >
              {action}
            </Box>
          )}
        </Stack>
      </Box>
      {children}
    </Box>
  );
}

/**
 * So'rovning yuklanish va xatolik holatlarini bir joyda ko'rsatadi.
 * Har bir sahifada bir xil uch shoxli shartni yozmaslik uchun.
 */
export function QueryState({
  isPending,
  error,
  children,
}: {
  isPending: boolean;
  error: unknown;
  children: ReactNode;
}) {
  const { t } = useTranslation();

  if (isPending) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={28} />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{errorMessage(error, t)}</Alert>;
  }

  return <>{children}</>;
}

/**
 * Xatolikni ko'rsatadi.
 *
 * <p>Validatsiya xatoligida umumiy xabar ("Kiritilgan ma'lumotlarda xatolik
 * bor") qaysi maydonda muammo borligini aytmaydi - shuning uchun backend
 * qaytargan maydonlar ro'yxati ham chiqariladi. Aks holda foydalanuvchi
 * uzun shaklni ko'zi bilan qidirib chiqishga majbur bo'lardi.
 */
export function MutationError({ error }: { error: unknown }) {
  const { t } = useTranslation();
  if (!error) return null;

  const fields = Object.entries(fieldErrors(error));

  return (
    <Alert severity="error" sx={{ mt: 2 }}>
      {errorMessage(error, t)}

      {fields.length > 0 && (
        <Box component="ul" sx={{ m: 0, mt: 1, pl: 2.5 }}>
          {fields.map(([field, message]) => (
            <Box component="li" key={field}>
              <Typography variant="body2" component="span">
                <b>{fieldLabel(field, t)}</b> — {message}
              </Typography>
            </Box>
          ))}
        </Box>
      )}
    </Alert>
  );
}

/**
 * Maydon yo'lini o'qiladigan nomga aylantiradi.
 *
 * <p>Backend `questions[0].options` kabi yo'l yuboradi. Har bir bo'lak
 * alohida tarjima qilinadi, raqam esa odam sanaydigan ko'rinishga
 * o'tkaziladi (0 -> 1). Tarjimasi yo'q bo'lak o'z nomi bilan qoladi:
 * noma'lum maydon ham hech narsadan ko'ra foydaliroq.
 */
function fieldLabel(path: string, t: TFunction): string {
  return path
    .split('.')
    .map((segment) => {
      const match = /^(\w+)\[(\d+)\]$/.exec(segment);
      const name = match ? match[1] : segment;
      const label = t(`admin.fieldNames.${name}`, name);

      return match ? `${label} ${Number(match[2]) + 1}` : label;
    })
    .join(' · ');
}

const STATUS_COLORS: Record<
  string,
  'default' | 'primary' | 'warning' | 'secondary' | 'success' | 'error'
> = {
  NEW: 'primary',
  IN_REVIEW: 'warning',
  NEED_INFO: 'secondary',
  RESOLVED: 'success',
  REJECTED: 'default',
};

export function StatusChip({ status, label }: { status: string; label: string }) {
  return <Chip size="small" label={label} color={STATUS_COLORS[status] ?? 'default'} />;
}

/** O'chirishdan oldin tasdiqlash. */
export function ConfirmDialog({
  open,
  title,
  message,
  busy,
  onCancel,
  onConfirm,
}: {
  open: boolean;
  title: string;
  message?: string;
  busy?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onClose={onCancel} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary">
          {message ?? t('common.deleteConfirm')}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={busy}>
          {t('common.cancel')}
        </Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={busy}>
          {t('common.delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/** Yaratish/tahrirlash oynasi uchun umumiy chig'anoq. */
export function FormDialog({
  open,
  title,
  busy,
  error,
  onClose,
  onSubmit,
  children,
  maxWidth = 'sm',
  submitLabel,
}: {
  open: boolean;
  title: string;
  busy?: boolean;
  error?: unknown;
  onClose: () => void;
  onSubmit: () => void;
  children: ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md';
  /** Tasdiqlash tugmasi matni; berilmasa "Saqlash". */
  submitLabel?: string;
}) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onClose={onClose} maxWidth={maxWidth} fullWidth>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit();
        }}
      >
        <DialogTitle>{title}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {children}
          </Stack>
          <MutationError error={error} />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={busy}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" variant="contained" disabled={busy}>
            {submitLabel ?? t('common.save')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
