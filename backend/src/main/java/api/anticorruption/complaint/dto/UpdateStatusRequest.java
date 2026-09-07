package api.anticorruption.complaint.dto;

import api.anticorruption.complaint.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Murojaat holatini o'zgartirish so'rovi.
 *
 * @param status           yangi holat
 * @param note             ichki izoh - faqat xodimlarga ko'rinadi
 * @param officialResponse murojaatchiga ko'rinadigan rasmiy javob (ixtiyoriy)
 */
public record UpdateStatusRequest(

        @NotNull(message = "{validation.required}")
        ComplaintStatus status,

        @Size(max = 2000, message = "{validation.size.max}")
        String note,

        @Size(max = 5000, message = "{validation.size.max}")
        String officialResponse
) {
}
