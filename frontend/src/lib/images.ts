/*
 * Rasmni yuklashdan oldin brauzerning o'zida kichraytirish.
 *
 * Telefon yoki fotoapparat surati 5-10 MB bo'ladi, bosh banner esa har bir
 * tashrifchiga yuklanadi. Ekran uchun 2400 piksel enli rasm yetarli - undan
 * kattasi sahifani sekinlashtiradi, lekin ko'zga farq qilmaydi.
 */

/** Rasmning eng katta tomoni, piksel. */
const MAX_SIDE = 2400;

const JPEG_QUALITY = 0.85;

/** Bundan yengil va o'lchami chegaradan oshmagan rasm o'zgartirilmaydi. */
const SMALL_ENOUGH_BYTES = 700 * 1024;

/**
 * Katta rasmni kichraytirib JPEG ga o'giradi.
 *
 * <p>Kichraytirib bo'lmasa (brauzer rasmni o'qiy olmasa) yoki natija
 * asl fayldan og'ir chiqsa, asl fayl qaytadi - yuklash baribir davom etadi.
 */
export async function downscaleImage(file: File, maxSide = MAX_SIDE): Promise<File> {
  if (!file.type.startsWith('image/') || typeof createImageBitmap !== 'function') {
    return file;
  }

  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file);
  } catch {
    return file;
  }

  const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height));
  if (scale === 1 && file.size <= SMALL_ENOUGH_BYTES) {
    bitmap.close();
    return file;
  }

  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);

  const context = canvas.getContext('2d');
  if (!context) {
    bitmap.close();
    return file;
  }

  // JPEG da shaffoflik yo'q: bo'sh joylar qora emas, banner rangida bo'lsin.
  context.fillStyle = '#172554';
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();

  const blob = await new Promise<Blob | null>((resolve) =>
    canvas.toBlob(resolve, 'image/jpeg', JPEG_QUALITY),
  );
  if (!blob || blob.size >= file.size) {
    return file;
  }

  const name = `${file.name.replace(/\.[^.]+$/, '') || 'rasm'}.jpg`;
  return new File([blob], name, { type: 'image/jpeg' });
}
