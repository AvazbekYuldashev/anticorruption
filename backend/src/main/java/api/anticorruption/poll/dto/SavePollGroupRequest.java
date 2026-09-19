package api.anticorruption.poll.dto;

import api.anticorruption.poll.PollType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Guruh yaratish yoki tahrirlash.
 *
 * @param type                so'rovnomalar guruhimi yoki testlar guruhi; ko'rsatilmasa
 *                            so'rovnoma. Tahrirlashda o'zgartirilmaydi - guruh turini
 *                            almashtirish ichidagi so'rovnomalarni boshqa sahifaga
 *                            ko'chirib yuborardi
 * @param displayOrder        ro'yxatdagi tartib; berilmasa 0
 * @param questionsPerAttempt faqat test guruhida: guruhdagi har bir testda ishtirokchiga
 *                            savollardan nechtasi tasodifiy tanlab berilsin (masalan,
 *                            200 tadan 50 tasi). null bo'lsa barcha savollar kiritilgan
 *                            tartibda beriladi. Yuqori chegara testdagi savollar
 *                            chegarasi bilan bir xil
 */
public record SavePollGroupRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 200, message = "{validation.size}")
        String name,

        @Size(max = 500, message = "{validation.size.max}")
        String description,

        PollType type,

        Integer displayOrder,

        @Min(value = 1, message = "{validation.min}")
        @Max(value = 500, message = "{validation.max}")
        Integer questionsPerAttempt
) {
}
