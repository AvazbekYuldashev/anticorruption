package api.anticorruption.poll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** So'rovnoma va test guruhlari. */
public interface PollGroupRepository extends JpaRepository<PollGroup, Long> {

    List<PollGroup> findByTypeOrderByDisplayOrderAscNameAsc(PollType type);

    List<PollGroup> findAllByOrderByDisplayOrderAscNameAsc();

    /**
     * Bir turdagi guruhlar orasida shu nom bandmi.
     *
     * <p>Registr hisobga olinmaydi: "2026 anketalar" va "2026 Anketalar" bir
     * xil guruh deb qaraladi, aks holda ro'yxatda ikkita bir xil sarlavha
     * paydo bo'lardi.
     */
    @Query("""
            select count(g) > 0 from PollGroup g
            where g.type = :type
              and lower(g.name) = lower(:name)
              and (:excludeId is null or g.id <> :excludeId)
            """)
    boolean nameTaken(
            @Param("type") PollType type,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    /** Guruhdagi so'rovnomalar soni - ro'yxatda ko'rsatish uchun. */
    @Query("select p.group.id, count(p) from Poll p group by p.group.id")
    List<Object[]> countByGroup();

    /** Bitta guruhdagi so'rovnomalar soni - o'chirishdan oldin tekshiriladi. */
    @Query("select count(p) from Poll p where p.group.id = :groupId")
    long countPolls(@Param("groupId") Long groupId);
}
