package api.anticorruption.poll.dto;

import api.anticorruption.common.i18n.Translator;
import api.anticorruption.poll.PollGroup;
import api.anticorruption.poll.PollType;

import java.time.Instant;

/**
 * So'rovnoma yoki testlar guruhi.
 *
 * @param pollCount           guruhdagi so'rovnomalar soni - ro'yxatda sarlavha yonida
 *                            ko'rsatiladi, shuning uchun har bir guruh uchun alohida
 *                            so'rov yubormaslik kerak
 * @param questionsPerAttempt test guruhida: har bir ishtirokchiga nechta savol tasodifiy
 *                            beriladi; barcha savollar berilsa null
 */
public record PollGroupResponse(
        Long id,
        String name,
        String description,
        PollType type,
        String typeLabel,
        int displayOrder,
        long pollCount,
        Integer questionsPerAttempt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PollGroupResponse from(PollGroup group, long pollCount, Translator translator) {
        return new PollGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getType(),
                translator.of(group.getType()),
                group.getDisplayOrder(),
                pollCount,
                group.getQuestionsPerAttempt(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }
}
