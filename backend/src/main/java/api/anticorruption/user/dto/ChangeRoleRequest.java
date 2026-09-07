package api.anticorruption.user.dto;

import api.anticorruption.user.Role;
import jakarta.validation.constraints.NotNull;

/** Foydalanuvchi rolini o'zgartirish so'rovi. */
public record ChangeRoleRequest(

        @NotNull(message = "{validation.required}")
        Role role
) {
}
