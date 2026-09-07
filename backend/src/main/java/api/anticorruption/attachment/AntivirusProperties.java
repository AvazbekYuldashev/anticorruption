package api.anticorruption.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Yuklangan fayllarni antivirus bilan tekshirish ("app.antivirus.*").
 *
 * <p>Standart holatda o'chiq: ishlab chiqish uchun ClamAV o'rnatish shart
 * emas. Ishlab chiqarishda yoqiladi va o'sha serverda {@code clamd}
 * ishlab turishi kerak.
 *
 * @param enabled    tekshiruv yoqilganmi
 * @param host       clamd manzili
 * @param port       clamd porti (odatda 3310)
 * @param timeout    ulanish va javob kutish muddati
 * @param failClosed clamd javob bermasa fayl rad etilsinmi. {@code true} -
 *                   tekshirilmagan fayl qabul qilinmaydi; {@code false} -
 *                   fayl o'tadi va logga ogohlantirish yoziladi
 */
@ConfigurationProperties(prefix = "app.antivirus")
public record AntivirusProperties(

        @DefaultValue("false") boolean enabled,

        @DefaultValue("127.0.0.1") String host,

        @DefaultValue("3310") int port,

        @DefaultValue("30s") Duration timeout,

        @DefaultValue("true") boolean failClosed
) {
}
