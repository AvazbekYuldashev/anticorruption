package api.anticorruption.university;

import api.anticorruption.university.dto.DepartmentResponse;
import api.anticorruption.university.dto.FacultyResponse;
import api.anticorruption.university.dto.SaveDepartmentRequest;
import api.anticorruption.university.dto.SaveFacultyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Universitet tuzilmasini boshqarish. Faqat ADMIN uchun.
 *
 * <p>Murojaat shakli aynan shu ma'lumotdan to'ldiriladi, shuning uchun
 * tizimni ishga tushirgandan keyin birinchi qadam - fakultetlarni kiritish.
 */
@Tag(name = "Admin - universitet tuzilmasi", description = "Fakultet va kafedralarni boshqarish")
@RestController
@RequestMapping("/api/v1/admin/faculties")
@RequiredArgsConstructor
public class AdminUniversityController {

    private final UniversityService universityService;

    @Operation(summary = "Barcha fakultetlar", description = "Nofaollari ham ko'rsatiladi")
    @GetMapping
    public ResponseEntity<List<FacultyResponse>> listFaculties() {
        return ResponseEntity.ok(universityService.listAllFaculties());
    }

    @Operation(summary = "Bitta fakultet va uning kafedralari")
    @GetMapping("/{facultyId}")
    public ResponseEntity<FacultyResponse> facultyDetail(@PathVariable Long facultyId) {
        return ResponseEntity.ok(universityService.findFaculty(facultyId));
    }

    @Operation(summary = "Yangi fakultet qo'shish")
    @PostMapping
    public ResponseEntity<FacultyResponse> createFaculty(@Valid @RequestBody SaveFacultyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(universityService.createFaculty(request));
    }

    @Operation(summary = "Fakultetni tahrirlash", description = "active=false qilinsa yangi murojaatlarda tanlanmaydi")
    @PutMapping("/{facultyId}")
    public ResponseEntity<FacultyResponse> updateFaculty(
            @PathVariable Long facultyId,
            @Valid @RequestBody SaveFacultyRequest request) {

        return ResponseEntity.ok(universityService.updateFaculty(facultyId, request));
    }

    @Operation(
            summary = "Fakultetni o'chirish",
            description = "Unga bog'langan murojaat bo'lsa o'chirilmaydi - 409 qaytadi")
    @DeleteMapping("/{facultyId}")
    public ResponseEntity<Void> deleteFaculty(@PathVariable Long facultyId) {
        universityService.deleteFaculty(facultyId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------- kafedralar

    @Operation(summary = "Fakultet kafedralari", description = "Nofaollari ham ko'rsatiladi")
    @GetMapping("/{facultyId}/departments")
    public ResponseEntity<List<DepartmentResponse>> listDepartments(@PathVariable Long facultyId) {
        return ResponseEntity.ok(universityService.listDepartments(facultyId, true));
    }

    @Operation(summary = "Yangi kafedra qo'shish")
    @PostMapping("/{facultyId}/departments")
    public ResponseEntity<DepartmentResponse> createDepartment(
            @PathVariable Long facultyId,
            @Valid @RequestBody SaveDepartmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(universityService.createDepartment(facultyId, request));
    }

    @Operation(summary = "Kafedrani tahrirlash")
    @PutMapping("/departments/{departmentId}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody SaveDepartmentRequest request) {

        return ResponseEntity.ok(universityService.updateDepartment(departmentId, request));
    }

    @Operation(summary = "Kafedrani o'chirish", description = "Unga bog'langan murojaat bo'lsa 409 qaytadi")
    @DeleteMapping("/departments/{departmentId}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long departmentId) {
        universityService.deleteDepartment(departmentId);
        return ResponseEntity.noContent().build();
    }
}
