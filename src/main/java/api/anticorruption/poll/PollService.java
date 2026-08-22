package api.anticorruption.poll;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.poll.dto.PollResponse;
import api.anticorruption.poll.dto.PollVoteRequest;
import api.anticorruption.poll.dto.SavePollRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** So'rovnomalar va ovoz berish. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;

    // ---------------------------------------------------------------- ochiq

    /**
     * Faol so'rovnomalar.
     *
     * @param voterKeyResolver so'rovnoma id si bo'yicha ovoz beruvchi belgisini qaytaradi.
     *                        Anonim tashrifchining belgisiga so'rovnoma id si ham kirgani
     *                        uchun u har bir so'rovnoma uchun alohida hisoblanadi.
     *                        {@code null} bo'lsa "ovoz berganmi" tekshirilmaydi.
     */
    @Transactional(readOnly = true)
    public List<PollResponse> listActive(Function<Long, String> voterKeyResolver) {
        return pollRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(poll -> {
                    String voterKey = voterKeyResolver == null ? null : voterKeyResolver.apply(poll.getId());
                    return PollResponse.from(poll, hasVoted(poll.getId(), voterKey));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PollResponse findById(Long pollId, String voterKeyOrNull) {
        Poll poll = requirePoll(pollId);
        return PollResponse.from(poll, hasVoted(pollId, voterKeyOrNull));
    }

    /**
     * Ovozni qabul qiladi.
     *
     * <p>Bitta odam bir so'rovnomada bir marta ovoz beradi. Ko'p tanlovli
     * so'rovnomada bitta yuborishda bir nechta variant belgilanadi, lekin
     * keyin qaytib o'zgartirib bo'lmaydi.
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

        // Takroriy id lar yuborilgan bo'lsa ham bitta ovoz sifatida qaraladi.
        Set<Long> chosenIds = new LinkedHashSet<>(request.optionIds());

        if (!poll.isMultipleChoice() && chosenIds.size() > 1) {
            throw new BadRequestException(MessageKeys.POLL_SINGLE_CHOICE_ONLY);
        }

        List<PollOption> chosen = resolveOptions(poll, chosenIds);

        for (PollOption option : chosen) {
            option.setVoteCount(option.getVoteCount() + 1);
            pollVoteRepository.save(PollVote.builder()
                    .poll(poll)
                    .option(option)
                    .voterKey(voterKey)
                    .build());
        }
        poll.setVoterCount(poll.getVoterCount() + 1);
        pollRepository.save(poll);

        return PollResponse.from(poll, true);
    }

    /** Tanlangan id lar shu so'rovnomaga tegishli ekanligini tekshiradi. */
    private List<PollOption> resolveOptions(Poll poll, Set<Long> chosenIds) {
        Map<Long, PollOption> byId = new HashMap<>();
        for (PollOption option : poll.getOptions()) {
            byId.put(option.getId(), option);
        }

        List<PollOption> chosen = new ArrayList<>(chosenIds.size());
        for (Long optionId : chosenIds) {
            PollOption option = byId.get(optionId);
            if (option == null) {
                throw new BadRequestException(MessageKeys.POLL_UNKNOWN_OPTION, optionId);
            }
            chosen.add(option);
        }
        return chosen;
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<PollResponse> listAll() {
        return pollRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(poll -> PollResponse.from(poll, false))
                .toList();
    }

    @Transactional
    public PollResponse create(SavePollRequest request) {
        validateWindow(request);

        Poll poll = Poll.builder()
                .question(request.question().trim())
                .description(blankToNull(request.description()))
                .multipleChoice(Boolean.TRUE.equals(request.multipleChoice()))
                .active(request.active() == null || request.active())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();

        int order = 0;
        for (SavePollRequest.SavePollOptionRequest option : request.options()) {
            poll.addOption(PollOption.builder()
                    .text(option.text().trim())
                    .displayOrder(order++)
                    .build());
        }

        pollRepository.save(poll);
        log.info("So'rovnoma yaratildi: id={}, variantlar={}", poll.getId(), poll.getOptions().size());

        return PollResponse.from(poll, false);
    }

    /**
     * So'rovnomani tahrirlaydi.
     *
     * <p>Id si yuborilgan variantlar saqlanadi (ovozlari bilan), yuborilmaganlari
     * o'chiriladi, id siz kelganlari yangi variant sifatida qo'shiladi.
     */
    @Transactional
    public PollResponse update(Long pollId, SavePollRequest request) {
        validateWindow(request);
        Poll poll = requirePoll(pollId);

        poll.setQuestion(request.question().trim());
        poll.setDescription(blankToNull(request.description()));
        poll.setStartsAt(request.startsAt());
        poll.setEndsAt(request.endsAt());
        if (request.multipleChoice() != null) {
            poll.setMultipleChoice(request.multipleChoice());
        }
        if (request.active() != null) {
            poll.setActive(request.active());
        }

        applyOptions(poll, request.options());

        pollRepository.save(poll);
        return PollResponse.from(poll, false);
    }

    private void applyOptions(Poll poll, List<SavePollRequest.SavePollOptionRequest> requested) {
        Map<Long, PollOption> existing = new HashMap<>();
        for (PollOption option : poll.getOptions()) {
            existing.put(option.getId(), option);
        }

        List<PollOption> updated = new ArrayList<>(requested.size());
        int order = 0;

        for (SavePollRequest.SavePollOptionRequest item : requested) {
            PollOption option = item.id() == null ? null : existing.get(item.id());
            if (option == null) {
                option = PollOption.builder().poll(poll).build();
            }
            option.setText(item.text().trim());
            option.setDisplayOrder(order++);
            updated.add(option);
        }

        // orphanRemoval ro'yxatdan chiqarilgan variantlarni o'chiradi.
        poll.getOptions().clear();
        poll.getOptions().addAll(updated);
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
        return PollResponse.from(poll, false);
    }

    @Transactional
    public void delete(Long pollId) {
        pollRepository.delete(requirePoll(pollId));
        log.info("So'rovnoma o'chirildi: id={}", pollId);
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
        return pollRepository.findWithOptionsById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_POLL, pollId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
