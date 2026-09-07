package api.anticorruption.attachment;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.i18n.MessageKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Yuklangan faylni ClamAV ({@code clamd}) ga yuborib tekshiradi.
 *
 * <p>Kutubxona ishlatilmaydi - clamd protokoli ({@code INSTREAM}) juda
 * sodda: buyruq yuboriladi, keyin fayl bo'laklarga bo'lib uzatiladi va
 * bitta qatorli javob o'qiladi. Shu bir necha o'nlab qator uchun yangi
 * bog'liqlik qo'shish, uni yangilab yurish va uning xavfsizligini
 * kuzatish oqlanmaydi.
 *
 * <p>Fayl xotiraga yuklanmaydi: u diskdan oqim bo'lib o'tadi, shuning
 * uchun katta fayl ham xotirani band qilmaydi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntivirusScanner {

    /** clamd standart chegarasi 1 MB dan katta bo'lakni qabul qilmaydi. */
    private static final int CHUNK_SIZE = 64 * 1024;

    /** Bo'laklar tugaganini bildiradi. */
    private static final byte[] END_OF_STREAM = {0, 0, 0, 0};

    private final AntivirusProperties properties;

    /**
     * Faylni tekshiradi.
     *
     * <p>Zararli fayl topilsa xatolik tashlanadi. clamd javob bermasa
     * {@code failClosed} hal qiladi: tekshirilmagan faylni qabul qilish
     * antivirusni yoqishning ma'nosini yo'qotadi, lekin serverdagi
     * nosozlik tufayli butun murojaat yuborishni to'xtatib qo'yish ham
     * to'g'ri emas - shuning uchun bu sozlama bor.
     */
    public void verify(Path file, String label) {
        if (!properties.enabled()) {
            return;
        }

        String reply;
        try (InputStream in = Files.newInputStream(file)) {
            reply = scan(in);
        } catch (IOException ex) {
            unavailable(label, ex.getMessage());
            return;
        }

        if (reply.contains("FOUND")) {
            log.warn("Zararli fayl rad etildi: {} ({})", label, reply);
            throw new BadRequestException(MessageKeys.FILE_INFECTED, signatureOf(reply));
        }

        if (!reply.contains("OK")) {
            unavailable(label, reply);
            return;
        }

        log.debug("Fayl tekshiruvdan o'tdi: {}", label);
    }

    private String scan(InputStream data) throws IOException {
        int timeout = (int) properties.timeout().toMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(properties.host(), properties.port()), timeout);
            socket.setSoTimeout(timeout);

            try (OutputStream out = new BufferedOutputStream(socket.getOutputStream());
                 InputStream in = socket.getInputStream()) {

                out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));

                byte[] buffer = new byte[CHUNK_SIZE];
                int read;
                while ((read = data.read(buffer)) != -1) {
                    if (read == 0) {
                        continue;
                    }
                    // Har bir bo'lak oldidan uning uzunligi (4 bayt, big-endian).
                    out.write(ByteBuffer.allocate(Integer.BYTES).putInt(read).array());
                    out.write(buffer, 0, read);
                }

                out.write(END_OF_STREAM);
                out.flush();

                return new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim();
            }
        }
    }

    /** Javobdan imzo nomini ajratadi: "stream: Eicar-Test-Signature FOUND". */
    private String signatureOf(String reply) {
        int start = reply.indexOf(':');
        int end = reply.lastIndexOf("FOUND");
        if (start < 0 || end <= start) {
            return "unknown";
        }
        return reply.substring(start + 1, end).trim();
    }

    private void unavailable(String label, String detail) {
        log.warn("Antivirus tekshiruvi bajarilmadi: {} ({})", label, detail);

        if (properties.failClosed()) {
            throw new BadRequestException(MessageKeys.FILE_SCAN_UNAVAILABLE);
        }
    }
}
