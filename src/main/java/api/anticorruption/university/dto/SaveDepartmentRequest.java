package api.anticorruption.university.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Kafedra yaratish yoki tahrirlash so'rovi. Fakultet URL manzilidan olinadi. */
public record SaveDepartmentRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 200, message = "{validation.size}")
        String name,

        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 20, message = "{validation.size}")
        String code,

        Boolean active
) {
}
