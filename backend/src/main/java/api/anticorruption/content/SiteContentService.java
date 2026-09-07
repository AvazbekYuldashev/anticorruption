package api.anticorruption.content;

import api.anticorruption.attachment.FileStorageService;
import api.anticorruption.attachment.StorageArea;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.content.dto.AboutSectionResponse;
import api.anticorruption.content.dto.SaveAboutSectionRequest;
import api.anticorruption.content.dto.SaveStaffMemberRequest;
import api.anticorruption.content.dto.StaffMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Saytning o'zgarmas bo'limlari: xodimlar ro'yxati va "Bo'lim haqida"
 * sahifasi. Admin kiritadi, tashrifchi o'qiydi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteContentService {

    private static final int DEFAULT_ORDER = 100;

    private final StaffMemberRepository staffMemberRepository;
    private final AboutSectionRepository aboutSectionRepository;
    private final FileStorageService fileStorageService;

    // ================================================================ xodimlar

    @Transactional(readOnly = true)
    public List<StaffMemberResponse> listStaff(boolean includeInactive) {
        List<StaffMember> members = includeInactive
                ? staffMemberRepository.findAllByOrderByDisplayOrderAscFullNameAsc()
                : staffMemberRepository.findByActiveTrueOrderByDisplayOrderAscFullNameAsc();
        return members.stream().map(StaffMemberResponse::from).toList();
    }

    @Transactional
    public StaffMemberResponse createStaff(SaveStaffMemberRequest request) {
        StaffMember member = StaffMember.builder()
                .fullName(request.fullName().trim())
                .position(request.position().trim())
                .academicDegree(blankToNull(request.academicDegree()))
                .biography(blankToNull(request.biography()))
                .phone(blankToNull(request.phone()))
                .email(blankToNull(request.email()))
                .receptionHours(blankToNull(request.receptionHours()))
                .displayOrder(request.displayOrder() == null ? DEFAULT_ORDER : request.displayOrder())
                .active(request.active() == null || request.active())
                .build();

        staffMemberRepository.save(member);
        return StaffMemberResponse.from(member);
    }

    @Transactional
    public StaffMemberResponse updateStaff(Long memberId, SaveStaffMemberRequest request) {
        StaffMember member = requireStaff(memberId);

        member.setFullName(request.fullName().trim());
        member.setPosition(request.position().trim());
        member.setAcademicDegree(blankToNull(request.academicDegree()));
        member.setBiography(blankToNull(request.biography()));
        member.setPhone(blankToNull(request.phone()));
        member.setEmail(blankToNull(request.email()));
        member.setReceptionHours(blankToNull(request.receptionHours()));
        if (request.displayOrder() != null) {
            member.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            member.setActive(request.active());
        }

        staffMemberRepository.save(member);
        return StaffMemberResponse.from(member);
    }

    /** Suratni almashtiradi. Eskisi diskdan o'chiriladi. */
    @Transactional
    public StaffMemberResponse replaceStaffPhoto(Long memberId, MultipartFile image) {
        StaffMember member = requireStaff(memberId);
        String previous = member.getPhoto();

        member.setPhoto(fileStorageService.store(image, StorageArea.PUBLIC));
        staffMemberRepository.save(member);

        fileStorageService.delete(previous, StorageArea.PUBLIC);
        return StaffMemberResponse.from(member);
    }

    @Transactional
    public void deleteStaff(Long memberId) {
        StaffMember member = requireStaff(memberId);
        String photo = member.getPhoto();

        staffMemberRepository.delete(member);
        fileStorageService.delete(photo, StorageArea.PUBLIC);
    }

    // ========================================================== bo'lim haqida

    /** Sahifa hali to'ldirilmagan bo'lsa bo'sh mazmun qaytadi - 404 emas. */
    @Transactional(readOnly = true)
    public AboutSectionResponse about() {
        return aboutSectionRepository.findFirstByOrderByIdAsc()
                .map(AboutSectionResponse::from)
                .orElseGet(AboutSectionResponse::empty);
    }

    /**
     * Sahifani saqlaydi.
     *
     * <p>Yozuv bitta: birinchi saqlashda yaratiladi, keyingilarida
     * o'sha yozuv yangilanadi.
     */
    @Transactional
    public AboutSectionResponse saveAbout(SaveAboutSectionRequest request) {
        AboutSection about = aboutSectionRepository.findFirstByOrderByIdAsc()
                .orElseGet(AboutSection::new);

        about.setTitle(blankToNull(request.title()));
        about.setBody(blankToNull(request.body()));
        about.setTasksTitle(blankToNull(request.tasksTitle()));
        about.setGoal(blankToNull(request.goal()));

        // Bo'sh bandlar saqlanmaydi: muharrirda qolib ketgan bo'sh qator
        // saytda bo'sh nuqta bo'lib chiqmasin.
        List<String> tasks = request.tasks() == null ? List.of() : request.tasks().stream()
                .filter(task -> task != null && !task.isBlank())
                .map(String::trim)
                .toList();

        about.getTasks().clear();
        about.getTasks().addAll(tasks);

        aboutSectionRepository.save(about);
        return AboutSectionResponse.from(about);
    }

    // ================================================================ yordamchilar

    private StaffMember requireStaff(Long memberId) {
        return staffMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_STAFF_MEMBER, memberId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
