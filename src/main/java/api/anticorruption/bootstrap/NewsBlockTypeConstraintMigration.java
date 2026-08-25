package api.anticorruption.bootstrap;

import api.anticorruption.content.NewsBlock;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * {@code news_blocks.type} ustunidagi CHECK cheklovini enum bilan moslashtiradi.
 *
 * <p>Hibernate jadval yaratilganda enum qiymatlari ro'yxatidan cheklov yasaydi,
 * lekin {@code ddl-auto=update} keyinchalik uni yangilamaydi. Shu sababli
 * enumga yangi tur qo'shilsa (masalan {@code HEADING} va {@code GALLERY}),
 * mavjud bazada yozuv eski cheklovga urilib qolardi.
 *
 * <p>Cheklov har safar qaytadan yasaladi - amal qanday holatdan boshlanmasin,
 * natija bir xil bo'ladi. Qiymatlar enumning o'zidan olinadi, ya'ni ro'yxat
 * hech qachon kod bilan ajralib qolmaydi.
 */
@Slf4j
@Component
@Order(15)
@RequiredArgsConstructor
public class NewsBlockTypeConstraintMigration implements ApplicationRunner {

    private static final String CONSTRAINT = "news_blocks_type_check";

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String values = Arrays.stream(NewsBlock.Type.values())
                .map(type -> "'" + type.name() + "'")
                .collect(Collectors.joining(", "));

        try {
            entityManager.createNativeQuery(
                            "alter table news_blocks drop constraint if exists " + CONSTRAINT)
                    .executeUpdate();
            entityManager.createNativeQuery(
                            "alter table news_blocks add constraint " + CONSTRAINT
                                    + " check (type in (" + values + "))")
                    .executeUpdate();
        } catch (RuntimeException ex) {
            // Cheklov qo'shilmasa ham ilova ishlayveradi: turni allaqachon
            // enum va xizmat qatlami tekshiradi. Shu sababli ishga tushishni
            // to'xtatmaymiz, faqat ogohlantiramiz.
            log.warn("{} cheklovini yangilab bo'lmadi: {}", CONSTRAINT, ex.getMessage());
        }
    }
}
