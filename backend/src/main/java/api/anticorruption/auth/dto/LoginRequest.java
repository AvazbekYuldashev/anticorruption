package api.anticorruption.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Tizimga kirish so'rovi. */
public record LoginRequest(

        @NotBlank(message = "{validation.required}")
        String email,

        @NotBlank(message = "{validation.required}")
        String password
) {
}
