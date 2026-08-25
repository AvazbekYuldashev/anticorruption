import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  Divider,
  IconButton,
  Menu,
  MenuItem,
  Paper,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { contentApi, type SaveNewsBlock } from '../api/content';
import type { NewsBlockResponse, NewsBlockType } from '../api/types';
import { errorMessage } from '../lib/errors';
import { RICH_TEXT_MARKS, renderRichText } from '../lib/richText';

/** Albomdagi bitta rasm. */
export interface EditorImage {
  key: string;
  storedName: string;
  url: string;
  originalName: string;
  caption: string;
}

/**
 * Muharrirdagi blok.
 *
 * <p>`key` - faqat React ro'yxati uchun mahalliy belgi. Server id si
 * saqlanmaydi: saqlashda blok ro'yxati to'liq almashtiriladi, shuning
 * uchun eski id lar keraksiz.
 */
export interface EditorBlock {
  key: string;
  type: NewsBlockType;
  text: string;
  storedName: string;
  url: string;
  originalName: string;
  caption: string;
  images: EditorImage[];
}

let keyCounter = 0;

function newKey(): string {
  keyCounter += 1;
  return `block-${keyCounter}`;
}

/** `/api/v1/media/abc.png` -> `abc.png` */
function storedNameFromUrl(url: string | null): string {
  return url ? url.slice(url.lastIndexOf('/') + 1) : '';
}

/** Serverdan kelgan bloklarni muharrir ko'rinishiga o'giradi. */
export function toEditorBlocks(blocks: NewsBlockResponse[]): EditorBlock[] {
  return blocks.map((block) => ({
    key: newKey(),
    type: block.type,
    text: block.text ?? '',
    // Rasm bloki server nomini saqlashi kerak - u url dan ajratib olinadi.
    storedName: block.type === 'IMAGE' ? storedNameFromUrl(block.url) : '',
    url: block.url ?? '',
    originalName: block.originalName ?? '',
    caption: block.caption ?? '',
    images: block.images.map((image) => ({
      key: newKey(),
      storedName: storedNameFromUrl(image.url),
      url: image.url,
      originalName: image.originalName ?? '',
      caption: image.caption ?? '',
    })),
  }));
}

/** Saqlash uchun serverga yuboriladigan ko'rinish. */
export function toSavePayload(blocks: EditorBlock[]): SaveNewsBlock[] {
  return blocks.map((block) => {
    if (block.type === 'HEADING' || block.type === 'TEXT') {
      return { type: block.type, text: block.text };
    }

    if (block.type === 'GALLERY') {
      return {
        type: 'GALLERY',
        caption: block.caption || undefined,
        images: block.images.map((image) => ({
          storedName: image.storedName,
          originalName: image.originalName || undefined,
          caption: image.caption || undefined,
        })),
      };
    }

    return {
      type: 'IMAGE',
      storedName: block.storedName,
      originalName: block.originalName,
      caption: block.caption || undefined,
    };
  });
}

function emptyBlock(type: NewsBlockType): EditorBlock {
  return {
    key: newKey(),
    type,
    text: '',
    storedName: '',
    url: '',
    originalName: '',
    caption: '',
    images: [],
  };
}

interface Props {
  value: EditorBlock[];
  onChange: (blocks: EditorBlock[]) => void;
}

/**
 * Yangilik mazmunini bloklardan yig'adigan muharrir.
 *
 * <p>Har bir blokdan keyin "+" turadi: bosilganda sarlavha, matn, yakka
 * rasm yoki albom tanlanadi va yangi blok aynan o'sha joyga qo'shiladi.
 * Rasm tanlangan zahoti serverga yuklanadi - shunda foydalanuvchi uni
 * darhol ko'radi; yangilikning o'zi esa faqat "Saqlash" bosilganda yoziladi.
 */
export function NewsBlockEditor({ value, onChange }: Props) {
  const { t } = useTranslation();
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  function insertAt(index: number, block: EditorBlock) {
    const next = [...value];
    next.splice(index, 0, block);
    onChange(next);
  }

  function update(index: number, patch: Partial<EditorBlock>) {
    onChange(value.map((block, i) => (i === index ? { ...block, ...patch } : block)));
  }

  function remove(index: number) {
    onChange(value.filter((_, i) => i !== index));
  }

  function move(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= value.length) return;
    const next = [...value];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  function addTextAt(index: number, type: NewsBlockType) {
    insertAt(index, emptyBlock(type));
  }

  /** Tanlangan fayllarni yuklaydi. Bittasi ham yuklanmasa xato ko'rsatiladi. */
  async function upload(files: File[]): Promise<EditorImage[] | null> {
    setUploadError(null);
    setUploading(true);
    try {
      const uploaded = await Promise.all(files.map((file) => contentApi.uploadMedia(file)));
      return uploaded.map((media) => ({
        key: newKey(),
        storedName: media.storedName,
        url: media.url,
        originalName: media.originalName,
        caption: '',
      }));
    } catch (error) {
      setUploadError(errorMessage(error, t));
      return null;
    } finally {
      setUploading(false);
    }
  }

  async function addImageAt(index: number, file: File) {
    const images = await upload([file]);
    if (!images) return;

    insertAt(index, {
      ...emptyBlock('IMAGE'),
      storedName: images[0].storedName,
      url: images[0].url,
      originalName: images[0].originalName,
    });
  }

  async function addGalleryAt(index: number, files: File[]) {
    const images = await upload(files);
    if (!images) return;

    insertAt(index, { ...emptyBlock('GALLERY'), images });
  }

  /** Mavjud albomga yana rasm qo'shadi. */
  async function appendToGallery(index: number, files: File[]) {
    const images = await upload(files);
    if (!images) return;

    update(index, { images: [...value[index].images, ...images] });
  }

  function removeGalleryImage(index: number, imageKey: string) {
    update(index, { images: value[index].images.filter((image) => image.key !== imageKey) });
  }

  function adder(index: number) {
    return (
      <AddBlockButton
        uploading={uploading}
        onAddText={(type) => addTextAt(index, type)}
        onAddImage={(file) => void addImageAt(index, file)}
        onAddGallery={(files) => void addGalleryAt(index, files)}
      />
    );
  }

  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
        {t('admin.contentHint')}
      </Typography>

      {uploadError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setUploadError(null)}>
          {uploadError}
        </Alert>
      )}

      {value.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
          {t('admin.emptyContent')}
        </Typography>
      )}

      {/* Birinchi blokdan oldin ham qo'shish mumkin */}
      {adder(0)}

      {value.map((block, index) => (
        <Box key={block.key}>
          <Paper variant="outlined" sx={{ p: 2, mb: 1 }}>
            <Stack
              direction="row"
              spacing={1}
              sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}
            >
              <Typography variant="caption" color="text.secondary">
                {index + 1}. {t(BLOCK_LABELS[block.type])}
              </Typography>

              <Stack direction="row" spacing={0.5}>
                <IconButton
                  size="small"
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                  aria-label={t('admin.moveUp')}
                >
                  <Typography variant="caption">↑</Typography>
                </IconButton>
                <IconButton
                  size="small"
                  disabled={index === value.length - 1}
                  onClick={() => move(index, 1)}
                  aria-label={t('admin.moveDown')}
                >
                  <Typography variant="caption">↓</Typography>
                </IconButton>
                <IconButton
                  size="small"
                  color="error"
                  onClick={() => remove(index)}
                  aria-label={t('admin.removeBlock')}
                >
                  <Typography variant="caption">×</Typography>
                </IconButton>
              </Stack>
            </Stack>

            {block.type === 'HEADING' || block.type === 'TEXT' ? (
              <TextBlockFields
                block={block}
                onChangeText={(text) => update(index, { text })}
                onChangeType={(type) => update(index, { type })}
              />
            ) : block.type === 'GALLERY' ? (
              <GalleryBlockFields
                block={block}
                uploading={uploading}
                onAppend={(files) => void appendToGallery(index, files)}
                onRemoveImage={(imageKey) => removeGalleryImage(index, imageKey)}
                onChangeCaption={(caption) => update(index, { caption })}
              />
            ) : (
              <Stack spacing={1.5}>
                <Box
                  component="img"
                  src={block.url}
                  alt={block.originalName}
                  sx={{
                    width: '100%',
                    maxHeight: 260,
                    objectFit: 'contain',
                    borderRadius: 1,
                    bgcolor: 'grey.100',
                  }}
                />
                <TextField
                  size="small"
                  label={t('admin.blockCaption')}
                  value={block.caption}
                  onChange={(event) => update(index, { caption: event.target.value })}
                  fullWidth
                />
              </Stack>
            )}
          </Paper>

          {/* Har bir blokdan keyin yana "+" turadi */}
          {adder(index + 1)}
        </Box>
      ))}
    </Box>
  );
}

const BLOCK_LABELS: Record<NewsBlockType, string> = {
  HEADING: 'admin.blockHeading',
  TEXT: 'admin.blockText',
  IMAGE: 'admin.blockImage',
  GALLERY: 'admin.blockGallery',
};

/**
 * Sarlavha yoki matn bloki: tur almashtirgichi, formatlash tugmalari va
 * yozilayotgan matn qanday ko'rinishini ko'rsatuvchi namuna.
 */
function TextBlockFields({
  block,
  onChangeText,
  onChangeType,
}: {
  block: EditorBlock;
  onChangeText: (text: string) => void;
  onChangeType: (type: NewsBlockType) => void;
}) {
  const { t } = useTranslation();
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const pendingSelection = useRef<[number, number] | null>(null);

  /**
   * Formatlashdan keyin tanlovni tiklaydi.
   *
   * <p>Bog'liqliklar ro'yxatisiz - effekt har bir chizishdan keyin, ya'ni
   * yangi matn maydonga tushgandan keyin ishlaydi. Aks holda kursor matn
   * oxiriga sakrab ketardi va tugmalarni ketma-ket bosib bo'lmasdi.
   */
  useEffect(() => {
    const range = pendingSelection.current;
    const element = inputRef.current;
    if (!range || !element) return;

    pendingSelection.current = null;
    element.focus();
    element.setSelectionRange(range[0], range[1]);
  });

  /**
   * Tanlangan matnni belgilar bilan o'raydi. Hech narsa tanlanmagan bo'lsa
   * namuna so'z qo'yiladi va o'sha tanlangan holda qoladi - foydalanuvchi
   * darhol ustidan yozib ketaveradi.
   */
  function wrap(mark: string) {
    const element = inputRef.current;
    if (!element) return;

    const start = element.selectionStart;
    const end = element.selectionEnd;
    const selected = block.text.slice(start, end) || t('admin.sampleText');

    onChangeText(block.text.slice(0, start) + mark + selected + mark + block.text.slice(end));
    pendingSelection.current = [start + mark.length, start + mark.length + selected.length];
  }

  return (
    <Stack spacing={1.5}>
      <Stack
        direction="row"
        spacing={1}
        sx={{ flexWrap: 'wrap', gap: 1, alignItems: 'center' }}
      >
        <ToggleButtonGroup
          size="small"
          exclusive
          value={block.type}
          onChange={(_, next: NewsBlockType | null) => next && onChangeType(next)}
        >
          <ToggleButton value="HEADING">{t('admin.blockHeading')}</ToggleButton>
          <ToggleButton value="TEXT">{t('admin.blockText')}</ToggleButton>
        </ToggleButtonGroup>

        <Divider orientation="vertical" flexItem />

        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => wrap(RICH_TEXT_MARKS.bold)} title={t('admin.bold')}>
            <b>B</b>
          </Button>
          <Button size="small" onClick={() => wrap(RICH_TEXT_MARKS.italic)} title={t('admin.italic')}>
            <i>I</i>
          </Button>
          <Button
            size="small"
            onClick={() => wrap(RICH_TEXT_MARKS.underline)}
            title={t('admin.underline')}
          >
            <u>U</u>
          </Button>
          <Button
            size="small"
            onClick={() => wrap(RICH_TEXT_MARKS.strike)}
            title={t('admin.strike')}
          >
            <s>S</s>
          </Button>
        </Stack>
      </Stack>

      <TextField
        value={block.text}
        onChange={(event) => onChangeText(event.target.value)}
        placeholder={
          block.type === 'HEADING' ? t('admin.headingPlaceholder') : t('admin.textPlaceholder')
        }
        multiline
        minRows={block.type === 'HEADING' ? 1 : 3}
        fullWidth
        inputRef={inputRef}
        autoFocus={block.text === ''}
      />

      <Typography variant="caption" color="text.secondary">
        {t('admin.formatHint')}
      </Typography>

      {block.text.trim() !== '' && (
        <Box sx={{ borderLeft: 3, borderColor: 'divider', pl: 1.5 }}>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
            {t('admin.preview')}
          </Typography>
          <Typography
            component="div"
            variant={block.type === 'HEADING' ? 'h6' : 'body2'}
            sx={{ whiteSpace: 'pre-line' }}
          >
            {renderRichText(block.text)}
          </Typography>
        </Box>
      )}
    </Stack>
  );
}

/** Albom bloki: rasmlar to'ri, har biriga alohida izoh va butun albomga sarlavha. */
function GalleryBlockFields({
  block,
  uploading,
  onAppend,
  onRemoveImage,
  onChangeCaption,
}: {
  block: EditorBlock;
  uploading: boolean;
  onAppend: (files: File[]) => void;
  onRemoveImage: (imageKey: string) => void;
  onChangeCaption: (caption: string) => void;
}) {
  const { t } = useTranslation();
  const fileInput = useRef<HTMLInputElement>(null);

  return (
    <Stack spacing={1.5}>
      {block.images.length === 0 ? (
        <Alert severity="warning">{t('admin.galleryEmpty')}</Alert>
      ) : (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
            gap: 1.5,
          }}
        >
          {block.images.map((image) => (
            <Box key={image.key}>
              <Box sx={{ position: 'relative' }}>
                <Box
                  component="img"
                  src={image.url}
                  alt={image.originalName}
                  sx={{
                    width: '100%',
                    aspectRatio: '1 / 1',
                    objectFit: 'cover',
                    borderRadius: 1,
                    bgcolor: 'grey.100',
                  }}
                />
                <IconButton
                  size="small"
                  color="error"
                  aria-label={t('admin.removeImage')}
                  onClick={() => onRemoveImage(image.key)}
                  sx={{
                    position: 'absolute',
                    top: 4,
                    right: 4,
                    bgcolor: 'background.paper',
                    '&:hover': { bgcolor: 'background.paper' },
                  }}
                >
                  <Typography variant="caption">×</Typography>
                </IconButton>
              </Box>
            </Box>
          ))}
        </Box>
      )}

      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <Button size="small" disabled={uploading} onClick={() => fileInput.current?.click()}>
          {uploading ? t('admin.uploadingImage') : `+ ${t('admin.addImages')}`}
        </Button>
        <Typography variant="caption" color="text.secondary">
          {t('news.imageCount', { count: block.images.length })}
        </Typography>
      </Stack>

      <TextField
        size="small"
        label={t('admin.blockCaption')}
        value={block.caption}
        onChange={(event) => onChangeCaption(event.target.value)}
        fullWidth
      />

      <input
        ref={fileInput}
        type="file"
        hidden
        multiple
        accept="image/jpeg,image/png,image/webp"
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          if (files.length > 0) onAppend(files);
          event.target.value = '';
        }}
      />
    </Stack>
  );
}

/** "+" tugmasi: bosilganda sarlavha, matn, yakka rasm yoki albom tanlanadi. */
function AddBlockButton({
  uploading,
  onAddText,
  onAddImage,
  onAddGallery,
}: {
  uploading: boolean;
  onAddText: (type: NewsBlockType) => void;
  onAddImage: (file: File) => void;
  onAddGallery: (files: File[]) => void;
}) {
  const { t } = useTranslation();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const singleInput = useRef<HTMLInputElement>(null);
  const multiInput = useRef<HTMLInputElement>(null);

  /** Menyu yopilgandan keyin fayl tanlash oynasi ochiladi. */
  function pick(input: HTMLInputElement | null) {
    setAnchor(null);
    input?.click();
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', my: 1 }}>
      <Button
        size="small"
        variant="outlined"
        disabled={uploading}
        onClick={(event) => setAnchor(event.currentTarget)}
        sx={{ minWidth: 0, px: 2, borderStyle: 'dashed' }}
      >
        {uploading ? t('admin.uploadingImage') : `+ ${t('admin.addBlock')}`}
      </Button>

      <Menu anchorEl={anchor} open={anchor !== null} onClose={() => setAnchor(null)}>
        <MenuItem
          onClick={() => {
            setAnchor(null);
            onAddText('HEADING');
          }}
        >
          {t('admin.blockHeading')}
        </MenuItem>
        <MenuItem
          onClick={() => {
            setAnchor(null);
            onAddText('TEXT');
          }}
        >
          {t('admin.blockText')}
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => pick(singleInput.current)}>{t('admin.blockImage')}</MenuItem>
        <MenuItem onClick={() => pick(multiInput.current)}>{t('admin.blockGallery')}</MenuItem>
      </Menu>

      <input
        ref={singleInput}
        type="file"
        hidden
        accept="image/jpeg,image/png,image/webp"
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) onAddImage(file);
          // Bir xil faylni qayta tanlash mumkin bo'lishi uchun tozalaymiz.
          event.target.value = '';
        }}
      />
      <input
        ref={multiInput}
        type="file"
        hidden
        multiple
        accept="image/jpeg,image/png,image/webp"
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          if (files.length > 0) onAddGallery(files);
          event.target.value = '';
        }}
      />
    </Box>
  );
}
