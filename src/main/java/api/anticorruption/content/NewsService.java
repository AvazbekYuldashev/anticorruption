package api.anticorruption.content;

import api.anticorruption.attachment.FileStorageService;
import api.anticorruption.attachment.StorageArea;
import api.anticorruption.common.Slugs;
import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.content.dto.NewsDetailResponse;
import api.anticorruption.content.dto.NewsSummaryResponse;
import api.anticorruption.content.dto.SaveNewsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Locale;

/** Yangiliklar bo'limi. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final int MAX_SLUG_ATTEMPTS = 50;

    private final NewsRepository newsRepository;
    private final FileStorageService fileStorageService;

    // ---------------------------------------------------------------- ochiq

    @Transactional(readOnly = true)
    public PageResponse<NewsSummaryResponse> listPublished(String query, Pageable pageable) {
        Page<News> page = (query == null || query.isBlank())
                ? newsRepository.findByPublishedTrueOrderByPublishedAtDesc(pageable)
                : newsRepository.searchPublished("%" + query.trim().toLowerCase(Locale.ROOT) + "%", pageable);

        return PageResponse.from(page.map(NewsSummaryResponse::from));
    }

    /**
     * Chop etilgan yangilikni slug bo'yicha qaytaradi va ko'rishlar sonini oshiradi.
     * Qoralama yangilik bu yerda topilmaydi - u faqat admin panelida ko'rinadi.
     */
    @Transactional
    public NewsDetailResponse readPublished(String slug) {
        News news = newsRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NEWS_NOT_FOUND_BY_SLUG, slug));

        newsRepository.incrementViewCount(news.getId());

        // Entity ataylab o'zgartirilmaydi - javobda yangi son ko'rinsin, xolos.
        return NewsDetailResponse.from(news, news.getViewCount() + 1);
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public PageResponse<NewsSummaryResponse> listAll(Pageable pageable) {
        return PageResponse.from(newsRepository.findAll(pageable).map(NewsSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public NewsDetailResponse findById(Long newsId) {
        return NewsDetailResponse.from(requireNews(newsId));
    }

    @Transactional
    public NewsDetailResponse create(SaveNewsRequest request) {
        boolean published = Boolean.TRUE.equals(request.published());

        News news = News.builder()
                .slug(uniqueSlug(request.title(), null))
                .title(request.title().trim())
                .summary(blankToNull(request.summary()))
                .body(request.body().trim())
                .published(published)
                .publishedAt(published ? Instant.now() : null)
                .build();

        newsRepository.save(news);
        log.info("Yangilik yaratildi: {} (chop etilgan: {})", news.getSlug(), published);

        return NewsDetailResponse.from(news);
    }

    @Transactional
    public NewsDetailResponse update(Long newsId, SaveNewsRequest request) {
        News news = requireNews(newsId);

        // Sarlavha o'zgarsa slug ham yangilanadi, lekin chop etilgan yangilikda emas:
        // tashqi havolalar buzilib ketmasligi kerak.
        if (!news.isPublished() && !news.getTitle().equals(request.title().trim())) {
            news.setSlug(uniqueSlug(request.title(), news.getId()));
        }

        news.setTitle(request.title().trim());
        news.setSummary(blankToNull(request.summary()));
        news.setBody(request.body().trim());

        if (request.published() != null) {
            applyPublishState(news, request.published());
        }

        newsRepository.save(news);
        return NewsDetailResponse.from(news);
    }

    @Transactional
    public NewsDetailResponse setPublished(Long newsId, boolean published) {
        News news = requireNews(newsId);
        if (news.isPublished() == published) {
            throw new BadRequestException(published
                    ? MessageKeys.NEWS_ALREADY_PUBLISHED
                    : MessageKeys.NEWS_ALREADY_DRAFT);
        }
        applyPublishState(news, published);
        newsRepository.save(news);
        return NewsDetailResponse.from(news);
    }

    /** Muqova rasmini almashtiradi. Eski rasm diskdan o'chiriladi. */
    @Transactional
    public NewsDetailResponse replaceCover(Long newsId, MultipartFile image) {
        News news = requireNews(newsId);
        String previous = news.getCoverImage();

        news.setCoverImage(fileStorageService.store(image, StorageArea.PUBLIC));
        newsRepository.save(news);

        fileStorageService.delete(previous, StorageArea.PUBLIC);
        return NewsDetailResponse.from(news);
    }

    @Transactional
    public void delete(Long newsId) {
        News news = requireNews(newsId);
        String cover = news.getCoverImage();

        newsRepository.delete(news);
        fileStorageService.delete(cover, StorageArea.PUBLIC);
        log.info("Yangilik o'chirildi: id={}", newsId);
    }

    // ---------------------------------------------------------------- yordamchilar

    /** Chop etilgan sana faqat birinchi marta chop etilganda qo'yiladi. */
    private void applyPublishState(News news, boolean published) {
        news.setPublished(published);
        if (published && news.getPublishedAt() == null) {
            news.setPublishedAt(Instant.now());
        }
    }

    /**
     * Sarlavhadan slug yasaydi. Band bo'lsa oxiriga raqam qo'shadi.
     *
     * @param currentId tahrirlanayotgan yangilik id si (o'zi bilan to'qnashmasligi uchun)
     */
    private String uniqueSlug(String title, Long currentId) {
        String base = Slugs.from(title);
        if (base.isEmpty()) {
            base = "yangilik";
        }

        String candidate = base;
        for (int suffix = 2; suffix < MAX_SLUG_ATTEMPTS; suffix++) {
            if (isSlugFree(candidate, currentId)) {
                return candidate;
            }
            candidate = base + "-" + suffix;
        }
        // Juda kam uchraydigan holat: oxirida vaqt belgisi bilan kafolatlangan noyob nom.
        return base + "-" + Instant.now().toEpochMilli();
    }

    private boolean isSlugFree(String slug, Long currentId) {
        return newsRepository.findBySlug(slug)
                .map(existing -> existing.getId().equals(currentId))
                .orElse(true);
    }

    private News requireNews(Long newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_NEWS, newsId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
