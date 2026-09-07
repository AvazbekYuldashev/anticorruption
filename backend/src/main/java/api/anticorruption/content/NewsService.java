package api.anticorruption.content;

import api.anticorruption.attachment.FileStorageService;
import api.anticorruption.attachment.StorageArea;
import api.anticorruption.attachment.StorageProperties;
import api.anticorruption.common.Slugs;
import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.AppLanguage;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.content.dto.NewsDetailResponse;
import api.anticorruption.content.dto.NewsSummaryResponse;
import api.anticorruption.content.dto.NewsTranslationResponse;
import api.anticorruption.content.dto.SaveNewsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Yangiliklar bo'limi va ularning blokli mazmuni. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final int MAX_SLUG_ATTEMPTS = 50;

    private final NewsRepository newsRepository;
    private final NewsBlockRepository newsBlockRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final Translator translator;

    // ---------------------------------------------------------------- ochiq

    /**
     * Chop etilgan yangiliklar - so'rov tilida.
     *
     * <p>Til {@code ?lang=} parametridan olinadi. So'ralgan tilda nusxasi
     * bo'lmagan maqola ro'yxatdan tushib qolmaydi: uning asosiy tildagi
     * varianti ko'rsatiladi.
     */
    @Transactional(readOnly = true)
    public PageResponse<NewsSummaryResponse> listPublished(String query, Pageable pageable) {
        AppLanguage language = translator.currentLanguage();

        Page<News> page = (query == null || query.isBlank())
                ? newsRepository.findPublishedInLanguage(language, AppLanguage.DEFAULT, pageable)
                : newsRepository.searchPublishedInLanguage(
                        "%" + query.trim().toLowerCase(Locale.ROOT) + "%",
                        language, AppLanguage.DEFAULT, pageable);

        return toSummaryPage(page);
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
        return NewsDetailResponse.from(news, news.getViewCount() + 1, publishedTranslations(news));
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public PageResponse<NewsSummaryResponse> listAll(Pageable pageable) {
        return toSummaryPage(newsRepository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public NewsDetailResponse findById(Long newsId) {
        News news = requireNews(newsId);
        return NewsDetailResponse.from(news, allTranslations(news));
    }

    @Transactional
    public NewsDetailResponse create(SaveNewsRequest request) {
        boolean published = Boolean.TRUE.equals(request.published());
        AppLanguage language = AppLanguage.from(request.language());
        String group = translationGroupFor(request.translationOf(), language);

        News news = News.builder()
                .slug(uniqueSlug(request.title(), null))
                .language(language)
                .translationGroup(group)
                .title(request.title().trim())
                .summary(blankToNull(request.summary()))
                .published(published)
                .publishedAt(published ? Instant.now() : null)
                .build();

        applyBlocks(news, request.blocks());
        newsRepository.save(news);

        log.info("Yangilik yaratildi: {} ({}, {} ta blok, chop etilgan: {})",
                news.getSlug(), language.getCode(), news.getBlocks().size(), published);

        return NewsDetailResponse.from(news, allTranslations(news));
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

        if (request.language() != null && !request.language().isBlank()) {
            AppLanguage language = AppLanguage.from(request.language());
            if (language != news.getLanguage()) {
                requireLanguageFree(news.getTranslationGroup(), language, news.getId());
                news.setLanguage(language);
            }
        }

        if (request.published() != null) {
            applyPublishState(news, request.published());
        }

        // Ro'yxatdan chiqarilgan rasmlarni keyin diskdan ham o'chiramiz.
        Set<String> before = imageNames(news);
        applyBlocks(news, request.blocks());
        newsRepository.save(news);

        Set<String> removed = new HashSet<>(before);
        removed.removeAll(imageNames(news));
        removed.forEach(name -> fileStorageService.delete(name, StorageArea.PUBLIC));

        return NewsDetailResponse.from(news, allTranslations(news));
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
        return NewsDetailResponse.from(news, allTranslations(news));
    }

    /** Muqova rasmini almashtiradi. Eski rasm diskdan o'chiriladi. */
    @Transactional
    public NewsDetailResponse replaceCover(Long newsId, MultipartFile image) {
        News news = requireNews(newsId);
        String previous = news.getCoverImage();

        news.setCoverImage(fileStorageService.store(image, StorageArea.PUBLIC));
        newsRepository.save(news);

        fileStorageService.delete(previous, StorageArea.PUBLIC);
        return NewsDetailResponse.from(news, allTranslations(news));
    }

    @Transactional
    public void delete(Long newsId) {
        News news = requireNews(newsId);

        // Fayl nomlari entity o'chirilgunicha yig'ib olinadi.
        Set<String> files = new HashSet<>(imageNames(news));
        if (news.getCoverImage() != null) {
            files.add(news.getCoverImage());
        }

        newsRepository.delete(news);
        files.forEach(name -> fileStorageService.delete(name, StorageArea.PUBLIC));

        log.info("Yangilik o'chirildi: id={} ({} ta fayl bilan)", newsId, files.size());
    }

    // ---------------------------------------------------------------- bloklar

    /**
     * Blok ro'yxatini to'liq almashtiradi.
     *
     * <p>Mavjud bloklar tozalanib, so'rovdagi tartibda qaytadan yasaladi.
     * Bloklarni id bo'yicha solishtirib yangilash ham mumkin edi, lekin
     * bu yerda foyda bermaydi: blokda saqlanadigan narsa matn yoki fayl
     * nomi, ikkalasi ham arzon. Almashtirish esa tartibni chalkashtirmaydi.
     */
    private void applyBlocks(News news, List<SaveNewsRequest.SaveNewsBlockRequest> requested) {
        news.getBlocks().clear();

        if (requested == null) {
            news.setBody("");
            return;
        }

        int order = 0;
        for (SaveNewsRequest.SaveNewsBlockRequest item : requested) {
            news.getBlocks().add(buildBlock(news, item, order++));
        }

        // Chegara butun yangilik bo'yicha: yakka rasmlar va albom rasmlari birga
        // hisoblanadi, chunki sahifani og'irlashtiradigan narsa umumiy son.
        int images = imageNames(news).size();
        if (images > storageProperties.maxImagesPerNews()) {
            throw new BadRequestException(
                    MessageKeys.NEWS_TOO_MANY_IMAGES, storageProperties.maxImagesPerNews());
        }

        news.setBody(joinText(news));
    }

    private NewsBlock buildBlock(News news, SaveNewsRequest.SaveNewsBlockRequest item, int order) {
        NewsBlock.Type type = parseType(item.type());

        NewsBlock block = NewsBlock.builder()
                .news(news)
                .type(type)
                .caption(blankToNull(item.caption()))
                .displayOrder(order)
                .build();

        switch (type) {
            // Sarlavha va matn faqat ko'rinishi bilan farq qiladi, saqlanishi bir xil.
            case HEADING, TEXT -> {
                String text = blankToNull(item.text());
                if (text == null) {
                    throw new BadRequestException(MessageKeys.NEWS_TEXT_BLOCK_EMPTY);
                }
                block.setText(text);
            }
            case IMAGE -> {
                String storedName = blankToNull(item.storedName());
                if (storedName == null) {
                    throw new BadRequestException(MessageKeys.NEWS_IMAGE_BLOCK_MISSING);
                }
                block.setStoredName(storedName);
                block.setOriginalName(blankToNull(item.originalName()));
            }
            case GALLERY -> applyGalleryImages(block, item.images());
        }

        return block;
    }

    /**
     * Albom rasmlarini qo'shadi.
     *
     * <p>Bo'sh albom rad etiladi: u sahifada hech narsa ko'rsatmaydi, ya'ni
     * muallif rasm tanlashni unutgan bo'ladi - buni jimgina saqlab qo'yish
     * xatoni yashirish bo'lardi.
     */
    private void applyGalleryImages(NewsBlock block, List<SaveNewsRequest.SaveNewsBlockImageRequest> images) {
        if (images == null || images.isEmpty()) {
            throw new BadRequestException(MessageKeys.NEWS_GALLERY_BLOCK_EMPTY);
        }

        int order = 0;
        for (SaveNewsRequest.SaveNewsBlockImageRequest item : images) {
            String storedName = blankToNull(item.storedName());
            if (storedName == null) {
                throw new BadRequestException(MessageKeys.NEWS_IMAGE_BLOCK_MISSING);
            }
            block.getImages().add(NewsBlockImage.builder()
                    .block(block)
                    .storedName(storedName)
                    .originalName(blankToNull(item.originalName()))
                    .caption(blankToNull(item.caption()))
                    .displayOrder(order++)
                    .build());
        }
    }

    private NewsBlock.Type parseType(String value) {
        try {
            return NewsBlock.Type.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(MessageKeys.NEWS_BLOCK_TYPE_INVALID, value);
        }
    }

    /** Qidiruv uchun matn va sarlavha bloklarini birlashtiradi. */
    private String joinText(News news) {
        return news.getBlocks().stream()
                .filter(NewsBlock::isTextual)
                .map(NewsBlock::getText)
                .collect(Collectors.joining("\n\n"));
    }

    private Set<String> imageNames(News news) {
        return news.getBlocks().stream()
                .flatMap(block -> block.imageNames().stream())
                .collect(Collectors.toSet());
    }

    // ---------------------------------------------------------------- yordamchilar

    /** Sahifadagi barcha yangiliklar uchun rasmlar sonini bitta so'rovda oladi. */
    private PageResponse<NewsSummaryResponse> toSummaryPage(Page<News> page) {
        List<Long> ids = page.getContent().stream().map(News::getId).toList();
        Map<Long, Long> counts = countImages(ids);

        return PageResponse.from(page.map(news ->
                NewsSummaryResponse.from(news, counts.getOrDefault(news.getId(), 0L))));
    }

    private Map<Long, Long> countImages(List<Long> newsIds) {
        if (newsIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        accumulate(counts, newsBlockRepository.countSingleImagesGroupedByNewsIds(newsIds));
        accumulate(counts, newsBlockRepository.countGalleryImagesGroupedByNewsIds(newsIds));
        return counts;
    }

    private void accumulate(Map<Long, Long> counts, List<Object[]> rows) {
        for (Object[] row : rows) {
            counts.merge((Long) row[0], (Long) row[1], Long::sum);
        }
    }

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

    /**
     * Yangi yozuv qaysi tarjima guruhiga tegishli bo'lishini aniqlaydi.
     *
     * <p>{@code translationOf} berilmasa - bu mustaqil maqola va o'ziga
     * yangi guruh ochadi. Berilsa - o'sha maqolaning guruhiga qo'shiladi,
     * lekin bitta guruhda bir tildan ikkitasi bo'lishi mumkin emas.
     */
    private String translationGroupFor(Long translationOf, AppLanguage language) {
        if (translationOf == null) {
            return UUID.randomUUID().toString();
        }

        String group = requireNews(translationOf).getTranslationGroup();
        requireLanguageFree(group, language, null);
        return group;
    }

    private void requireLanguageFree(String group, AppLanguage language, Long exceptId) {
        boolean taken = exceptId == null
                ? newsRepository.existsByTranslationGroupAndLanguage(group, language)
                : newsRepository.existsByTranslationGroupAndLanguageAndIdNot(group, language, exceptId);

        if (taken) {
            throw new ConflictException(MessageKeys.NEWS_TRANSLATION_EXISTS, language.getDisplayName());
        }
    }

    /** Boshqa tillardagi nusxalar - o'zidan tashqari. */
    private List<NewsTranslationResponse> allTranslations(News news) {
        return toTranslations(newsRepository.findByTranslationGroupOrderByLanguageAsc(
                news.getTranslationGroup()), news.getId());
    }

    private List<NewsTranslationResponse> publishedTranslations(News news) {
        return toTranslations(newsRepository.findByTranslationGroupAndPublishedTrueOrderByLanguageAsc(
                news.getTranslationGroup()), news.getId());
    }

    private List<NewsTranslationResponse> toTranslations(List<News> siblings, Long selfId) {
        return siblings.stream()
                .filter(item -> !item.getId().equals(selfId))
                .map(NewsTranslationResponse::from)
                .toList();
    }

    private News requireNews(Long newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_NEWS, newsId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
