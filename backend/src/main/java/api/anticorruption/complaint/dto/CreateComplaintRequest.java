package api.anticorruption.complaint.dto;

import api.anticorruption.complaint.AccusedPosition;
import api.anticorruption.complaint.ComplaintCategory;
import api.anticorruption.complaint.ReporterType;
import api.anticorruption.complaint.StudyForm;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Yangi murojaat yuborish so'rovi.
 *
 * <p>Murojaatni tizimga kirmasdan ham yuborish mumkin. Agar {@code anonymous}
 * true bo'lsa, murojaatchining ismi va telefoni saqlanmaydi - faqat ixtiyoriy
 * email qoladi (xabarnoma yuborish uchun).
 *
 * <p>Majburiy maydonlar atigi to'rtta: sarlavha, matn, holat turi va
 * murojaatchi maqomi. Qolganlari ixtiyoriy - murojaat yuborishni
 * qiyinlashtirmaslik uchun.
 */
public record CreateComplaintRequest(

        @NotBlank(message = "{validation.required}")
        @Size(min = 10, max = 200, message = "{validation.size}")
        String title,

        @NotBlank(message = "{validation.required}")
        @Size(min = 30, max = 10000, message = "{validation.size}")
        String description,

        @NotNull(message = "{validation.required}")
        ComplaintCategory category,

        @NotNull(message = "{validation.required}")
        ReporterType reporterType,

        // --- universitet konteksti (ixtiyoriy) ---

        Long facultyId,

        Long departmentId,

        @Size(max = 200, message = "{validation.size.max}")
        String subjectName,

        AccusedPosition accusedPosition,

        // --- voqea ---

        @PastOrPresent(message = "{validation.pastOrPresent}")
        LocalDate incidentDate,

        @Size(max = 250, message = "{validation.size.max}")
        String incidentPlace,

        // --- talaba ma'lumotlari (ixtiyoriy) ---

        @Min(value = 1, message = "{validation.min}")
        @Max(value = 7, message = "{validation.max}")
        Integer courseYear,

        @Size(max = 50, message = "{validation.size.max}")
        String groupName,

        StudyForm studyForm,

        // --- murojaatchi ---

        /*
         * Ataylab Boolean (primitiv boolean emas): maydon umuman yuborilmasa
         * ham so'rov qabul qilinishi kerak. Kompakt konstruktor uni false ga
         * keltiradi, shuning uchun keyinchalik null bo'lmaydi.
         */
        Boolean anonymous,

        @Size(max = 150, message = "{validation.size.max}")
        String reporterName,

        @Email(message = "{validation.email}")
        @Size(max = 180, message = "{validation.size.max}")
        String reporterEmail,

        @Pattern(regexp = "^$|^[+]?[0-9]{9,15}$", message = "{validation.phone}")
        @Size(max = 30, message = "{validation.size.max}")
        String reporterPhone
) {
    public CreateComplaintRequest {
        if (anonymous == null) {
            anonymous = false;
        }
    }
}
