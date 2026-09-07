package api.anticorruption.complaint.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Murojaatni ochiq reyestrdan yashirish yoki qaytarish.
 *
 * @param hidden {@code true} - reyestrda ko'rinmaydi
 */
public record RegisterVisibilityRequest(

        @NotNull(message = "{validation.required}")
        Boolean hidden
) {
}
