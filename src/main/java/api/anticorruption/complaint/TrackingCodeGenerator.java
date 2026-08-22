package api.anticorruption.complaint;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Year;

/**
 * Murojaat uchun noyob kuzatuv kodi yaratadi, masalan "AC-2026-K7M2Q4".
 *
 * <p>Alifboda O/0 va I/1 kabi bir-biriga o'xshash belgilar yo'q - kodni
 * telefonda aytib berish yoki qo'lda ko'chirishda xato bo'lmasligi uchun.
 */
@Component
@RequiredArgsConstructor
public class TrackingCodeGenerator {

    private static final String ALPHABET = "ACDEFGHJKLMNPQRTUVWXY34679";
    private static final int RANDOM_PART_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final ComplaintRepository complaintRepository;

    /**
     * Bazada mavjud bo'lmagan kod qaytaradi.
     *
     * @throws IllegalStateException bir necha urinishdan keyin ham bo'sh kod topilmasa
     */
    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = "AC-" + Year.now().getValue() + "-" + randomPart();
            if (!complaintRepository.existsByTrackingCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Noyob kuzatuv kodi yaratib bo'lmadi (" + MAX_ATTEMPTS + " urinishdan keyin)");
    }

    private String randomPart() {
        StringBuilder builder = new StringBuilder(RANDOM_PART_LENGTH);
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
