package api.anticorruption.content;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AboutSectionRepository extends JpaRepository<AboutSection, Long> {

    /**
     * Sahifaning yagona yozuvi.
     *
     * <p>Jadvalda bittadan ortiq yozuv bo'lmasligi kerak, shuning uchun
     * eng birinchisi olinadi: tasodifan ikkinchisi paydo bo'lsa ham sayt
     * barqaror ishlaydi.
     */
    @EntityGraph(attributePaths = "tasks")
    Optional<AboutSection> findFirstByOrderByIdAsc();
}
