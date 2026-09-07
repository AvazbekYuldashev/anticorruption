package api.anticorruption.university;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByFacultyIdAndCodeIgnoreCase(Long facultyId, String code);

    List<Department> findByFacultyIdAndActiveTrueOrderByNameAsc(Long facultyId);

    List<Department> findByFacultyIdOrderByNameAsc(Long facultyId);
}
