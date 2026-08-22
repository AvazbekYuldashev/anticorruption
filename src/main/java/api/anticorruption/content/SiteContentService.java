package api.anticorruption.content;

import api.anticorruption.attachment.FileStorageService;
import api.anticorruption.attachment.StorageArea;
import api.anticorruption.common.Slugs;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.content.dto.SaveStaffMemberRequest;
import api.anticorruption.content.dto.SaveStaticPageRequest;
import api.anticorruption.content.dto.SaveUsefulLinkRequest;
import api.anticorruption.content.dto.StaffMemberResponse;
import api.anticorruption.content.dto.StaticPageResponse;
import api.anticorruption.content.dto.UsefulLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Saytning kamdan-kam o'zgaradigan bo'limlari: xodimlar ro'yxati,
 * matnli sahifalar va foydali havolalar.
 *
 * <p>Uchalasi bitta xizmatda, chunki mantiqan bir xil: admin kiritadi,
 * tashrifchi o'qiydi, hech qanday murakkab qoida yo'q.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteContentService {

    private static final int DEFAULT_ORDER = 100;

    private final StaffMemberRepository staffMemberRepository;
    private final StaticPageRepository staticPageRepository;
    private final UsefulLinkRepository usefulLinkRepository;
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

    // ================================================================ sahifalar

    @Transactional(readOnly = true)
    public List<StaticPageResponse> listPages(boolean includeUnpublished) {
        if (includeUnpublished) {
            return staticPageRepository.findAllByOrderByDisplayOrderAscTitleAsc().stream()
                    .map(StaticPageResponse::from)
                    .toList();
        }
        // Ochiq ro'yxatda matn qaytarilmaydi - u faqat sahifaning o'zida kerak.
        return staticPageRepository.findByPublishedTrueOrderByDisplayOrderAscTitleAsc().stream()
                .map(StaticPageResponse::withoutBody)
                .toList();
    }

    @Transactional(readOnly = true)
    public StaticPageResponse readPublishedPage(String slug) {
        return staticPageRepository.findBySlugAndPublishedTrue(slug)
                .map(StaticPageResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PAGE_NOT_FOUND_BY_SLUG, slug));
    }

    @Transactional(readOnly = true)
    public StaticPageResponse findPage(Long pageId) {
        return StaticPageResponse.from(requirePage(pageId));
    }

    @Transactional
    public StaticPageResponse createPage(SaveStaticPageRequest request) {
        String slug = resolveSlug(request);
        if (staticPageRepository.existsBySlug(slug)) {
            throw new ConflictException(MessageKeys.PAGE_SLUG_TAKEN, slug);
        }

        StaticPage page = StaticPage.builder()
                .slug(slug)
                .title(request.title().trim())
                .body(request.body().trim())
                .displayOrder(request.displayOrder() == null ? DEFAULT_ORDER : request.displayOrder())
                .published(request.published() == null || request.published())
                .build();

        staticPageRepository.save(page);
        log.info("Sahifa yaratildi: {}", slug);

        return StaticPageResponse.from(page);
    }

    @Transactional
    public StaticPageResponse updatePage(Long pageId, SaveStaticPageRequest request) {
        StaticPage page = requirePage(pageId);
        String slug = resolveSlug(request);

        if (!page.getSlug().equals(slug) && staticPageRepository.existsBySlug(slug)) {
            throw new ConflictException(MessageKeys.PAGE_SLUG_TAKEN, slug);
        }

        page.setSlug(slug);
        page.setTitle(request.title().trim());
        page.setBody(request.body().trim());
        if (request.displayOrder() != null) {
            page.setDisplayOrder(request.displayOrder());
        }
        if (request.published() != null) {
            page.setPublished(request.published());
        }

        staticPageRepository.save(page);
        return StaticPageResponse.from(page);
    }

    @Transactional
    public void deletePage(Long pageId) {
        staticPageRepository.delete(requirePage(pageId));
    }

    // ================================================================ havolalar

    @Transactional(readOnly = true)
    public List<UsefulLinkResponse> listLinks(boolean includeInactive) {
        List<UsefulLink> links = includeInactive
                ? usefulLinkRepository.findAllByOrderByDisplayOrderAscTitleAsc()
                : usefulLinkRepository.findByActiveTrueOrderByDisplayOrderAscTitleAsc();
        return links.stream().map(UsefulLinkResponse::from).toList();
    }

    @Transactional
    public UsefulLinkResponse createLink(SaveUsefulLinkRequest request) {
        UsefulLink link = UsefulLink.builder()
                .title(request.title().trim())
                .url(request.url().trim())
                .description(blankToNull(request.description()))
                .groupName(blankToNull(request.groupName()))
                .displayOrder(request.displayOrder() == null ? DEFAULT_ORDER : request.displayOrder())
                .active(request.active() == null || request.active())
                .build();

        usefulLinkRepository.save(link);
        return UsefulLinkResponse.from(link);
    }

    @Transactional
    public UsefulLinkResponse updateLink(Long linkId, SaveUsefulLinkRequest request) {
        UsefulLink link = requireLink(linkId);

        link.setTitle(request.title().trim());
        link.setUrl(request.url().trim());
        link.setDescription(blankToNull(request.description()));
        link.setGroupName(blankToNull(request.groupName()));
        if (request.displayOrder() != null) {
            link.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            link.setActive(request.active());
        }

        usefulLinkRepository.save(link);
        return UsefulLinkResponse.from(link);
    }

    @Transactional
    public void deleteLink(Long linkId) {
        usefulLinkRepository.delete(requireLink(linkId));
    }

    // ================================================================ yordamchilar

    /** Slug ko'rsatilmagan bo'lsa sarlavhadan yasaladi. */
    private String resolveSlug(SaveStaticPageRequest request) {
        if (request.slug() != null && !request.slug().isBlank()) {
            return request.slug().trim();
        }
        String generated = Slugs.from(request.title());
        return generated.isEmpty() ? "sahifa" : generated;
    }

    private StaffMember requireStaff(Long memberId) {
        return staffMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_STAFF_MEMBER, memberId));
    }

    private StaticPage requirePage(Long pageId) {
        return staticPageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_PAGE, pageId));
    }

    private UsefulLink requireLink(Long linkId) {
        return usefulLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_LINK, linkId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
