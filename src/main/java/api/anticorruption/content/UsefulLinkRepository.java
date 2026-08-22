package api.anticorruption.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsefulLinkRepository extends JpaRepository<UsefulLink, Long> {

    List<UsefulLink> findByActiveTrueOrderByDisplayOrderAscTitleAsc();

    List<UsefulLink> findAllByOrderByDisplayOrderAscTitleAsc();
}
