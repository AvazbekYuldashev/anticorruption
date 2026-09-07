package api.anticorruption.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Fayl mazmuni e'lon qilingan turga mos kelishini tekshiradi.
 *
 * <p>Brauzer yuboradigan {@code Content-Type} sarlavhasi mijoz qo'lida:
 * uni istalgan qiymatga qo'yish mumkin. Faqat sarlavhaga ishonilsa,
 * bajariladigan faylni {@code image/png} deb yuklab, keyin uni ochiq
 * {@code /api/v1/media/} manzili orqali tarqatish mumkin bo'lardi.
 *
 * <p>Shuning uchun faylning birinchi baytlari ("magic bytes") ham
 * tekshiriladi. Bu mutlaq himoya emas - masalan haqiqiy PNG ichiga
 * begona ma'lumot yashirish mumkin - lekin fayl turini yolg'on ko'rsatish
 * yo'lini yopadi. Qolgan himoyani {@code nosniff} sarlavhasi va ochiq
 * zonaga faqat rasm qo'yish qoidasi beradi.
 */
final class FileSignatures {

    /** Tekshiruv uchun o'qiladigan bosh qism. WebP uchun 12 bayt yetadi. */
    static final int HEADER_SIZE = 512;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    private FileSignatures() {
    }

    /**
     * Faylning bosh qismini o'qiydi.
     *
     * <p>Oqim qaytadan o'qilishi kerak, shuning uchun chaqiruvchi uni
     * {@code mark}/{@code reset} qo'llab-quvvatlaydigan oqimga o'rashi shart.
     */
    static byte[] readHeader(InputStream in) throws IOException {
        byte[] header = new byte[HEADER_SIZE];
        int read = in.readNBytes(header, 0, HEADER_SIZE);
        return read == HEADER_SIZE ? header : Arrays.copyOf(header, read);
    }

    /**
     * Bosh qism e'lon qilingan turga mos keladimi.
     *
     * <p>Noma'lum tur uchun {@code true} qaytaradi: turlar ro'yxati oq
     * ro'yxat bilan allaqachon cheklangan, bu yerda esa faqat imzosi
     * ma'lum turlar tekshiriladi.
     */
    static boolean matches(String contentType, byte[] header) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(header, JPEG);
            case "image/png" -> startsWith(header, PNG);
            // WebP: "RIFF" + 4 bayt uzunlik + "WEBP"
            case "image/webp" -> startsWith(header, RIFF) && regionMatches(header, 8, WEBP);
            case "application/pdf" -> startsWith(header, PDF);
            // Matnli faylning imzosi yo'q, lekin ichida nol bayt bo'lmaydi -
            // nol bayt deyarli har doim ikkilik fayl belgisi.
            case "text/plain" -> !containsNul(header);
            default -> true;
        };
    }

    private static boolean startsWith(byte[] header, byte[] signature) {
        return regionMatches(header, 0, signature);
    }

    private static boolean regionMatches(byte[] header, int offset, byte[] signature) {
        if (header.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsNul(byte[] header) {
        for (byte value : header) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }
}
