package api.anticorruption.poll;

import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.dto.PollGroupResponse;
import api.anticorruption.poll.dto.SavePollGroupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * So'rovnoma va test guruhlari.
 *
 * <p>Guruh - tartibga solish vositasi, ma'lumot egasi emas: guruh o'chirilsa
 * ichidagi so'rovnomalar saqlanib qoladi va "Guruhsiz" bo'limiga qaytadi.
 * Shu sababli bu yerda fakultetlardagi kabi "bog'langan yozuv bor, o'chirib
 * bo'lmaydi" qoidasi yo'q.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PollGroupService {

    private final PollGroupRepository groupRepository;
    private final Translator translator;

    @Transactional(readOnly = true)
    public List<PollGroupResponse> list(PollType type) {
        List<PollGroup> groups = type == null
                ? groupRepository.findAllByOrderByDisplayOrderAscNameAsc()
                : groupRepository.findByTypeOrderByDisplayOrderAscNameAsc(type);

        Map<Long, Long> counts = pollCounts();

        return groups.stream()
                .map(group -> PollGroupResponse.from(
                        group, counts.getOrDefault(group.getId(), 0L), translator))
                .toList();
    }

    @Transactional
    public PollGroupResponse create(SavePollGroupRequest request) {
        PollType type = request.type() == null ? PollType.SURVEY : request.type();
        String name = request.name().trim();
        requireNameFree(type, name, null);

        PollGroup group = PollGroup.builder()
                .name(name)
                .description(blankToNull(request.description()))
                .type(type)
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .questionsPerAttempt(questionsPerAttempt(type, request))
                .build();

        groupRepository.save(group);
        log.info("So'rovnoma guruhi yaratildi: id={}, turi={}, har biriga savol={}",
                group.getId(), type, group.getQuestionsPerAttempt());
        return PollGroupResponse.from(group, 0, translator);
    }

    /**
     * Guruhni tahrirlaydi.
     *
     * <p>Turi o'zgartirilmaydi: guruhni testlardan so'rovnomalarga ko'chirish
     * ichidagi barcha yozuvlarni boshqa sahifaga olib o'tardi va ular
     * yo'qolgandek ko'rinardi.
     */
    @Transactional
    public PollGroupResponse update(Long groupId, SavePollGroupRequest request) {
        PollGroup group = requireGroup(groupId);
        String name = request.name().trim();
        requireNameFree(group.getType(), name, groupId);

        group.setName(name);
        group.setDescription(blankToNull(request.description()));
        if (request.displayOrder() != null) {
            group.setDisplayOrder(request.displayOrder());
        }
        // Boshlangan to'plamlar o'zgarmaydi: yangi son faqat testni endi
        // boshlaydiganlarga ta'sir qiladi.
        group.setQuestionsPerAttempt(questionsPerAttempt(group.getType(), request));

        groupRepository.save(group);
        return PollGroupResponse.from(
                group, pollCounts().getOrDefault(groupId, 0L), translator);
    }

    /**
     * Bo'sh guruhni o'chiradi.
     *
     * <p>To'ldirilgan guruh o'chirilmaydi: har bir so'rovnoma guruhga tegishli
     * bo'lishi shart, shuning uchun guruhni yo'q qilish ichidagi yozuvlarni
     * ham yo'q qilishni yoki ularni qayerdadir "guruhsiz" holda qoldirishni
     * talab qilardi. Ikkalasi ham ma'lumot yo'qotadi - administrator avval
     * so'rovnomalarni boshqa guruhga ko'chiradi yoki o'chiradi.
     */
    @Transactional
    public void delete(Long groupId) {
        PollGroup group = requireGroup(groupId);

        long polls = groupRepository.countPolls(groupId);
        if (polls > 0) {
            throw new ConflictException(MessageKeys.POLL_GROUP_IN_USE, polls);
        }

        groupRepository.delete(group);
        log.info("So'rovnoma guruhi o'chirildi: id={}", groupId);
    }

    PollGroup requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageKeys.NOT_FOUND_POLL_GROUP, groupId));
    }

    /**
     * Har bir ishtirokchiga beriladigan savollar soni - faqat test guruhida.
     *
     * <p>Guruh yaratilayotganda testlarda hali savol bo'lmaydi, shuning uchun son
     * savollar bilan solishtirilmaydi: testda kamroq savol bo'lsa bori beriladi
     * ({@link Poll#drawSize()}).
     */
    private Integer questionsPerAttempt(PollType type, SavePollGroupRequest request) {
        return type == PollType.QUIZ ? request.questionsPerAttempt() : null;
    }

    private void requireNameFree(PollType type, String name, Long excludeId) {
        if (groupRepository.nameTaken(type, name, excludeId)) {
            throw new ConflictException(MessageKeys.POLL_GROUP_NAME_TAKEN, name);
        }
    }

    private Map<Long, Long> pollCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : groupRepository.countByGroup()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
