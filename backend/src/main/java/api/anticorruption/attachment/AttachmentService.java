package api.anticorruption.attachment;

import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.complaint.Complaint;
import api.anticorruption.complaint.ComplaintRepository;
import api.anticorruption.complaint.dto.AttachmentResponse;
import api.anticorruption.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Objects;

/** Murojaatga dalil fayllarini biriktirish va ularni yuklab olish. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ComplaintRepository complaintRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    /**
     * Kuzatuv kodi bo'yicha murojaatga fayl biriktiradi.
     *
     * <p>Bu amal autentifikatsiyasiz bajariladi: kuzatuv kodini bilish
     * murojaat egasi ekanligining isboti hisoblanadi. Kod tasodifiy va
     * taxmin qilib bo'lmaydigan darajada uzun.
     */
    @Transactional
    public AttachmentResponse attachByTrackingCode(String trackingCode, MultipartFile file) {
        Complaint complaint = complaintRepository
                .findByTrackingCode(normalizeCode(trackingCode))
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageKeys.COMPLAINT_NOT_FOUND_BY_CODE, trackingCode));

        if (complaint.getStatus().isFinal()) {
            throw new BadRequestException(MessageKeys.ATTACHMENT_COMPLAINT_CLOSED);
        }

        long existing = attachmentRepository.countByComplaintId(complaint.getId());
        if (existing >= storageProperties.maxFilesPerComplaint()) {
            throw new BadRequestException(
                    MessageKeys.ATTACHMENT_TOO_MANY, storageProperties.maxFilesPerComplaint());
        }

        String storedName = fileStorageService.store(file, StorageArea.PRIVATE);

        Attachment attachment = Attachment.builder()
                .complaint(complaint)
                .originalName(fileStorageService.sanitizeOriginalName(file.getOriginalFilename()))
                .storedName(storedName)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();

        attachmentRepository.save(attachment);
        log.info("Murojaat {} ga fayl biriktirildi: {} ({} bayt)",
                complaint.getTrackingCode(), attachment.getOriginalName(), attachment.getSizeBytes());

        return AttachmentResponse.from(attachment);
    }

    /**
     * Faylni yuklab olish uchun tayyorlaydi.
     *
     * <p>Ruxsat: xodimlar barcha fayllarni ko'ra oladi; oddiy foydalanuvchi
     * faqat o'zi yuborgan murojaatning fayllarini. Anonim murojaat fayllariga
     * faqat xodimlar kira oladi.
     */
    @Transactional(readOnly = true)
    public DownloadableAttachment prepareDownload(Long attachmentId, User requester) {
        Attachment attachment = attachmentRepository.findDetailById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_ATTACHMENT, attachmentId));

        if (!canAccess(attachment, requester)) {
            // Matn muhim emas: javob xabarini GlobalExceptionHandler tarjima qiladi.
            throw new AccessDeniedException("attachment access denied");
        }

        Resource resource = fileStorageService.load(attachment.getStoredName(), StorageArea.PRIVATE);
        return new DownloadableAttachment(resource, attachment.getOriginalName(), attachment.getContentType());
    }

    private boolean canAccess(Attachment attachment, User requester) {
        if (requester == null) {
            return false;
        }
        if (requester.isStaff()) {
            return true;
        }
        User author = attachment.getComplaint().getAuthor();
        return author != null && Objects.equals(author.getId(), requester.getId());
    }

    private String normalizeCode(String trackingCode) {
        return trackingCode == null ? null : trackingCode.trim().toUpperCase(Locale.ROOT);
    }

    /** Yuklab olish uchun fayl va uning ko'rsatiladigan nomi. */
    public record DownloadableAttachment(Resource resource, String fileName, String contentType) {
    }
}
