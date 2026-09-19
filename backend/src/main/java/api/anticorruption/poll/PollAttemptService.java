package api.anticorruption.poll;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.i18n.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Savollari tasodifiy tanlanadigan testda ishtirokchilarning to'plamlari.
 *
 * <p>Tranzaksiyani {@link PollService} ochadi - bu yerdagi amallar uning
 * ichida bajariladi.
 */
@Service
@RequiredArgsConstructor
public class PollAttemptService {

    private final PollAttemptRepository attemptRepository;

    /**
     * Tasodifiy sonlar manbai.
     *
     * <p>Oddiy {@link java.util.Random} ketma-ketligini bir nechta natijadan
     * keyin taxmin qilish mumkin - kimga qaysi savollar tushishi oldindan
     * bilinmasligi kerak.
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * Ishtirokchining to'plamini qaytaradi, hali bo'lmasa tanlaydi.
     *
     * <p>Qayta boshlaganda yangi to'plam berilmaydi - aks holda "osonroq"
     * savollar chiqquncha qayta-qayta boshlash mumkin bo'lardi.
     */
    PollAttempt startOrResume(Poll poll, String voterKey) {
        Optional<PollAttempt> existing = attemptRepository.findByPollIdAndVoterKey(poll.getId(), voterKey);

        if (existing.isPresent()) {
            PollAttempt attempt = existing.get();
            if (attempt.isSubmitted()) {
                throw new ConflictException(MessageKeys.POLL_ALREADY_VOTED);
            }
            // Tushgan savollarning hammasi keyin o'chirilgan bo'lsa ishtirokchi
            // bo'sh test bilan qolib ketmasin. To'plamni faqat administrator
            // o'zgartirgan savollar yangilaydi, ishtirokchining o'zi emas.
            if (questionsOf(poll, attempt).isEmpty()) {
                attempt.assignQuestions(draw(poll));
                attemptRepository.save(attempt);
            }
            return attempt;
        }

        PollAttempt attempt = PollAttempt.builder()
                .poll(poll)
                .voterKey(voterKey)
                .token(UUID.randomUUID().toString())
                .build();
        attempt.assignQuestions(draw(poll));
        return attemptRepository.save(attempt);
    }

    /**
     * Savollar bazasidan tasodifiy to'plam tanlaydi.
     *
     * <p>Ro'yxat aralashtiriladi (Fisher-Yates) va boshidan kerakli miqdori
     * olinadi. Har bir savol ro'yxatda bir marta turgani uchun to'plamda
     * takrorlanish bo'lmaydi, har qanday to'plam esa teng ehtimol bilan
     * tushadi. Savollar tartibi ham aralashganicha qoladi.
     */
    List<PollQuestion> draw(Poll poll) {
        List<PollQuestion> pool = new ArrayList<>(poll.getQuestions());
        Collections.shuffle(pool, random);

        Integer drawSize = poll.drawSize();
        int size = drawSize == null ? pool.size() : drawSize;
        return List.copyOf(pool.subList(0, size));
    }

    /**
     * Ishtirokchiga tushgan savollar - tushgan tartibida.
     *
     * <p>To'plam tanlangandan keyin administrator testni tahrirlab ba'zi
     * savollarni o'chirgan bo'lishi mumkin. Ular tashlab ketiladi: javob
     * berib bo'lmaydigan savol ishtirokchining natijasini nohaq tushirardi.
     */
    List<PollQuestion> questionsOf(Poll poll, PollAttempt attempt) {
        Map<Long, PollQuestion> byId = new HashMap<>();
        for (PollQuestion question : poll.getQuestions()) {
            byId.put(question.getId(), question);
        }

        List<PollQuestion> questions = new ArrayList<>(attempt.getQuestionCount());
        for (Long questionId : attempt.questionIdList()) {
            PollQuestion question = byId.get(questionId);
            if (question != null) {
                questions.add(question);
            }
        }
        return questions;
    }

    Optional<PollAttempt> findForVoter(Poll poll, String voterKey) {
        return attemptRepository.findByPollIdAndVoterKey(poll.getId(), voterKey);
    }

    /**
     * Yuborilgan javoblar qaysi to'plamga tegishli ekanini aniqlaydi.
     *
     * <p>Avvalo testni boshlaganda berilgan belgi bo'yicha qidiriladi: anonim
     * ishtirokchining belgisi IP manzildan hisoblanadi va u test davomida
     * o'zgarishi mumkin. Belgi kelmagan bo'lsa - masalan, sahifa yangilanishdan
     * oldingi versiyada ochiq qolgan - ishtirokchining o'z belgisi bo'yicha.
     *
     * <p>Belgi savollari tasodifiy tanlanmaydigan testda ham hisobga olinadi:
     * administrator ishtirokchi test ishlab turgan paytda tasodifiy tanlovni
     * o'chirib qo'ygan bo'lsa ham, u olgan savollariga javob bera oladi.
     *
     * @return to'plam; savollari tasodifiy tanlanmaydigan so'rovnomada null
     */
    PollAttempt resolveForVote(Poll poll, String token, String voterKey) {
        if (token != null && !token.isBlank()) {
            return attemptRepository.findByPollIdAndToken(poll.getId(), token.trim())
                    .orElseThrow(() -> new BadRequestException(MessageKeys.POLL_ATTEMPT_NOT_FOUND));
        }
        if (!poll.isRandomized()) {
            return null;
        }
        return attemptRepository.findByPollIdAndVoterKey(poll.getId(), voterKey)
                .orElseThrow(() -> new BadRequestException(MessageKeys.POLL_ATTEMPT_NOT_FOUND));
    }

    /**
     * To'plamni yakunlangan deb belgilaydi.
     *
     * @param questionCount javob yuborilgan paytdagi savollar soni - to'plamdan
     *                      o'chirilgan savollar ishtirokchining hisobiga kirmaydi
     */
    void markSubmitted(PollAttempt attempt, int questionCount) {
        attempt.setQuestionCount(questionCount);
        attempt.setSubmittedAt(Instant.now());
        attemptRepository.save(attempt);
    }

    /** Hisobot uchun: ishtirokchi belgisi -> unga berilgan savollar soni. */
    Map<String, Integer> submittedSizes(Long pollId) {
        Map<String, Integer> sizes = new HashMap<>();
        for (AttemptSizeRow row : attemptRepository.findSubmittedSizes(pollId)) {
            sizes.put(row.voterKey(), row.questionCount());
        }
        return sizes;
    }

    void deleteByPoll(Long pollId) {
        attemptRepository.deleteByPollId(pollId);
    }
}
