package api.anticorruption.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Administrator o'zgartirgan sayt matnlari. */
public interface SiteTextRepository extends JpaRepository<SiteText, Long> {

    List<SiteText> findAllByOrderByLanguageCodeAscTextKeyAsc();
}
