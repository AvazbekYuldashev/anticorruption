package api.anticorruption.university;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Faculty> findByCodeIgnoreCase(String code);

    /** Ochiq ro'yxat uchun: faqat faol fakultetlar, kafedralari bilan birga. */
    @EntityGraph(attributePaths = "departments")
    List<Faculty> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "departments")
    List<Faculty> findAllByOrderByNameAsc();
}
