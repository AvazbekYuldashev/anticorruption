package api.anticorruption.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Bosh banner fonining rasmlari. */
public interface HomeBannerImageRepository extends JpaRepository<HomeBannerImage, Long> {

    List<HomeBannerImage> findAllByOrderByDisplayOrderAscIdAsc();
}
