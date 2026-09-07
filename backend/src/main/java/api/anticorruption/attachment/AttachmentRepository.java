package api.anticorruption.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByComplaintId(Long complaintId);

    long countByComplaintId(Long complaintId);

    /** Yuklab olishda huquqni tekshirish uchun murojaat va uning muallifi ham kerak. */
    @Query("select a from Attachment a join fetch a.complaint c left join fetch c.author where a.id = :id")
    Optional<Attachment> findDetailById(@Param("id") Long id);

    /**
     * Bir sahifadagi barcha murojaatlar uchun fayllar sonini bitta so'rovda hisoblaydi.
     * Natija: [murojaat_id, soni] juftliklari.
     */
    @Query("select a.complaint.id, count(a) from Attachment a where a.complaint.id in :ids group by a.complaint.id")
    List<Object[]> countGroupedByComplaintIds(@Param("ids") Collection<Long> ids);
}
