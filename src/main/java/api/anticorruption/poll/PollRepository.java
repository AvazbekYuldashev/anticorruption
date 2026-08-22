package api.anticorruption.poll;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = "options")
    List<Poll> findByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "options")
    List<Poll> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "options")
    Optional<Poll> findWithOptionsById(Long id);
}
