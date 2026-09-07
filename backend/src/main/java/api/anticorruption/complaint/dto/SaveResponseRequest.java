package api.anticorruption.complaint.dto;

import jakarta.validation.constraints.Size;

/**
 * Murojaatga javob yozish - holatni o'zgartirmasdan.
 *
 * <p>Holat o'zgarishisiz javob kerak bo'ladigan ikki holat bor: murojaat hali
 * ko'rib chiqilayotganda oraliq javob berish va allaqachon yopilgan murojaatga
 * qo'shimcha izoh yozish (yopilgandan keyin holat o'tishlari tugaydi, ammo
 * murojaatchiga aytadigan gap qolgan bo'lishi mumkin).
 *
 * @param officialResponse murojaatchiga ko'rinadigan rasmiy javob
 * @param note             ichki izoh - faqat xodimlarga ko'rinadi
 */
public record SaveResponseRequest(

        @Size(max = 5000, message = "{validation.size.max}")
        String officialResponse,

        @Size(max = 2000, message = "{validation.size.max}")
        String note
) {
}
