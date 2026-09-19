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
import java.util.Locale;
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
    private final PollGroupService pollGroupService;
    private final PollAttemptService attemptService;
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
                    // Tasodifiy testning savollari ro'yxatda berilmaydi: har bir
                    // ishtirokchi o'z to'plamini testni boshlaganda oladi.
                    List<PollQuestion> questions = poll.isRandomized() ? List.of() : poll.getQuestions();
                    return PollResponse.from(poll, questions, hasVoted(poll.getId(), voterKey), translator);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PollResponse findById(Long pollId, String voterKeyOrNull) {
        Poll poll = requirePoll(pollId);
        boolean voted = hasVoted(pollId, voterKeyOrNull);
        return PollResponse.from(poll, siteQuestions(poll, voterKeyOrNull, voted), voted, translator);
    }

    /**
     * Ochiq sahifada ko'rsatiladigan savollar.
     *
     * <p>Tasodifiy testda butun savollar bazasi hech qachon ochilmaydi:
     * ishtirokchi o'z to'plamini testni boshlaganda oladi, ishlab bo'lgach esa
     * faqat o'shani to'g'ri javoblari bilan ko'radi. Aks holda bir marta test
     * ishlagan odam barcha savollarning javobini ko'rib, tarqatib yuborardi.
     */
    private List<PollQuestion> siteQuestions(Poll poll, String voterKey, boolean voted) {
        if (!poll.isRandomized()) {
            return poll.getQuestions();
        }
        if (!voted) {
            return List.of();
        }
        return attemptService.findForVoter(poll, voterKey)
                .map(attempt -> attemptService.questionsOf(poll, attempt))
                .orElse(List.of());
    }

    /**
     * Testni boshlaydi va ishtirokchiga tushgan savollarni qaytaradi.
     *
     * <p>Savollari tasodifiy tanlanadigan testda to'plam shu yerda tanlanadi va
     * saqlanadi. Qayta boshlaganda o'sha to'plam qaytadi - sahifani yangilab
     * boshqa savollar olib bo'lmaydi.
     *
     * <p>Barcha savollari beriladigan so'rovnoma va testda hech narsa
     * saqlanmaydi, savollar qaytadi xolos: interfeys ikkala holatni bir xil
     * ishlata olsin.
     */
    @Transactional
    public PollResponse start(Long pollId, String voterKey) {
        Poll poll = requirePoll(pollId);

        if (!poll.isOpenForVoting()) {
            throw new BadRequestException(MessageKeys.POLL_CLOSED);
        }
        if (pollVoteRepository.existsByPollIdAndVoterKey(pollId, voterKey)) {
            throw new ConflictException(MessageKeys.POLL_ALREADY_VOTED);
        }
        if (!poll.isRandomized()) {
            return PollResponse.from(poll, poll.getQuestions(), false, translator);
        }

        PollAttempt attempt = attemptService.startOrResume(poll, voterKey);
        return PollResponse.forAttempt(
                poll, attemptService.questionsOf(poll, attempt), attempt.getToken(), translator);
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
     *
     * <p>Savollari tasodifiy tanlanadigan testda javoblar ishtirokchiga tushgan
     * to'plamga qarab tekshiriladi va baholanadi: bazadagi boshqa savollarga
     * javob qabul qilinmaydi, natija esa to'plamdagi savollar sonidan chiqadi.
     */
    @Transactional
    public PollResponse vote(Long pollId, PollVoteRequest request, String voterKey) {
        Poll poll = requirePoll(pollId);

        if (!poll.isOpenForVoting()) {
            throw new BadRequestException(MessageKeys.POLL_CLOSED);
        }

        PollAttempt attempt = attemptService.resolveForVote(poll, request.attemptToken(), voterKey);
        // Ovoz to'plam egasi nomidan yoziladi: anonim ishtirokchining IP manzili
        // test davomida o'zgargan bo'lsa ham u "boshqa odam" bo'lib qolmaydi.
        String owner = attempt == null ? voterKey : attempt.getVoterKey();

        if ((attempt != null && attempt.isSubmitted())
                || pollVoteRepository.existsByPollIdAndVoterKey(pollId, owner)) {
            throw new ConflictException(MessageKeys.POLL_ALREADY_VOTED);
        }

        List<PollQuestion> questions = attempt == null
                ? poll.getQuestions()
                : attemptService.questionsOf(poll, attempt);

        Map<PollQuestion, List<PollOption>> chosen = resolveAnswers(questions, request.answers());
        requireAllRequiredAnswered(questions, chosen.keySet());

        for (Map.Entry<PollQuestion, List<PollOption>> answer : chosen.entrySet()) {
            PollQuestion question = answer.getKey();
            question.setAnsweredCount(question.getAnsweredCount() + 1);

            for (PollOption option : answer.getValue()) {
                option.setVoteCount(option.getVoteCount() + 1);
                pollVoteRepository.save(PollVote.builder()
                        .poll(poll)
                        .question(question)
                        .option(option)
                        .voterKey(owner)
                        .build());
            }
        }

        poll.setVoterCount(poll.getVoterCount() + 1);
        pollRepository.save(poll);

        if (attempt != null) {
            attemptService.markSubmitted(attempt, questions.size());
        }

        // Javob yuborilgandan keyin to'g'ri variantlarni ko'rsatsa bo'ladi.
        return PollResponse.afterVote(poll, questions,
                poll.isQuiz() ? scoreQuiz(questions, chosen) : null, translator);
    }

    /**
     * Test natijasini hisoblaydi.
     *
     * <p>Savol to'liq to'g'ri deb hisoblanadi: barcha to'g'ri variantlar
     * belgilangan va ortiqchasi tanlanmagan bo'lsa. Yarim javob ball
     * keltirmaydi - ko'p tanlovli savolda hammasini belgilab qo'yish
     * bilan ball olishning oldi olinadi.
     *
     * @param questions ishtirokchiga berilgan savollar - natija shular sonidan chiqadi
     */
    private QuizResultResponse scoreQuiz(
            List<PollQuestion> questions, Map<PollQuestion, List<PollOption>> chosen) {

        List<QuizResultResponse.QuestionResult> results = new ArrayList<>(questions.size());

        for (PollQuestion question : questions) {
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

        return QuizResultResponse.of(questions.size(), results);
    }

    /**
     * Yuborilgan javoblarni so'rovnomaning o'z savollari va variantlariga bog'laydi.
     *
     * <p>Tartib saqlanadi ({@link LinkedHashMap}) - xatolik yuz bersa qaysi
     * savolda ekani javobda aniq ko'rinsin.
     *
     * @param questions ishtirokchiga berilgan savollar; boshqasiga javob qabul qilinmaydi
     */
    private Map<PollQuestion, List<PollOption>> resolveAnswers(
            List<PollQuestion> questions, List<PollVoteRequest.QuestionAnswer> answers) {

        Map<Long, PollQuestion> questionsById = new HashMap<>();
        for (PollQuestion question : questions) {
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

    private void requireAllRequiredAnswered(List<PollQuestion> questions, Set<PollQuestion> answered) {
        for (PollQuestion question : questions) {
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
        Map<String, Integer> drawSizes = attemptService.submittedSizes(pollId);

        return PollStatisticsResponse.from(
                poll,
                pollVoteRepository.findFirstVoteAt(pollId).orElse(null),
                pollVoteRepository.findLastVoteAt(pollId).orElse(null),
                expectedAnswers(poll, drawSizes),
                poll.isQuiz() ? quizStatistics(poll, drawSizes) : null,
                translator);
    }

    /**
     * Ishtirokchilarga jami nechta savol berilgan.
     *
     * <p>To'plami bor ishtirokchiga o'z to'plamidagi savollar, qolganlariga
     * barcha savollar berilgan. Aralash holat testda tasodifiy tanlov o'tkazish
     * davomida yoqilgan yoki o'chirilganda uchraydi.
     */
    private long expectedAnswers(Poll poll, Map<String, Integer> drawSizes) {
        long drawn = drawSizes.values().stream().mapToLong(Integer::longValue).sum();
        long withoutDraw = Math.max(0, poll.getVoterCount() - drawSizes.size());
        return drawn + withoutDraw * poll.getQuestions().size();
    }

    /**
     * Test bo'yicha ball hisoboti: kim nechta savolga to'g'ri javob bergan.
     *
     * <p>Savol to'g'ri hisoblanishi uchun ishtirokchining butun tanlovi kerak,
     * shuning uchun ovozlar avval ishtirokchi va savol kesimida yig'iladi.
     * Hisob-kitob xotirada bajariladi: bitta so'rovnomaning ovozlari SQL da
     * to'plamlarni solishtirishdan ko'ra shu yerda soddaroq chiqadi.
     *
     * <p>Savol kesimidagi ulush o'sha savolga javob berganlarga nisbatan olinadi,
     * shuning uchun tasodifiy testda ham to'g'ri chiqadi: har bir savolni
     * ishtirokchilarning faqat bir qismi olgan.
     *
     * @param drawSizes ishtirokchi belgisi -> unga tushgan savollar soni
     */
    private QuizStatisticsResponse quizStatistics(Poll poll, Map<String, Integer> drawSizes) {
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

        // Har bir ishtirokchi o'ziga berilgan savollar sonidan baholanadi.
        long expected = 0;
        for (String voterKey : byVoter.keySet()) {
            expected += drawSizes.getOrDefault(voterKey, poll.getQuestions().size());
        }

        return QuizStatisticsResponse.of(byVoter.size(), expected, stats);
    }

    @Transactional
    public PollResponse create(SavePollRequest request) {
        validateWindow(request);

        PollType type = request.type() == null ? PollType.SURVEY : request.type();

        Poll poll = Poll.builder()
                .title(request.title().trim())
                .description(blankToNull(request.description()))
                .type(type)
                .active(request.active() == null || request.active())
                .group(resolveGroup(request.groupId(), type))
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();

        applyQuestions(poll, request.questions());
        requireCorrectAnswers(poll);
        requireUniqueQuestions(poll);
        pollRepository.save(poll);

        log.info("{} yaratildi: id={}, savollar={}",
                poll.isQuiz() ? "Test" : "So'rovnoma", poll.getId(), poll.getQuestions().size());
        return PollResponse.forAdmin(poll, translator);
    }

    /**
     * Bitta savol ikki marta kiritilmasin.
     *
     * <p>Yuzlab savollik bazada bir savolni ikki marta kiritib qo'yish oson,
     * tasodifiy tanlovda esa ikkalasi bitta ishtirokchiga tushib qolishi
     * mumkin. Savol takroriy hisoblanadi, agar matni ham, variantlari ham bir
     * xil bo'lsa - katta-kichik harf, ortiqcha bo'shliq va variantlar tartibi
     * hisobga olinmaydi. Faqat matni bir xil savollar takroriy emas: "To'g'ri
     * javobni belgilang" kabi savol turli variantlar bilan ko'p uchraydi.
     */
    private void requireUniqueQuestions(Poll poll) {
        Set<String> seen = new HashSet<>();
        for (PollQuestion question : poll.getQuestions()) {
            if (!seen.add(contentKey(question))) {
                throw new BadRequestException(MessageKeys.POLL_DUPLICATE_QUESTION, question.getText());
            }
        }
    }

    private static String contentKey(PollQuestion question) {
        List<String> options = question.getOptions().stream()
                .map(option -> normalizeText(option.getText()))
                .sorted()
                .toList();

        // Qator ko'chishi solishtiriladigan matnlarda qolmaydi - ajratgich sifatida xavfsiz.
        return normalizeText(question.getText()) + "\n" + String.join("\n", options);
    }

    private static String normalizeText(String text) {
        return text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
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
        poll.setGroup(resolveGroup(request.groupId(), poll.typeOrSurvey()));

        // Ovozlar variantlarga tashqi kalit bilan bog'langan, shuning uchun
        // ular variantlar o'chirilishidan oldin tozalanishi kerak.
        Set<Long> removedOptionIds = optionsAboutToBeRemoved(poll, request.questions());
        if (!removedOptionIds.isEmpty()) {
            pollVoteRepository.deleteByOptionIds(removedOptionIds);
        }

        applyQuestions(poll, request.questions());
        requireCorrectAnswers(poll);
        requireUniqueQuestions(poll);

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
                // Yangi o'tkazish eskisi turgan guruhda qoladi: qayta
                // o'tkazish tartibni o'zgartirmasligi kerak.
                .group(previous.getGroup())
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

        // Ovozlar va to'plamlar avval - ular so'rovnomaga tashqi kalit bilan bog'langan.
        int votes = pollVoteRepository.deleteByPollId(pollId);
        attemptService.deleteByPoll(pollId);
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

    /**
     * So'rovnomani boshqa guruhga ko'chiradi.
     *
     * <p>Alohida amal: ro'yxatdan turib ko'chirish uchun to'liq tahrirlash
     * shaklini ochish (savollar, variantlar, muddat bilan) ortiqcha bo'lardi.
     */
    @Transactional
    public PollResponse setGroup(Long pollId, Long groupId) {
        Poll poll = requirePoll(pollId);
        poll.setGroup(resolveGroup(groupId, poll.typeOrSurvey()));
        pollRepository.save(poll);
        return PollResponse.forAdmin(poll, translator);
    }

    /**
     * Guruh id sini guruhga aylantiradi.
     *
     * <p>Guruh majburiy: har bir so'rovnoma qaysidir o'tkazishga tegishli
     * bo'lishi kerak. Turi ham mos kelishi shart - testni so'rovnomalar
     * guruhiga qo'yish uni test sahifasidan yo'qotardi, chunki o'sha guruh
     * u yerda umuman chiqmaydi.
     */
    private PollGroup resolveGroup(Long groupId, PollType pollType) {
        if (groupId == null) {
            throw new BadRequestException(MessageKeys.POLL_GROUP_REQUIRED);
        }
        PollGroup group = pollGroupService.requireGroup(groupId);
        if (group.getType() != pollType) {
            throw new BadRequestException(MessageKeys.POLL_GROUP_TYPE_MISMATCH, group.getName());
        }
        return group;
    }

    private Poll requirePoll(Long pollId) {
        return pollRepository.findWithQuestionsById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_POLL, pollId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
