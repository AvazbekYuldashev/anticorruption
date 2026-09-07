import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Button, Stack, TextField, Typography } from '@mui/material';
import {
  applyLink,
  applyMark,
  renderRichText,
  RICH_TEXT_MARKS,
  type MarkupEdit,
} from '../lib/richText';

interface Props {
  label?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  helperText?: string;
  minRows?: number;
  /** Sarlavha uchun: panel ko'rsatiladi, lekin namuna bitta qatorda. */
  dense?: boolean;
}

/**
 * Formatlash paneli bo'lgan matn maydoni.
 *
 * <p>Matn HTML sifatida saqlanmaydi - faqat `**qalin**` kabi belgilar.
 * Shu tufayli saqlangan matn hech qachon sahifaga HTML bo'lib
 * qo'yilmaydi va tozalash (sanitizatsiya) umuman kerak emas:
 * `renderRichText` faqat o'zi biladigan elementlarni yasaydi, qolgan
 * hamma narsa oddiy matn bo'lib qoladi.
 *
 * <p>Panel tugmalari matn maydonining o'zini tahrirlaydi, shuning uchun
 * muharrir nima saqlanayotganini har doim ko'rib turadi - "yashirin HTML"
 * degan holat yo'q.
 */
export function RichTextField({
  label,
  value,
  onChange,
  placeholder,
  helperText,
  minRows = 4,
  dense = false,
}: Props) {
  const { t } = useTranslation();
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const pendingSelection = useRef<[number, number] | null>(null);

  // Belgilar qo'yilgandan keyin kursorni joyiga qaytaramiz: aks holda u
  // matn oxiriga sakrab ketardi va yozishni davom ettirib bo'lmasdi.
  useEffect(() => {
    const selection = pendingSelection.current;
    const element = inputRef.current;
    if (!selection || !element) return;

    pendingSelection.current = null;
    element.focus();
    element.setSelectionRange(selection[0], selection[1]);
  }, [value]);

  function edit(apply: (text: string, start: number, end: number) => MarkupEdit) {
    const element = inputRef.current;
    if (!element) return;

    const result = apply(value, element.selectionStart, element.selectionEnd);
    onChange(result.text);
    pendingSelection.current = result.selection;
  }

  const mark = (symbol: string) => () =>
    edit((text, start, end) => applyMark(text, start, end, symbol, t('admin.sampleText')));

  return (
    <Stack spacing={1}>
      <Stack direction="row" spacing={0.5} sx={{ flexWrap: 'wrap', gap: 0.5 }}>
        <Button size="small" onClick={mark(RICH_TEXT_MARKS.bold)} title={t('admin.bold')}>
          <b>B</b>
        </Button>
        <Button size="small" onClick={mark(RICH_TEXT_MARKS.italic)} title={t('admin.italic')}>
          <i>I</i>
        </Button>
        <Button size="small" onClick={mark(RICH_TEXT_MARKS.underline)} title={t('admin.underline')}>
          <u>U</u>
        </Button>
        <Button size="small" onClick={mark(RICH_TEXT_MARKS.strike)} title={t('admin.strike')}>
          <s>S</s>
        </Button>
        <Button
          size="small"
          title={t('admin.link')}
          onClick={() =>
            edit((text, start, end) =>
              applyLink(text, start, end, t('admin.sampleText'), 'https://'),
            )
          }
        >
          🔗
        </Button>
      </Stack>

      <TextField
        label={label}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        helperText={helperText ?? t('admin.formatHint')}
        multiline
        minRows={dense ? 1 : minRows}
        fullWidth
        inputRef={inputRef}
      />

      {value.trim() !== '' && (
        <Box sx={{ borderLeft: 3, borderColor: 'divider', pl: 1.5 }}>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
            {t('admin.preview')}
          </Typography>
          <Typography component="div" variant="body2" sx={{ whiteSpace: 'pre-line' }}>
            {renderRichText(value)}
          </Typography>
        </Box>
      )}
    </Stack>
  );
}
