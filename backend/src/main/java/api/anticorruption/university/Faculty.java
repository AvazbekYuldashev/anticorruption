package api.anticorruption.university;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Universitet fakulteti.
 *
 * <p>Enum emas, jadval: fakultetlar universitetdan universitetga farq qiladi
 * va vaqt o'tishi bilan o'zgaradi (qo'shiladi, birlashtiriladi, yopiladi).
 * Administrator ularni API orqali boshqaradi.
 *
 * <p>Fakultet o'chirilmaydi, {@code active=false} qilinadi - eski murojaatlar
 * qaysi fakultetga tegishli ekanligini yo'qotmaslik uchun.
 */
@Entity
@Table(
        name = "faculties",
        indexes = @Index(name = "idx_faculties_code", columnList = "code", unique = true)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /** Qisqa belgi, masalan "KIF" yoki "IQT". Ro'yxatlarda va hisobotlarda qulay. */
    @Column(nullable = false, length = 20)
    private String code;

    /** false bo'lsa yangi murojaatlarda tanlanmaydi, lekin eskilarida ko'rinadi. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    @Builder.Default
    private List<Department> departments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addDepartment(Department department) {
        departments.add(department);
        department.setFaculty(this);
    }
}
