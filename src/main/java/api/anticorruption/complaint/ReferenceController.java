package api.anticorruption.complaint;

import api.anticorruption.common.LabeledEnum;
import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.dto.EnumOption;
import api.anticorruption.university.UniversityService;
import api.anticorruption.university.dto.DepartmentResponse;
import api.anticorruption.university.dto.FacultyResponse;
import api.anticorruption.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Murojaat shaklini to'ldirish uchun ma'lumotnomalar.
 *
 * <p>Ochiq yo'l - shaklni tizimga kirmasdan ham to'ldirish kerak.
 * Nomlar joriy tilda qaytadi: {@code ?lang=ru} qo'shilsa ruscha keladi.
 */
@Tag(name = "Ma'lumotnomalar", description = "Kategoriya, fakultet, maqom va holat ro'yxatlari")
@RestController
@RequestMapping("/api/v1/reference")
@RequiredArgsConstructor
public class ReferenceController {

    private final UniversityService universityService;
    private final Translator translator;

    @Operation(summary = "Qo'llab-quvvatlanadigan tillar", description = "?lang= parametriga beriladigan qiymatlar")
    @GetMapping("/languages")
    public ResponseEntity<List<EnumOption>> languages() {
        return ResponseEntity.ok(Arrays.stream(AppLanguage.values())
                .map(language -> new EnumOption(language.getCode(), language.getDisplayName()))
                .toList());
    }

    @Operation(summary = "Korrupsiya holatlarining turlari")
    @GetMapping("/categories")
    public ResponseEntity<List<EnumOption>> categories() {
        return ResponseEntity.ok(options(ComplaintCategory.values()));
    }

    @Operation(summary = "Murojaat holatlari")
    @GetMapping("/statuses")
    public ResponseEntity<List<EnumOption>> statuses() {
        return ResponseEntity.ok(options(ComplaintStatus.values()));
    }

    @Operation(summary = "Murojaatchi maqomlari", description = "Talaba, o'qituvchi, xodim va h.k.")
    @GetMapping("/reporter-types")
    public ResponseEntity<List<EnumOption>> reporterTypes() {
        return ResponseEntity.ok(options(ReporterType.values()));
    }

    @Operation(summary = "Ta'lim shakllari")
    @GetMapping("/study-forms")
    public ResponseEntity<List<EnumOption>> studyForms() {
        return ResponseEntity.ok(options(StudyForm.values()));
    }

    @Operation(summary = "Lavozimlar", description = "Murojaat kimning harakati haqida ekanligini ko'rsatish uchun")
    @GetMapping("/positions")
    public ResponseEntity<List<EnumOption>> positions() {
        return ResponseEntity.ok(options(AccusedPosition.values()));
    }

    @Operation(summary = "Rollar", description = "Admin panelida rol tanlash uchun")
    @GetMapping("/roles")
    public ResponseEntity<List<EnumOption>> roles() {
        return ResponseEntity.ok(options(Role.values()));
    }

    @Operation(summary = "Fakultetlar", description = "Faqat faol fakultetlar, kafedralari bilan birga")
    @GetMapping("/faculties")
    public ResponseEntity<List<FacultyResponse>> faculties() {
        return ResponseEntity.ok(universityService.listActiveFaculties());
    }

    @Operation(summary = "Fakultet kafedralari", description = "Faqat faol kafedralar")
    @GetMapping("/faculties/{facultyId}/departments")
    public ResponseEntity<List<DepartmentResponse>> departments(@PathVariable Long facultyId) {
        return ResponseEntity.ok(universityService.listDepartments(facultyId, false));
    }

    @Operation(
            summary = "Barcha ma'lumotnomalar",
            description = "Murojaat shakli uchun kerak bo'ladigan hamma ro'yxatni bitta so'rovda qaytaradi")
    @GetMapping
    public ResponseEntity<Map<String, Object>> all() {
        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("language", translator.currentLanguage().getCode());
        reference.put("languages", languages().getBody());
        reference.put("categories", options(ComplaintCategory.values()));
        reference.put("statuses", options(ComplaintStatus.values()));
        reference.put("reporterTypes", options(ReporterType.values()));
        reference.put("studyForms", options(StudyForm.values()));
        reference.put("positions", options(AccusedPosition.values()));
        reference.put("roles", options(Role.values()));
        reference.put("faculties", universityService.listActiveFaculties());
        return ResponseEntity.ok(reference);
    }

    /**
     * Har qanday {@link LabeledEnum} ni frontend kutadigan {value, label} juftliklariga o'giradi.
     * Nom tarjima faylidan, joriy tilga qarab olinadi.
     */
    private <E extends Enum<E> & LabeledEnum> List<EnumOption> options(E[] values) {
        return Arrays.stream(values)
                .map(value -> new EnumOption(value.name(), translator.of(value)))
                .toList();
    }
}
