package api.anticorruption.content.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Bo'lim xodimini qo'shish yoki tahrirlash. */
public record SaveStaffMemberRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 150, message = "{validation.size}")
        String fullName,

        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 200, message = "{validation.size}")
        String position,

        @Size(max = 150, message = "{validation.size.max}")
        String academicDegree,

        @Pattern(regexp = "^$|^[+]?[0-9]{9,15}$", message = "{validation.phone}")
        @Size(max = 30, message = "{validation.size.max}")
        String phone,

        @Email(message = "{validation.email}")
        @Size(max = 180, message = "{validation.size.max}")
        String email,

        @Size(max = 150, message = "{validation.size.max}")
        String receptionHours,

        Integer displayOrder,

        Boolean active
) {
}
