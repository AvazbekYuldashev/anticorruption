package api.anticorruption.university;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.complaint.ComplaintRepository;
import api.anticorruption.university.dto.DepartmentResponse;
import api.anticorruption.university.dto.FacultyResponse;
import api.anticorruption.university.dto.SaveDepartmentRequest;
import api.anticorruption.university.dto.SaveFacultyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Universitet tuzilmasi: fakultetlar va kafedralar.
 *
 * <p>O'chirish qoidasi: tuzilma birligiga bog'langan murojaat bo'lsa, uni
 * o'chirib bo'lmaydi - faqat nofaol qilinadi. Aks holda eski murojaatlar
 * qaysi bo'limga tegishli ekanligini yo'qotardi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniversityService {

    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;

    // ---------------------------------------------------------------- o'qish

    /** Murojaat shakli uchun: faqat faol fakultetlar va faol kafedralar. */
    @Transactional(readOnly = true)
    public List<FacultyResponse> listActiveFaculties() {
        return facultyRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(FacultyResponse::activeOnly)
                .toList();
    }

    /** Admin paneli uchun: nofaollari ham. */
    @Transactional(readOnly = true)
    public List<FacultyResponse> listAllFaculties() {
        return facultyRepository.findAllByOrderByNameAsc().stream()
                .map(FacultyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacultyResponse findFaculty(Long facultyId) {
        return FacultyResponse.from(requireFaculty(facultyId));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments(Long facultyId, boolean includeInactive) {
        requireFaculty(facultyId);
        List<Department> departments = includeInactive
                ? departmentRepository.findByFacultyIdOrderByNameAsc(facultyId)
                : departmentRepository.findByFacultyIdAndActiveTrueOrderByNameAsc(facultyId);
        return departments.stream().map(DepartmentResponse::from).toList();
    }

    // ---------------------------------------------------------------- fakultet

    @Transactional
    public FacultyResponse createFaculty(SaveFacultyRequest request) {
        String code = normalizeCode(request.code());
        if (facultyRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(MessageKeys.FACULTY_CODE_TAKEN, code);
        }

        Faculty faculty = Faculty.builder()
                .name(request.name().trim())
                .code(code)
                .active(request.active() == null || request.active())
                .build();

        facultyRepository.save(faculty);
        log.info("Fakultet yaratildi: {} ({})", faculty.getName(), faculty.getCode());

        return FacultyResponse.from(faculty);
    }

    @Transactional
    public FacultyResponse updateFaculty(Long facultyId, SaveFacultyRequest request) {
        Faculty faculty = requireFaculty(facultyId);
        String code = normalizeCode(request.code());

        if (!faculty.getCode().equalsIgnoreCase(code) && facultyRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(MessageKeys.FACULTY_CODE_TAKEN, code);
        }

        faculty.setName(request.name().trim());
        faculty.setCode(code);
        if (request.active() != null) {
            faculty.setActive(request.active());
        }

        facultyRepository.save(faculty);
        return FacultyResponse.from(faculty);
    }

    /**
     * Fakultetni butunlay o'chiradi. Unga bog'langan murojaat bo'lsa
     * o'chirmaydi - nofaol qilishni taklif etadi.
     */
    @Transactional
    public void deleteFaculty(Long facultyId) {
        Faculty faculty = requireFaculty(facultyId);

        long usage = complaintRepository.countByFacultyId(facultyId);
        if (usage > 0) {
            throw new ConflictException(MessageKeys.FACULTY_IN_USE, usage);
        }

        facultyRepository.delete(faculty);
        log.info("Fakultet o'chirildi: id={}", facultyId);
    }

    // ---------------------------------------------------------------- kafedra

    @Transactional
    public DepartmentResponse createDepartment(Long facultyId, SaveDepartmentRequest request) {
        Faculty faculty = requireFaculty(facultyId);
        String code = normalizeCode(request.code());

        if (departmentRepository.existsByFacultyIdAndCodeIgnoreCase(facultyId, code)) {
            throw new ConflictException(MessageKeys.DEPARTMENT_CODE_TAKEN, code);
        }

        Department department = Department.builder()
                .faculty(faculty)
                .name(request.name().trim())
                .code(code)
                .active(request.active() == null || request.active())
                .build();

        departmentRepository.save(department);
        log.info("Kafedra yaratildi: {} ({}), fakultet id={}",
                department.getName(), department.getCode(), facultyId);

        return DepartmentResponse.from(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, SaveDepartmentRequest request) {
        Department department = requireDepartment(departmentId);
        String code = normalizeCode(request.code());
        Long facultyId = department.getFaculty().getId();

        if (!department.getCode().equalsIgnoreCase(code)
                && departmentRepository.existsByFacultyIdAndCodeIgnoreCase(facultyId, code)) {
            throw new ConflictException(MessageKeys.DEPARTMENT_CODE_TAKEN, code);
        }

        department.setName(request.name().trim());
        department.setCode(code);
        if (request.active() != null) {
            department.setActive(request.active());
        }

        departmentRepository.save(department);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = requireDepartment(departmentId);

        long usage = complaintRepository.countByDepartmentId(departmentId);
        if (usage > 0) {
            throw new ConflictException(MessageKeys.DEPARTMENT_IN_USE, usage);
        }

        departmentRepository.delete(department);
        log.info("Kafedra o'chirildi: id={}", departmentId);
    }

    // ---------------------------------------------------------------- boshqa xizmatlar uchun

    /** Murojaat yaratishda ishlatiladi: fakultet mavjud va faol ekanligini tekshiradi. */
    @Transactional(readOnly = true)
    public Faculty requireSelectableFaculty(Long facultyId) {
        Faculty faculty = requireFaculty(facultyId);
        if (!faculty.isActive()) {
            throw new BadRequestException(MessageKeys.FACULTY_INACTIVE, faculty.getName());
        }
        return faculty;
    }

    /**
     * Kafedra mavjud, faol va ko'rsatilgan fakultetga tegishli ekanligini tekshiradi.
     *
     * @param facultyId murojaatda tanlangan fakultet; null bo'lsa tekshirilmaydi
     */
    @Transactional(readOnly = true)
    public Department requireSelectableDepartment(Long departmentId, Long facultyId) {
        Department department = requireDepartment(departmentId);
        if (!department.isActive()) {
            throw new BadRequestException(MessageKeys.DEPARTMENT_INACTIVE, department.getName());
        }
        if (facultyId != null && !department.getFaculty().getId().equals(facultyId)) {
            throw new BadRequestException(MessageKeys.DEPARTMENT_FACULTY_MISMATCH);
        }
        return department;
    }

    public Faculty requireFaculty(Long facultyId) {
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_FACULTY, facultyId));
    }

    public Department requireDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_DEPARTMENT, departmentId));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
