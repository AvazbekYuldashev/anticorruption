package api.anticorruption.content;

import api.anticorruption.attachment.FileStorageService;
import api.anticorruption.attachment.StorageArea;
import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.content.dto.HomeBannerImageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bosh banner fonining rasmlari: bitta rasm yoki albom.
 *
 * <p>Rasmlar matnlardan alohida, yuklanishi bilan saqlanadi: fayl diskka
 * tushgach uni "saqlanmagan" holda ushlab turishning ma'nosi yo'q.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeBannerService {

    /**
     * Albomdagi rasmlar chegarasi.
     *
     * <p>Banner bosh sahifaning birinchi ekrani: har bir rasm tashrifchiga
     * yuklanadi, shuning uchun albom katta bo'lsa sahifa sekinlashadi.
     */
    static final int MAX_IMAGES = 10;

    private final HomeBannerImageRepository imageRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<HomeBannerImageResponse> listForSite() {
        return imageRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(HomeBannerImageResponse::forSite)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeBannerImageResponse> listForAdmin() {
        return imageRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(HomeBannerImageResponse::forAdmin)
                .toList();
    }

    /**
     * Rasmlarni albom oxiriga qo'shadi.
     *
     * <p>Bittasi rad etilsa (turi noto'g'ri, zararli) butun to'plam qabul
     * qilinmaydi va shu so'rovda diskka yozilganlari o'chiriladi - albomda
     * yarim yuklangan to'plam qolmasin.
     */
    @Transactional
    public List<HomeBannerImageResponse> add(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException(MessageKeys.FILE_NONE_SELECTED);
        }

        List<HomeBannerImage> current = imageRepository.findAllByOrderByDisplayOrderAscIdAsc();
        if (current.size() + files.size() > MAX_IMAGES) {
            throw new BadRequestException(MessageKeys.HOME_BANNER_TOO_MANY, MAX_IMAGES);
        }

        int order = current.stream().mapToInt(HomeBannerImage::getDisplayOrder).max().orElse(-1) + 1;
        List<String> stored = new ArrayList<>(files.size());

        try {
            for (MultipartFile file : files) {
                String storedName = fileStorageService.store(file, StorageArea.PUBLIC);
                stored.add(storedName);

                imageRepository.save(HomeBannerImage.builder()
                        .storedName(storedName)
                        .originalName(fileStorageService.sanitizeOriginalName(file.getOriginalFilename()))
                        .displayOrder(order++)
                        .build());
            }
        } catch (RuntimeException ex) {
            stored.forEach(name -> fileStorageService.delete(name, StorageArea.PUBLIC));
            throw ex;
        }

        log.info("Bosh bannerga {} ta rasm qo'shildi", files.size());
        return listForAdmin();
    }

    /** Albom tartibini o'zgartiradi. Ro'yxatda albomdagi barcha rasmlar bo'lishi shart. */
    @Transactional
    public List<HomeBannerImageResponse> reorder(List<Long> ids) {
        List<HomeBannerImage> images = imageRepository.findAllByOrderByDisplayOrderAscIdAsc();
        Map<Long, HomeBannerImage> byId = images.stream()
                .collect(Collectors.toMap(HomeBannerImage::getId, Function.identity()));

        if (ids.size() != images.size() || !new HashSet<>(ids).equals(byId.keySet())) {
            throw new BadRequestException(MessageKeys.HOME_BANNER_ORDER_MISMATCH);
        }

        int order = 0;
        for (Long id : ids) {
            byId.get(id).setDisplayOrder(order++);
        }
        imageRepository.saveAll(images);
        return listForAdmin();
    }

    /** Rasmni albomdan va diskdan o'chiradi. */
    @Transactional
    public List<HomeBannerImageResponse> delete(Long imageId) {
        HomeBannerImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageKeys.NOT_FOUND_HOME_BANNER_IMAGE, imageId));

        imageRepository.delete(image);
        fileStorageService.delete(image.getStoredName(), StorageArea.PUBLIC);

        log.info("Bosh banner rasmi o'chirildi: id={}", imageId);
        return listForAdmin();
    }
}
