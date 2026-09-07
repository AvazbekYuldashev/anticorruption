package api.anticorruption.poll;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.dto.PollResponse;
import api.anticorruption.poll.dto.PollStatisticsResponse;
import api.anticorruption.poll.dto.PollVoteRequest;
import api.anticorruption.poll.dto.QuizResultResponse;
import api.anticorruption.poll.dto.QuizStatisticsResponse;
import api.anticorruption.poll.dto.RestartPollRequest;
import api.anticorruption.poll.dto.SavePollRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** So'rovnomalar, ovoz berish va statistika. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final Translator translator;

    // ---------------------------------------------------------------- ochiq

    /** Saytda ko'rinmaydigan holatlar: hali e'lon qilinmagan va qo'lda to'xtatilgan. */
    private static final Set<PollStatus> HIDDEN_FROM_SITE =
            Set.of(PollStatus.SCHEDULED, PollStatus.STOPPED);

    /**
     * Saytda ko'rinadigan so'rovnomalar: ochiqlari va muddati tugaganlari.
     *
     * <p>Boshlanish sanasi hali kelmagan so'rovnoma ro'yxatga tushmaydi -
     * u hali e'lon qilinmagan hisoblanadi. Qo'lda to'xtatilgani ham
     * ko'rinmaydi, lekin natijalari admin panelida qoladi. Muddati
     * tugagani esa saytda qoladi: natijalari o'qishga arziydi.
     *
     * @param type             faqat shu turdagilari; null bo'lsa hammasi
     * @param voterKeyResolver so'rovnoma id si bo'yicha ovoz beruvchi belgisini qaytaradi.
     *                        Anonim tashrifchining belgisiga so'rovnoma id si ham kirgani
     *                        uchun u har bir so'rovnoma uchun alohida hisoblanadi.
     *                        {@code null} bo'lsa "ovoz berganmi" tekshirilmaydi.
     */
    @Transactional(readOnly = true)
    public List<PollResponse> listActive(PollType type, Function<Long, String> voterKeyResolver) {
        return pollRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(poll -> matchesType(poll, type))
                .filter(poll -> !HIDDEN_FROM_SITE.contains(poll.status()))
                .map(poll -> {
                    String voterKey = voterKeyResolver == null ? null : voterKeyResolver.apply(poll.getId());
                    return PollResponse.from(poll, hasVoted(poll.getId(), voterKey), translator);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PollResponse findById(Long pollId, String voterKeyOrNull) {
        Poll poll = requirePoll(pollId);
        return PollResponse.from(poll, hasVoted(pollId, voterKeyOrNull), translator);
    }

    /** Admin panelidagi tafsilot: to'g'ri javoblar va umumiy raqamlar bilan. */
    @Transactional(readOnly = true)
    public PollResponse findByIdForAdmin(Long pollId) {
        return PollResponse.forAdmin(requirePoll(pollId), translator);
    }

    /**
     * Ovozni qabul qiladi.
     *
     * <p>Butun so'rovnoma bir marta yuboriladi: ishtirokchi barcha savollarni
     * to'ldirib jo'natadi va keyin javoblarini o'zgartira olmaydi. Majburiy
     * bo'lmagan savolni ro'yxatga qo'shmasa, u javobsiz hisoblanadi.
     */
    @Transactional
    public PollResponse vote(Long pollId, PollVoteRequest request, String voterKey) {
        Poll poll = requirePoll(pollId);

        if (!poll.isOpenForVoting()) {
            throw new BadRequestException(MessageKeys.POLL_CLOSED);
        }
        if (pollVoteRepository.existsByPollIdAndVoterKey(pollId, voterKey)) {
            throw new ConflictException(MessageKeys.POLL_ALREADY_VOTED);
        }

        Map<PollQuestion, List<PollOption>> chosen = resolveAnswers(poll, request.answers());
        requireAllRequiredAnswered(poll, chosen.keySet());

        for (Map.Entry<PollQuestion, List<PollOption>> answer : chosen.entrySet()) {
            PollQuestion question = answer.getKey();
            question.setAnsweredCount(question.getAnsweredCount() + 1);

            for (PollOption option : answer.getValue()) {
                option.setVoteCount(option.getVoteCount() + 1);
                pollVoteRepository.save(PollVote.builder()
                        .poll(poll)
                        .question(question)
                        .option(option)
                        .voterKey(voterKey)
                        .build());
            }
        }

        poll.setVoterCount(poll.getVoterCount() + 1);
        pollRepository.save(poll);

        // Javob yuborilgandan keyin to'g'ri variantlarni ko'rsatsa bo'ladi.
        return PollResponse.from(poll, true, translator, true,
                poll.isQuiz() ? scoreQuiz(poll, chosen) : null);
    }

    /**
     * Test natijasini hisoblaydi.
     *
     * <p>Savol to'liq to'g'ri deb hisoblanadi: barcha to'g'ri variantlar
     * belgilangan va ortiqchasi tanlanmagan bo'lsa. Yarim javob ball
     * keltirmaydi - ko'p tanlovli savolda hammasini belgilab qo'yish
     * bilan ball olishning oldi olinadi.
     */
    private QuizResultResponse scoreQuiz(Poll poll, Map<PollQuestion, List<PollOption>> chosen) {
        List<QuizResultResponse.QuestionResult> results = new ArrayList<>(poll.getQuestions().size());

        for (PollQuestion question : poll.getQuestions()) {
            Set<Long> correctIds = new LinkedHashSet<>();
            for (PollOption option : question.getOptions()) {
                if (option.isCorrectAnswer()) {
                    correctIds.add(option.getId());
                }
            }

            Set<Long> chosenIds = new LinkedHashSet<>();
            for (PollOption option : chosen.getOrDefault(question, List.of())) {
                chosenIds.add(option.getId());
            }

            results.add(new QuizResultResponse.QuestionResult(
                    question.getId(),
                    !chosenIds.isEmpty() && chosenIds.equals(correctIds),
                    List.copyOf(chosenIds),
                    List.copyOf(correctIds)));
        }

        return QuizResultResponse.of(poll.getQuestions().size(), results);
    }

    /**
     * Yuborilgan javoblarni so'rovnomaning o'z savollari va variantlariga bog'laydi.
     *
     * <p>Tartib saqlanadi ({@link LinkedHashMap}) - xatolik yuz bersa qaysi
     * savolda ekani javobda aniq ko'rinsin.
     */
    private Map<PollQuestion, List<PollOption>> resolveAnswers(
            Poll poll, List<PollVoteRequest.QuestionAnswer> answers) {

        Map<Long, PollQuestion> questionsById = new HashMap<>();
        for (PollQuestion question : poll.getQuestions()) {
            questionsById.put(question.getId(), question);
        }

        Map<PollQuestion, List<PollOption>> chosen = new LinkedHashMap<>();

        for (PollVoteRequest.QuestionAnswer answer : answers) {
            PollQuestion question = questionsById.get(answer.questionId());
            if (question == null) {
                throw new BadRequestException(MessageKeys.POLL_UNKNOWN_QUESTION, answer.questionId());
            }
            if (chosen.containsKey(question)) {
                throw new BadRequestException(MessageKeys.POLL_DUPLICATE_ANSWER, question.getText());
            }

            // Takroriy id lar yuborilgan bo'lsa ham bitta tanlov sifatida qaraladi.
            Set<Long> optionIds = new LinkedHashSet<>(answer.optionIds());
            if (!question.isMultipleChoice() && optionIds.size() > 1) {
                throw new BadRequestException(MessageKeys.POLL_SINGLE_CHOICE_ONLY, question.getText());
            }

            chosen.put(question, resolveOptions(question, optionIds));
        }

        return chosen;
    }

    /** Tanlangan id lar aynan shu savolga tegishli ekanligini tekshiradi. */
    private List<PollOption> resolveOptions(PollQuestion question, Set<Long> optionIds) {
        Map<Long, PollOption> byId = new HashMap<>();
        for (PollOption option : question.getOptions()) {
            byId.put(option.getId(), option);
        }

        List<PollOption> chosen = new ArrayList<>(optionIds.size());
        for (Long optionId : optionIds) {
            PollOption option = byId.get(optionId);
            if (option == null) {
                throw new BadRequestException(MessageKeys.POLL_UNKNOWN_OPTION, optionId);
            }
            chosen.add(option);
        }
        return chosen;
    }

    private void requireAllRequiredAnswered(Poll poll, Set<PollQuestion> answered) {
        for (PollQuestion question : poll.getQuestions()) {
            if (question.isRequired() && !answered.contains(question)) {
                throw new BadRequestException(MessageKeys.POLL_QUESTION_REQUIRED, question.getText());
            }
        }
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<PollResponse> listAll(PollType type) {
        return pollRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(poll -> matchesType(poll, type))
                .map(poll -> PollResponse.forAdmin(poll, translator))
                .toList();
    }

    /**
     * Tur bo'yicha filtr.
     *
     * <p>Filtr SQL da emas, xotirada: turi ko'rsatilmagan eski yozuvlar
     * so'rovnoma hisoblanadi va buni {@code where} shartida ifodalash
     * null bilan cho'zilib ketardi. Ro'yxat kichik.
     */
    private boolean matchesType(Poll poll, PollType type) {
        return type == null || poll.typeOrSurvey() == type;
    }

    @Transactional(readOnly = true)
    public PollStatisticsResponse statistics(Long pollId) {
        Poll poll = requirePoll(pollId);

        return PollStatisticsResponse.from(
                poll,
                pollVoteRepository.findFirstVoteAt(pollId).orElse(null),
                pollVoteRepository.findLastVoteAt(pollId).orElse(null),
                poll.isQuiz() ? quizStatistics(poll) : null,
                translator);
    }

    /**
     * Test bo'yicha ball hisoboti: kim nechta savolga to'g'ri javob bergan.
     *
     * <p>Savol to'g'ri hisoblanishi uchun ishtirokchining butun tanlovi kerak,
     * shuning uchun ovozlar avval ishtirokchi va savol kesimida yig'iladi.
     * Hisob-kitob xotirada bajariladi: bitta so'rovnomaning ovozlari SQL da
     * to'plamlarni solishtirishdan ko'ra shu yerda soddaroq chiqadi.
     */
    private QuizStatisticsResponse quizStatistics(Poll poll) {
        Map<String, Map<Long, Set<Long>>> byVoter = new LinkedHashMap<>();

        for (QuizAnswerRow row : pollVoteRepository.findAnswerRows(poll.getId())) {
            byVoter.computeIfAbsent(row.voterKey(), key -> new HashMap<>())
                    .computeIfAbsent(row.questionId(), key -> new LinkedHashSet<>())
                    .add(row.optionId());
        }

        List<QuizStatisticsResponse.QuestionStat> stats = new ArrayList<>(poll.getQuestions().size());

        for (PollQuestion question : poll.getQuestions()) {
            Set<Long> correctIds = new LinkedHashSet<>();
            for (PollOption option : question.getOptions()) {
                if (option.isCorrectAnswer()) {
                    correctIds.add(option.getId());
                }
            }

            long answered = 0;
            long correct = 0;

            for (Map<Long, Set<Long>> answers : byVoter.values()) {
                Set<Long> chosen = answers.get(question.getId());
                if (chosen == null || chosen.isEmpty()) {
                    continue;
                }
                answered++;
                if (chosen.equals(correctIds)) {
                    correct++;
                }
            }

            stats.add(new QuizStatisticsResponse.QuestionStat(
                    question.getId(),
                    question.getText(),
                    answered,
                    correct,
                    answered <= 0 ? 0.0 : Math.round(correct * 1000.0 / answered) / 10.0));
        }

        return QuizStatisticsResponse.of(byVoter.size(), poll.getQuestions().size(), stats);
    }

    @Transactional
    public PollResponse create(SavePollRequest request) {
        validateWindow(request);

        Poll poll = Poll.builder()
                .title(request.title().trim())
                .description(blankToNull(request.description()))
                .type(request.type() == null ? PollType.SURVEY : request.type())
                .active(request.active() == null || request.active())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();

        applyQuestions(poll, request.questions());
        requireCorrectAnswers(poll);
        pollRepository.save(poll);

        log.info("{} yaratildi: id={}, savollar={}",
                poll.isQuiz() ? "Test" : "So'rovnoma", poll.getId(), poll.getQuestions().size());
        return PollResponse.forAdmin(poll, translator);
    }

    /**
     * Testda har bir savolda to'g'ri javob belgilangan bo'lishi shart.
     *
     * <p>Aks holda ishtirokchi hech qachon to'g'ri javob bera olmaydi: bunday
     * savolni saqlashga ruxsat berish xatoni natijalar chiqqanda ko'rsatardi.
     * Bitta variant tanlanadigan savolda to'g'ri javob ham bitta bo'ladi.
     */
    private void requireCorrectAnswers(Poll poll) {
        if (!poll.isQuiz()) {
            return;
        }

        for (PollQuestion question : poll.getQuestions()) {
            long correct = question.getOptions().stream()
                    .filter(PollOption::isCorrectAnswer)
                    .count();

            if (correct == 0) {
                throw new BadRequestException(MessageKeys.POLL_QUIZ_NO_CORRECT, question.getText());
            }
            if (!question.isMultipleChoice() && correct > 1) {
                throw new BadRequestException(MessageKeys.POLL_QUIZ_SINGLE_CORRECT, question.getText());
            }
        }
    }

    /**
     * So'rovnomani tahrirlaydi.
     *
     * <p>Id si yuborilgan savol va variantlar saqlanadi (ovozlari bilan),
     * yuborilmaganlari o'chiriladi, id siz kelganlari yangi deb qo'shiladi.
     */
    @Transactional
    public PollResponse update(Long pollId, SavePollRequest request) {
        validateWindow(request);
        Poll poll = requirePoll(pollId);

        poll.setTitle(request.title().trim());
        poll.setDescription(blankToNull(request.description()));
        poll.setStartsAt(request.startsAt());
        poll.setEndsAt(request.endsAt());
        if (request.type() != null) {
            poll.setType(request.type());
        }
        if (request.active() != null) {
            poll.setActive(request.active());
        }

        // Ovozlar variantlarga tashqi kalit bilan bog'langan, shuning uchun
        // ular variantlar o'chirilishidan oldin tozalanishi kerak.
        Set<Long> removedOptionIds = optionsAboutToBeRemoved(poll, request.questions());
        if (!removedOptionIds.isEmpty()) {
            pollVoteRepository.deleteByOptionIds(removedOptionIds);
        }

        applyQuestions(poll, request.questions());
        requireCorrectAnswers(poll);

        if (!removedOptionIds.isEmpty()) {
            recountVoters(poll);
        }

        pollRepository.save(poll);
        return PollResponse.forAdmin(poll, translator);
    }

    /**
     * Saqlashdan keyin qolmaydigan variantlarni topadi.
     *
     * <p>Savolning o'zi ro'yxatdan chiqarilgan bo'lsa, uning barcha
     * variantlari ham o'chadi.
     */
    private Set<Long> optionsAboutToBeRemoved(
            Poll poll, List<SavePollRequest.SavePollQuestionRequest> requested) {

        Set<Long> keptQuestionIds = new HashSet<>();
        Set<Long> keptOptionIds = new HashSet<>();

        for (SavePollRequest.SavePollQuestionRequest question : requested) {
            if (question.id() != null) {
                keptQuestionIds.add(question.id());
            }
            for (SavePollRequest.SavePollOptionRequest option : question.options()) {
                if (option.id() != null) {
                    keptOptionIds.add(option.id());
                }
            }
        }

        Set<Long> removed = new HashSet<>();
        for (PollQuestion question : poll.getQuestions()) {
            boolean questionKept = keptQuestionIds.contains(question.getId());
            for (PollOption option : question.getOptions()) {
                if (!questionKept || !keptOptionIds.contains(option.getId())) {
                    removed.add(option.getId());
                }
            }
        }
        return removed;
    }

    /**
     * Ovozlar o'chirilgandan keyin hisoblagichlarni qaytadan sanaydi.
     *
     * <p>Ishtirokchining yagona tanlovi o'chirilgan bo'lsa, u endi javob
     * bergan hisoblanmaydi - aks holda foizlar haqiqatdan katta chiqardi.
     */
    private void recountVoters(Poll poll) {
        poll.setVoterCount(pollVoteRepository.countVotersByPollId(poll.getId()));

        for (PollQuestion question : poll.getQuestions()) {
            if (question.getId() != null) {
                question.setAnsweredCount(pollVoteRepository.countVotersByQuestionId(question.getId()));
            }
        }
    }

    private void applyQuestions(Poll poll, List<SavePollRequest.SavePollQuestionRequest> requested) {
        Map<Long, PollQuestion> existing = new HashMap<>();
        for (PollQuestion question : poll.getQuestions()) {
            existing.put(question.getId(), question);
        }

        List<PollQuestion> updated = new ArrayList<>(requested.size());
        int order = 0;

        for (SavePollRequest.SavePollQuestionRequest item : requested) {
            PollQuestion question = item.id() == null ? null : existing.get(item.id());
            if (question == null) {
                question = PollQuestion.builder().poll(poll).build();
            }

            question.setText(item.text().trim());
            question.setDisplayOrder(order++);
            if (item.multipleChoice() != null) {
                question.setMultipleChoice(item.multipleChoice());
            }
            if (item.required() != null) {
                question.setRequired(item.required());
            }

            applyOptions(question, item.options());
            updated.add(question);
        }

        // orphanRemoval ro'yxatdan chiqarilgan savollarni ovozlari bilan o'chiradi.
        poll.getQuestions().clear();
        poll.getQuestions().addAll(updated);
    }

    private void applyOptions(
            PollQuestion question, List<SavePollRequest.SavePollOptionRequest> requested) {

        Map<Long, PollOption> existing = new HashMap<>();
        for (PollOption option : question.getOptions()) {
            existing.put(option.getId(), option);
        }

        List<PollOption> updated = new ArrayList<>(requested.size());
        int order = 0;

        for (SavePollRequest.SavePollOptionRequest item : requested) {
            PollOption option = item.id() == null ? null : existing.get(item.id());
            if (option == null) {
                option = PollOption.builder().question(question).build();
            }
            option.setText(item.text().trim());
            option.setDisplayOrder(order++);
            option.setCorrect(item.correct() != null && item.correct());
            updated.add(option);
        }

        question.getOptions().clear();
        question.getOptions().addAll(updated);
    }

    @Transactional
    public PollResponse setActive(Long pollId, boolean active) {
        Poll poll = requirePoll(pollId);
        if (poll.isActive() == active) {
            throw new BadRequestException(active
                    ? MessageKeys.POLL_ALREADY_ACTIVE
                    : MessageKeys.POLL_ALREADY_CLOSED);
        }
        poll.setActive(active);
        pollRepository.save(poll);
        return PollResponse.forAdmin(poll, translator);
    }

    /**
     * So'rovnomani qo'lda to'xtatadi yoki to'xtatishni bekor qiladi.
     *
     * <p>To'xtatilgan so'rovnoma saytda ko'rinmaydi va ovoz qabul qilmaydi,
     * lekin admin panelida natijalari o'z joyida qoladi.
     */
    @Transactional
    public PollResponse setStopped(Long pollId, boolean stopped) {
        Poll poll = requirePoll(pollId);

        if ((poll.getStoppedAt() != null) == stopped) {
            throw new BadRequestException(stopped
                    ? MessageKeys.POLL_ALREADY_STOPPED
                    : MessageKeys.POLL_NOT_STOPPED);
        }

        poll.setStoppedAt(stopped ? Instant.now() : null);
        pollRepository.save(poll);

        log.info("So'rovnoma {}: id={}", stopped ? "to'xtatildi" : "davom ettirildi", pollId);
        return PollResponse.forAdmin(poll, translator);
    }

    /**
     * So'rovnomani qaytadan o'tkazadi.
     *
     * <p>Eski yozuv o'zgartirilmaydi: u to'xtatiladi va hisoboti butunligicha
     * qoladi. Savollar va variantlar nusxasi bilan yangi so'rovnoma ochiladi,
     * uning hisoblagichlari noldan boshlanadi - shu tufayli yangi ovozlar
     * eskisiga qo'shilib ketmaydi va ilgarigi ishtirokchilar yana ovoz bera
     * oladi.
     *
     * @param request yangi muddat; null bo'lsa yangi o'tkazish muddatsiz boshlanadi
     */
    @Transactional
    public PollResponse restart(Long pollId, RestartPollRequest request) {
        Poll previous = requirePoll(pollId);
        Instant startsAt = request == null ? null : request.startsAt();
        Instant endsAt = request == null ? null : request.endsAt();

        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BadRequestException(MessageKeys.POLL_INVALID_WINDOW);
        }

        if (previous.getStoppedAt() == null) {
            previous.setStoppedAt(Instant.now());
            pollRepository.save(previous);
        }

        Poll next = Poll.builder()
                .title(previous.getTitle())
                .description(previous.getDescription())
                .type(previous.getType())
                .active(true)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .runNumber(previous.runNumberOrFirst() + 1)
                .previousPoll(previous)
                .build();

        for (PollQuestion question : previous.getQuestions()) {
            next.addQuestion(copyQuestion(question));
        }

        pollRepository.save(next);

        log.info("So'rovnoma qayta o'tkazilmoqda: eski id={}, yangi id={}, o'tkazish={}",
                previous.getId(), next.getId(), next.getRunNumber());

        return PollResponse.forAdmin(next, translator);
    }

    /** Savol nusxasi - matni bilan, ovozlarisiz. */
    private PollQuestion copyQuestion(PollQuestion source) {
        PollQuestion copy = PollQuestion.builder()
                .text(source.getText())
                .multipleChoice(source.isMultipleChoice())
                .required(source.isRequired())
                .displayOrder(source.getDisplayOrder())
                .build();

        for (PollOption option : source.getOptions()) {
            copy.addOption(PollOption.builder()
                    .text(option.getText())
                    .displayOrder(option.getDisplayOrder())
                    .correct(option.getCorrect())
                    .build());
        }
        return copy;
    }

    @Transactional
    public void delete(Long pollId) {
        Poll poll = requirePoll(pollId);

        // Ovozlar avval - ular variantlarga tashqi kalit bilan bog'langan.
        int votes = pollVoteRepository.deleteByPollId(pollId);
        pollRepository.detachSuccessors(pollId);
        pollRepository.delete(poll);

        log.info("So'rovnoma o'chirildi: id={} ({} ta ovoz bilan)", pollId, votes);
    }

    // ---------------------------------------------------------------- yordamchilar

    private void validateWindow(SavePollRequest request) {
        if (request.startsAt() != null && request.endsAt() != null
                && !request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException(MessageKeys.POLL_INVALID_WINDOW);
        }
    }

    private boolean hasVoted(Long pollId, String voterKey) {
        return voterKey != null && pollVoteRepository.existsByPollIdAndVoterKey(pollId, voterKey);
    }

    private Poll requirePoll(Long pollId) {
        return pollRepository.findWithQuestionsById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_POLL, pollId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
