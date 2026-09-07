package api.anticorruption.content;

import api.anticorruption.attachment.FileStorageService;
import api.anticorruption.attachment.StorageArea;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.content.dto.AboutSectionResponse;
import api.anticorruption.content.dto.AboutTranslationPayload;
import api.anticorruption.content.dto.SaveAboutSectionRequest;
import api.anticorruption.content.dto.SaveStaffMemberRequest;
import api.anticorruption.content.dto.StaffMemberResponse;
import api.anticorruption.content.dto.StaffTranslationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    private final Translator translator;

    // ================================================================ xodimlar

    /** Sayt uchun: faol xodimlar, so'rov tilidagi matn bilan. */
    @Transactional(readOnly = true)
    public List<StaffMemberResponse> listStaff() {
        AppLanguage language = translator.currentLanguage();
        return staffMemberRepository.findByActiveTrueOrderByDisplayOrderAscFullNameAsc().stream()
                .map(member -> StaffMemberResponse.localized(member, language))
                .toList();
    }

    /** Admin uchun: bloklanganlari ham, barcha tarjimalari bilan. */
    @Transactional(readOnly = true)
    public List<StaffMemberResponse> listStaffForAdmin() {
        return staffMemberRepository.findAllByOrderByDisplayOrderAscFullNameAsc().stream()
                .map(StaffMemberResponse::forAdmin)
                .toList();
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

        applyStaffTranslations(member, request.translations());
        staffMemberRepository.save(member);
        return StaffMemberResponse.forAdmin(member);
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

        applyStaffTranslations(member, request.translations());
        staffMemberRepository.save(member);
        return StaffMemberResponse.forAdmin(member);
    }

    /** Suratni almashtiradi. Eskisi diskdan o'chiriladi. */
    @Transactional
    public StaffMemberResponse replaceStaffPhoto(Long memberId, MultipartFile image) {
        StaffMember member = requireStaff(memberId);
        String previous = member.getPhoto();

        member.setPhoto(fileStorageService.store(image, StorageArea.PUBLIC));
        staffMemberRepository.save(member);

        fileStorageService.delete(previous, StorageArea.PUBLIC);
        return StaffMemberResponse.forAdmin(member);
    }

    @Transactional
    public void deleteStaff(Long memberId) {
        StaffMember member = requireStaff(memberId);
        String photo = member.getPhoto();

        staffMemberRepository.delete(member);
        fileStorageService.delete(photo, StorageArea.PUBLIC);
    }

    // ========================================================== bo'lim haqida

    /** Sayt uchun. Sahifa hali to'ldirilmagan bo'lsa bo'sh mazmun qaytadi - 404 emas. */
    @Transactional(readOnly = true)
    public AboutSectionResponse about() {
        AppLanguage language = translator.currentLanguage();
        return aboutSectionRepository.findFirstByOrderByIdAsc()
                .map(about -> AboutSectionResponse.localized(about, language))
                .orElseGet(AboutSectionResponse::empty);
    }

    /** Admin uchun: asosiy matn va barcha tarjimalar birga. */
    @Transactional(readOnly = true)
    public AboutSectionResponse aboutForAdmin() {
        return aboutSectionRepository.findFirstByOrderByIdAsc()
                .map(AboutSectionResponse::forAdmin)
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

        applyAboutTranslations(about, request.translations());

        aboutSectionRepository.save(about);
        return AboutSectionResponse.forAdmin(about);
    }

    // ================================================================ tarjimalar

    /*
     * Tarjimalar joyida yangilanadi, o'chirilib qaytadan yozilmaydi.
     * Sabab: (yozuv, til) juftligi noyob bo'lgani uchun Hibernate
     * o'chirishdan oldin qo'shib yuborsa, unikal indeks buzilardi.
     * Ro'yxatdan tushgan tillar oxirida olib tashlanadi.
     */
    private void applyStaffTranslations(StaffMember member, List<StaffTranslationPayload> requested) {
        Set<AppLanguage> keep = EnumSet.noneOf(AppLanguage.class);

        if (requested != null) {
            for (StaffTranslationPayload item : requested) {
                AppLanguage language = AppLanguage.from(item.languageCode());

                // Asosiy til tarjima emas - u yozuvning o'zida turadi.
                if (language == AppLanguage.DEFAULT || isEmpty(item) || !keep.add(language)) {
                    continue;
                }

                StaffMemberTranslation translation = member.getTranslations().stream()
                        .filter(existing -> existing.getLanguage() == language)
                        .findFirst()
                        .orElseGet(() -> {
                            StaffMemberTranslation created = StaffMemberTranslation.builder()
                                    .member(member)
                                    .language(language)
                                    .build();
                            member.getTranslations().add(created);
                            return created;
                        });

                translation.setFullName(blankToNull(item.fullName()));
                translation.setPosition(blankToNull(item.position()));
                translation.setAcademicDegree(blankToNull(item.academicDegree()));
                translation.setBiography(blankToNull(item.biography()));
                translation.setReceptionHours(blankToNull(item.receptionHours()));
            }
        }

        member.getTranslations().removeIf(translation -> !keep.contains(translation.getLanguage()));
    }

    private void applyAboutTranslations(AboutSection about, List<AboutTranslationPayload> requested) {
        Set<AppLanguage> keep = EnumSet.noneOf(AppLanguage.class);

        if (requested != null) {
            for (AboutTranslationPayload item : requested) {
                AppLanguage language = AppLanguage.from(item.languageCode());

                if (language == AppLanguage.DEFAULT || isEmpty(item) || !keep.add(language)) {
                    continue;
                }

                AboutSectionTranslation translation = about.getTranslations().stream()
                        .filter(existing -> existing.getLanguage() == language)
                        .findFirst()
                        .orElseGet(() -> {
                            AboutSectionTranslation created = AboutSectionTranslation.builder()
                                    .about(about)
                                    .language(language)
                                    .build();
                            about.getTranslations().add(created);
                            return created;
                        });

                translation.setTitle(blankToNull(item.title()));
                translation.setBody(blankToNull(item.body()));
                translation.setTasksTitle(blankToNull(item.tasksTitle()));
                translation.setGoal(blankToNull(item.goal()));
                translation.getTasks().clear();
                translation.getTasks().addAll(cleanTasks(item.tasks()));
            }
        }

        about.getTranslations().removeIf(translation -> !keep.contains(translation.getLanguage()));
    }

    /** Bo'sh tarjima saqlanmaydi: muharrir varaqni ochib yopgani yozuv yaratmasin. */
    private boolean isEmpty(StaffTranslationPayload item) {
        return blankToNull(item.fullName()) == null
                && blankToNull(item.position()) == null
                && blankToNull(item.academicDegree()) == null
                && blankToNull(item.biography()) == null
                && blankToNull(item.receptionHours()) == null;
    }

    private boolean isEmpty(AboutTranslationPayload item) {
        return blankToNull(item.title()) == null
                && blankToNull(item.body()) == null
                && blankToNull(item.tasksTitle()) == null
                && blankToNull(item.goal()) == null
                && cleanTasks(item.tasks()).isEmpty();
    }

    private List<String> cleanTasks(List<String> tasks) {
        return tasks == null ? List.of() : tasks.stream()
                .filter(task -> task != null && !task.isBlank())
                .map(String::trim)
                .toList();
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
