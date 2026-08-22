package api.anticorruption.complaint;

import api.anticorruption.attachment.AttachmentRepository;
import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.dto.AssignRequest;
import api.anticorruption.complaint.dto.ComplaintCreatedResponse;
import api.anticorruption.complaint.dto.ComplaintResponse;
import api.anticorruption.complaint.dto.ComplaintSearchFilter;
import api.anticorruption.complaint.dto.ComplaintSummaryResponse;
import api.anticorruption.complaint.dto.ComplaintTrackingResponse;
import api.anticorruption.complaint.dto.CreateComplaintRequest;
import api.anticorruption.complaint.dto.RegisterEntryResponse;
import api.anticorruption.complaint.dto.UpdateStatusRequest;
import api.anticorruption.notification.EmailService;
import api.anticorruption.university.Department;
import api.anticorruption.university.Faculty;
import api.anticorruption.university.UniversityService;
import api.anticorruption.user.User;
import api.anticorruption.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Murojaatlar bilan bog'liq barcha biznes-mantiq. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final UniversityService universityService;
    private final TrackingCodeGenerator trackingCodeGenerator;
    private final EmailService emailService;
    private final Translator translator;

    // ---------------------------------------------------------------- yaratish

    /**
     * Yangi murojaatni qabul qiladi.
     *
     * @param author tizimga kirgan foydalanuvchi yoki {@code null} (kirmasdan yuborilgan)
     */
    @Transactional
    public ComplaintCreatedResponse create(CreateComplaintRequest request, User author) {
        Complaint complaint = Complaint.builder()
                .trackingCode(trackingCodeGenerator.generate())
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category())
                .reporterType(request.reporterType())
                .subjectName(blankToNull(request.subjectName()))
                .accusedPosition(request.accusedPosition())
                .incidentDate(request.incidentDate())
                .incidentPlace(blankToNull(request.incidentPlace()))
                .anonymous(request.anonymous())
                .status(ComplaintStatus.NEW)
                // Xabarnomalar keyinchalik shu tilda yuboriladi.
                .locale(translator.currentLanguage().getCode())
                .build();

        applyUniversityContext(complaint, request);
        applyStudentContext(complaint, request);
        applyReporterInfo(complaint, request, author);

        // Izoh saqlanmaydi: bu tizim yozuvi, uni har safar joriy tilga o'giramiz.
        complaint.addHistoryEntry(ComplaintStatusHistory.builder()
                .newStatus(ComplaintStatus.NEW)
                .changedBy(complaint.getAuthor())
                .build());

        complaintRepository.save(complaint);
        log.info("Yangi murojaat qabul qilindi: {} (kategoriya: {})",
                complaint.getTrackingCode(), complaint.getCategory());

        emailService.sendComplaintReceived(
                complaint.notificationEmail(),
                complaint.getLocale(),
                complaint.getTrackingCode(),
                complaint.getTitle());

        return ComplaintCreatedResponse.from(complaint, translator);
    }

    /**
     * Fakultet va kafedrani bog'laydi.
     *
     * <p>Kafedra ko'rsatilib, fakultet ko'rsatilmagan bo'lsa, fakultet
     * kafedradan olinadi - shakl to'ldirishni soddalashtirish uchun.
     */
    private void applyUniversityContext(Complaint complaint, CreateComplaintRequest request) {
        Faculty faculty = request.facultyId() == null
                ? null
                : universityService.requireSelectableFaculty(request.facultyId());

        if (request.departmentId() != null) {
            Department department = universityService.requireSelectableDepartment(
                    request.departmentId(), request.facultyId());
            complaint.setDepartment(department);
            if (faculty == null) {
                faculty = department.getFaculty();
            }
        }
        complaint.setFaculty(faculty);
    }

    /** Kurs, guruh va ta'lim shakli faqat talaba va magistrantlar uchun ma'noli. */
    private void applyStudentContext(Complaint complaint, CreateComplaintRequest request) {
        if (request.reporterType() != null && request.reporterType().isStudent()) {
            complaint.setCourseYear(request.courseYear());
            complaint.setGroupName(blankToNull(request.groupName()));
            complaint.setStudyForm(request.studyForm());
        }
    }

    /**
     * Murojaatchi ma'lumotlarini to'ldiradi.
     *
     * <p>Anonim murojaatda shaxsni aniqlovchi hech narsa saqlanmaydi - hatto
     * foydalanuvchi tizimga kirgan bo'lsa ham muallif bog'lanmaydi. Faqat
     * ixtiyoriy email qoladi, u ham faqat xabarnoma yuborish uchun.
     */
    private void applyReporterInfo(Complaint complaint, CreateComplaintRequest request, User author) {
        if (request.anonymous()) {
            complaint.setReporterEmail(normalizeEmail(request.reporterEmail()));
            return;
        }
        if (author != null) {
            complaint.setAuthor(author);
            complaint.setReporterName(author.getFullName());
            complaint.setReporterEmail(author.getEmail());
            complaint.setReporterPhone(author.getPhone());
            return;
        }
        complaint.setReporterName(blankToNull(request.reporterName()));
        complaint.setReporterEmail(normalizeEmail(request.reporterEmail()));
        complaint.setReporterPhone(blankToNull(request.reporterPhone()));
    }

    // ---------------------------------------------------------------- ochiq ko'rinish

    /** Kuzatuv kodi bo'yicha holatni qaytaradi (autentifikatsiyasiz). */
    @Transactional(readOnly = true)
    public ComplaintTrackingResponse trackByCode(String trackingCode) {
        Complaint complaint = complaintRepository
                .findDetailByTrackingCode(normalizeCode(trackingCode))
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageKeys.COMPLAINT_NOT_FOUND_BY_CODE, trackingCode));
        return ComplaintTrackingResponse.from(complaint, translator);
    }

    /**
     * Ochiq reyestr - barcha murojaatlarning anonimlashtirilgan ro'yxati.
     *
     * <p>Bu portalning shaffoflik vositasi: har kim nechta murojaat kelgani va
     * ular qanday hal qilinganini ko'ra oladi, lekin mazmunini ko'rmaydi.
     * Xodim yashirgan murojaatlar chiqmaydi.
     */
    @Transactional(readOnly = true)
    public PageResponse<RegisterEntryResponse> publicRegister(String trackingCodeQuery,
                                                              ComplaintCategory category,
                                                              ComplaintStatus status,
                                                              Long facultyId,
                                                              Pageable pageable) {
        List<Specification<Complaint>> parts = new ArrayList<>();
        parts.add(ComplaintSpecifications.visibleInRegister());
        addIfPresent(parts, ComplaintSpecifications.trackingCodeContains(trackingCodeQuery));
        addIfPresent(parts, ComplaintSpecifications.categoryIs(category));
        addIfPresent(parts, ComplaintSpecifications.statusIs(status));
        addIfPresent(parts, ComplaintSpecifications.facultyIs(facultyId));

        Specification<Complaint> specification = parts.stream().reduce(Specification::and).orElseThrow();
        Page<Complaint> page = complaintRepository.findAll(specification, pageable);

        return PageResponse.from(page.map(complaint -> RegisterEntryResponse.from(complaint, translator)));
    }

    // ---------------------------------------------------------------- foydalanuvchi ro'yxati

    /** Foydalanuvchining o'z murojaatlari. */
    @Transactional(readOnly = true)
    public PageResponse<ComplaintSummaryResponse> findMine(Long authorId, Pageable pageable) {
        return toSummaryPage(complaintRepository.findByAuthorId(authorId, pageable));
    }

    /**
     * Foydalanuvchi o'z murojaatining to'liq ko'rinishini oladi.
     * Boshqa odamning murojaatiga kirishga urinsa 404 qaytadi -
     * bunday id mavjudligini oshkor qilmaslik uchun.
     */
    @Transactional(readOnly = true)
    public ComplaintResponse findMineById(Long complaintId, User author) {
        Complaint complaint = complaintRepository.findDetailById(complaintId)
                .filter(c -> c.getAuthor() != null && Objects.equals(c.getAuthor().getId(), author.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_COMPLAINT, complaintId));
        return ComplaintResponse.from(complaint, translator);
    }

    // ---------------------------------------------------------------- xodimlar uchun

    /** Filtrlangan va sahifalangan ro'yxat. */
    @Transactional(readOnly = true)
    public PageResponse<ComplaintSummaryResponse> search(ComplaintSearchFilter filter, Pageable pageable) {
        Specification<Complaint> specification = buildSpecification(filter);
        Page<Complaint> page = specification == null
                ? complaintRepository.findAll(pageable)
                : complaintRepository.findAll(specification, pageable);
        return toSummaryPage(page);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse findById(Long complaintId) {
        return ComplaintResponse.from(requireComplaint(complaintId), translator);
    }

    /**
     * Holatni o'zgartiradi va tarixga o'zgarmas yozuv qo'shadi.
     *
     * @throws BadRequestException bunday o'tish taqiqlangan bo'lsa
     */
    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, UpdateStatusRequest request, User staff) {
        Complaint complaint = requireComplaint(complaintId);

        ComplaintStatus currentStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.status();

        if (currentStatus == newStatus) {
            throw new BadRequestException(MessageKeys.COMPLAINT_ALREADY_IN_STATUS, label(currentStatus));
        }
        if (!currentStatus.allowedTransitions().contains(newStatus)) {
            throw new BadRequestException(MessageKeys.COMPLAINT_TRANSITION_NOT_ALLOWED,
                    label(currentStatus), label(newStatus));
        }

        complaint.setStatus(newStatus);
        if (blankToNull(request.officialResponse()) != null) {
            complaint.setOfficialResponse(request.officialResponse().trim());
        }
        if (newStatus.isFinal()) {
            closeComplaint(complaint);
        }
        // Ko'rib chiqishni boshlagan xodim avtomatik mas'ul bo'ladi.
        if (complaint.getAssignee() == null) {
            complaint.setAssignee(staff);
        }

        complaint.addHistoryEntry(ComplaintStatusHistory.builder()
                .oldStatus(currentStatus)
                .newStatus(newStatus)
                .note(blankToNull(request.note()))
                .changedBy(staff)
                .build());

        complaintRepository.save(complaint);
        log.info("Murojaat {} holati o'zgardi: {} -> {} (xodim id={})",
                complaint.getTrackingCode(), currentStatus, newStatus, staff.getId());

        emailService.sendStatusChanged(
                complaint.notificationEmail(),
                complaint.getLocale(),
                complaint.getTrackingCode(),
                complaint.getTitle(),
                currentStatus,
                newStatus,
                complaint.getOfficialResponse());

        return ComplaintResponse.from(complaint, translator);
    }

    /** Yopilish vaqtini va ko'rib chiqish muddatini yozib qo'yadi. */
    private void closeComplaint(Complaint complaint) {
        Instant closedAt = Instant.now();
        complaint.setClosedAt(closedAt);
        complaint.setResolutionMinutes(
                Duration.between(complaint.getCreatedAt(), closedAt).toMinutes());
    }

    /** Murojaatni xodimga biriktiradi yoki biriktiruvni bekor qiladi. */
    @Transactional
    public ComplaintResponse assign(Long complaintId, AssignRequest request, User actor) {
        Complaint complaint = requireComplaint(complaintId);

        if (request.assigneeId() == null) {
            complaint.setAssignee(null);
        } else {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            MessageKeys.NOT_FOUND_STAFF_USER, request.assigneeId()));
            if (!assignee.isStaff()) {
                throw new BadRequestException(MessageKeys.COMPLAINT_ASSIGNEE_MUST_BE_STAFF);
            }
            if (!assignee.isEnabled()) {
                throw new BadRequestException(MessageKeys.COMPLAINT_ASSIGNEE_DISABLED);
            }
            complaint.setAssignee(assignee);
        }

        complaintRepository.save(complaint);
        log.info("Murojaat {} biriktiruvi o'zgardi (kim tomonidan: id={})",
                complaint.getTrackingCode(), actor.getId());

        return ComplaintResponse.from(complaint, translator);
    }

    /**
     * Murojaatni ochiq reyestrdan yashiradi yoki qaytaradi.
     * Tafsilotlar murojaatchini bilvosita oshkor qilishi mumkin bo'lgan
     * hollarda ishlatiladi (masalan bitta guruhdan bitta murojaat).
     */
    @Transactional
    public ComplaintResponse setRegisterVisibility(Long complaintId, boolean hidden, User staff) {
        Complaint complaint = requireComplaint(complaintId);

        if (complaint.isHiddenFromRegister() == hidden) {
            throw new BadRequestException(hidden
                    ? MessageKeys.COMPLAINT_ALREADY_HIDDEN
                    : MessageKeys.COMPLAINT_ALREADY_VISIBLE);
        }

        complaint.setHiddenFromRegister(hidden);
        complaintRepository.save(complaint);
        log.info("Murojaat {} reyestrda ko'rinishi o'zgardi: hidden={} (xodim id={})",
                complaint.getTrackingCode(), hidden, staff.getId());

        return ComplaintResponse.from(complaint, translator);
    }

    // ---------------------------------------------------------------- yordamchilar

    private Complaint requireComplaint(Long complaintId) {
        return complaintRepository.findDetailById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_COMPLAINT, complaintId));
    }

    /**
     * Enum nomini xabar argumenti sifatida o'raydi.
     *
     * <p>Tayyor matn emas, {@link DefaultMessageSourceResolvable} qaytariladi:
     * shunda holat nomi ham xabarning o'zi bilan bir vaqtda, bir xil tilda
     * hosil qilinadi. Bu yerda tarjima qilib qo'yilsa, xabar bir tilda,
     * argument boshqa tilda chiqib qolishi mumkin edi.
     */
    private Object label(ComplaintStatus status) {
        return new DefaultMessageSourceResolvable(new String[]{status.messageKey()}, status.name());
    }

    private Specification<Complaint> buildSpecification(ComplaintSearchFilter filter) {
        if (filter == null) {
            return null;
        }
        List<Specification<Complaint>> parts = new ArrayList<>();
        addIfPresent(parts, ComplaintSpecifications.textContains(filter.query()));
        addIfPresent(parts, ComplaintSpecifications.statusIs(filter.status()));
        addIfPresent(parts, ComplaintSpecifications.categoryIs(filter.category()));
        addIfPresent(parts, ComplaintSpecifications.facultyIs(filter.facultyId()));
        addIfPresent(parts, ComplaintSpecifications.departmentIs(filter.departmentId()));
        addIfPresent(parts, ComplaintSpecifications.reporterTypeIs(filter.reporterType()));
        addIfPresent(parts, ComplaintSpecifications.accusedPositionIs(filter.accusedPosition()));
        addIfPresent(parts, ComplaintSpecifications.assigneeIs(filter.assigneeId()));
        addIfPresent(parts, ComplaintSpecifications.unassigned(filter.unassigned()));
        addIfPresent(parts, ComplaintSpecifications.createdAfter(filter.from()));
        addIfPresent(parts, ComplaintSpecifications.createdBefore(filter.to()));

        return parts.stream().reduce(Specification::and).orElse(null);
    }

    private void addIfPresent(List<Specification<Complaint>> parts, Specification<Complaint> specification) {
        if (specification != null) {
            parts.add(specification);
        }
    }

    /** Sahifadagi barcha murojaatlar uchun fayllar sonini bitta so'rovda oladi. */
    private PageResponse<ComplaintSummaryResponse> toSummaryPage(Page<Complaint> page) {
        List<Long> ids = page.getContent().stream().map(Complaint::getId).toList();
        Map<Long, Long> attachmentCounts = countAttachments(ids);

        return PageResponse.from(page.map(complaint ->
                ComplaintSummaryResponse.from(
                        complaint,
                        attachmentCounts.getOrDefault(complaint.getId(), 0L),
                        translator)));
    }

    private Map<Long, Long> countAttachments(List<Long> complaintIds) {
        if (complaintIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : attachmentRepository.countGroupedByComplaintIds(complaintIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    /** Foydalanuvchi kodni kichik harfda yoki bo'shliq bilan kiritishi mumkin. */
    private String normalizeCode(String trackingCode) {
        return trackingCode == null ? null : trackingCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        String value = blankToNull(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
