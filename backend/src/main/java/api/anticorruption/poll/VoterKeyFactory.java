package api.anticorruption.poll;

import api.anticorruption.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Ovoz beruvchi uchun takrorlanmas, lekin shaxsni oshkor qilmaydigan belgi yasaydi.
 *
 * <p>Tizimga kirgan foydalanuvchi uchun bu uning id si - eng ishonchli variant.
 * Anonim tashrifchi uchun IP manzil va brauzer satri maxfiy tuz bilan birga
 * SHA-256 orqali xeshlanadi. Natijada bazada IP saqlanmaydi, lekin bir odam
 * bir necha marta ovoz berishga uringani aniqlanadi.
 *
 * <p>Bu usul mutlaq emas: IP o'zgartirsa yoki brauzerni almashtirsa yana ovoz
 * bera oladi. Ommaviy so'rovnoma uchun bu maqbul muvozanat - qat'iyroq nazorat
 * ro'yxatdan o'tishni talab qilardi va ishtirokni keskin kamaytirardi.
 */
@Component
@RequiredArgsConstructor
public class VoterKeyFactory {

    private final PollProperties pollProperties;

    public String create(Long pollId, HttpServletRequest request, AppUserPrincipal principal) {
        if (principal != null) {
            return "u:" + principal.id();
        }
        String material = pollProperties.voteSalt()
                + "|" + pollId
                + "|" + clientIp(request)
                + "|" + safeHeader(request.getHeader("User-Agent"));

        return sha256Hex(material);
    }

    /** Reverse proxy ortida haqiqiy IP X-Forwarded-For sarlavhasida keladi. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    private String safeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 200 ? value.substring(0, 200) : value;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 har bir Java platformasida bor, bu yerga tushmaydi.
            throw new IllegalStateException("SHA-256 mavjud emas", ex);
        }
    }
}
