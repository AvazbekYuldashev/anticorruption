package api.anticorruption.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaticPageRepository extends JpaRepository<StaticPage, Long> {

    Optional<StaticPage> findBySlug(String slug);

    Optional<StaticPage> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    List<StaticPage> findByPublishedTrueOrderByDisplayOrderAscTitleAsc();

    List<StaticPage> findAllByOrderByDisplayOrderAscTitleAsc();
}
