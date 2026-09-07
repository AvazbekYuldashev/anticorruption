package api.anticorruption.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Hisobni bloklash yoki blokdan chiqarish so'rovi.
 *
 * @param enabled {@code true} - hisob faol, {@code false} - bloklangan
 */
public record SetEnabledRequest(

        @NotNull(message = "{validation.required}")
        Boolean enabled
) {
}
