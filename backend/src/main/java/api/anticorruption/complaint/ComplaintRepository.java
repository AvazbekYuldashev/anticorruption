package api.anticorruption.complaint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    /**
     * Foydalanuvchi biror murojaatga bog'langanmi.
     *
     * <p>Hisobni o'chirishdan oldin tekshiriladi: muallif yoki mas'ul xodim
     * yo'qolsa murojaatlar tarixi buziladi.
     */
    boolean existsByAuthorIdOrAssigneeId(Long authorId, Long assigneeId);

    Optional<Complaint> findByTrackingCode(String trackingCode);

    boolean existsByTrackingCode(String trackingCode);

    /**
     * Ro'yxatda har bir qator uchun alohida so'rov ketmasligi uchun ToOne
     * bog'lanishlar bitta so'rovda yuklanadi. To'plamlar bu yerda yuklanmaydi -
     * sahifalash buzilmasligi uchun.
     */
    @Override
    @EntityGraph(attributePaths = {"author", "assignee", "faculty", "department"})
    Page<Complaint> findAll(Specification<Complaint> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"assignee", "faculty", "department"})
    Page<Complaint> findByAuthorId(Long authorId, Pageable pageable);

    long countByStatus(ComplaintStatus status);

    long countByCreatedAtAfter(Instant since);

    /** Tuzilma birligini o'chirish mumkinligini tekshirish uchun. */
    long countByFacultyId(Long facultyId);

    long countByDepartmentId(Long departmentId);

    /**
     * Tafsilot sahifasi uchun. To'plamlar (attachments, history) bu yerda
     * fetch qilinmaydi - ikkita List ni bir vaqtda fetch qilish mumkin emas.
     */
    @Query("select c from Complaint c "
            + "left join fetch c.author left join fetch c.assignee "
            + "left join fetch c.faculty left join fetch c.department "
            + "where c.id = :id")
    Optional<Complaint> findDetailById(@Param("id") Long id);

    @Query("select c from Complaint c "
            + "left join fetch c.author left join fetch c.assignee "
            + "left join fetch c.faculty left join fetch c.department "
            + "where c.trackingCode = :trackingCode")
    Optional<Complaint> findDetailByTrackingCode(@Param("trackingCode") String trackingCode);

    // ---------------------------------------------------------------- statistika

    @Query("select c.status as status, count(c) as total from Complaint c group by c.status")
    List<StatusCount> countGroupedByStatus();

    @Query("select c.category as category, count(c) as total from Complaint c group by c.category")
    List<CategoryCount> countGroupedByCategory();

    @Query("select c.reporterType as reporterType, count(c) as total from Complaint c group by c.reporterType")
    List<ReporterTypeCount> countGroupedByReporterType();

    /** Fakulteti ko'rsatilmagan murojaatlar ham chiqadi - ularda id va nom null bo'ladi. */
    @Query("select f.id as facultyId, f.name as facultyName, count(c) as total "
            + "from Complaint c left join c.faculty f group by f.id, f.name order by count(c) desc")
    List<FacultyCount> countGroupedByFaculty();

    /**
     * O'rtacha ko'rib chiqish muddati (daqiqalarda).
     *
     * <p>Muddat murojaat yopilganda alohida ustunga yozib qo'yiladi, chunki
     * ikki vaqt orasidagi farqni SQL da hisoblash bazaga xos funksiya talab
     * qiladi va ko'chma bo'lmaydi.
     */
    @Query("select avg(c.resolutionMinutes) from Complaint c where c.resolutionMinutes is not null")
    Double averageResolutionMinutes();

    /**
     * Fakultetlar kesimidagi ko'rsatkichlar: jami, hal qilingan, yopilgan va
     * o'rtacha muddat. Fakulteti ko'rsatilmagan murojaatlar bu yerga tushmaydi.
     */
    @Query("select f.id as facultyId, f.name as facultyName, "
            + "count(c) as total, "
            + "sum(case when c.status = :resolvedStatus then 1 else 0 end) as resolved, "
            + "sum(case when c.closedAt is not null then 1 else 0 end) as closed, "
            + "avg(c.resolutionMinutes) as averageMinutes "
            + "from Complaint c join c.faculty f "
            + "group by f.id, f.name "
            + "order by count(c) desc")
    List<FacultyRatingRow> facultyRating(@Param("resolvedStatus") ComplaintStatus resolvedStatus);

    /** Guruhlangan hisob-kitob natijalari uchun proyeksiyalar. */
    interface FacultyRatingRow {
        Long getFacultyId();

        String getFacultyName();

        long getTotal();

        long getResolved();

        long getClosed();

        /** Hali birorta murojaat yopilmagan bo'lsa null. */
        Double getAverageMinutes();
    }

    interface StatusCount {
        ComplaintStatus getStatus();

        long getTotal();
    }

    interface CategoryCount {
        ComplaintCategory getCategory();

        long getTotal();
    }

    interface ReporterTypeCount {
        ReporterType getReporterType();

        long getTotal();
    }

    interface FacultyCount {
        Long getFacultyId();

        String getFacultyName();

        long getTotal();
    }
}
