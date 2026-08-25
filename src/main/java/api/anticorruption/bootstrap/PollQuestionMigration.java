package api.anticorruption.bootstrap;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Eski so'rovnomalarni ko'p savolli modelga ko'chiradi.
 *
 * <p>Ilgari so'rovnoma bitta savoldan iborat edi: savol matni
 * {@code polls.question} da, variantlar esa to'g'ridan-to'g'ri so'rovnomaga
 * bog'langan edi. Endi so'rovnomada cheklanmagan sondagi savol bo'ladi va
 * har bir variant o'z savoliga tegishli.
 *
 * <p>Ko'chirish eski {@code polls.question} ustuni borligiga qarab
 * boshlanadi: yangi bazada bunday ustun yo'q va bu sinf hech narsa
 * qilmaydi. Har bir amal "allaqachon bajarilganmi" shartiga ega, shuning
 * uchun takroriy ishga tushirish xavfsiz.
 *
 * <p>Ko'chirish barcha muhitlarda bajarilgach, bu sinfni va u qoldirgan
 * eski ustunlarni ({@code polls.question}, {@code polls.multiple_choice},
 * {@code poll_options.poll_id}) o'chirish mumkin.
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class PollQuestionMigration implements ApplicationRunner {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!columnExists("polls", "question")) {
            return;
        }

        // Yangi yozuvlar eski ustunlarni to'ldirmaydi, shuning uchun ular
        // majburiy bo'lib qolsa birinchi saqlash xato beradi.
        dropNotNull("polls", "question");
        dropNotNull("polls", "multiple_choice");
        dropNotNull("poll_options", "poll_id");

        int titles = execute("""
                update polls set title = question
                where title is null and question is not null
                """);

        int questions = execute("""
                insert into poll_questions
                    (poll_id, text, multiple_choice, required, answered_count, display_order)
                select p.id, p.question, coalesce(p.multiple_choice, false), true, p.voter_count, 0
                from polls p
                where p.question is not null
                  and not exists (select 1 from poll_questions q where q.poll_id = p.id)
                """);

        int options = execute("""
                update poll_options o
                set question_id = (
                    select q.id from poll_questions q
                    where q.poll_id = o.poll_id
                    order by q.display_order, q.id
                    limit 1)
                where o.question_id is null and o.poll_id is not null
                """);

        // Ovozlar savolni variant orqali topadi - variantlar bog'langandan keyin.
        int votes = execute("""
                update poll_votes v
                set question_id = (select o.question_id from poll_options o where o.id = v.option_id)
                where v.question_id is null
                """);

        if (questions > 0 || options > 0) {
            log.info("So'rovnomalar ko'p savolli modelga ko'chirildi: "
                            + "{} ta sarlavha, {} ta savol, {} ta variant, {} ta ovoz",
                    titles, questions, options, votes);
        }
    }

    private boolean columnExists(String table, String column) {
        Object count = entityManager.createNativeQuery("""
                        select count(*) from information_schema.columns
                        where lower(table_name) = :table and lower(column_name) = :column
                        """)
                .setParameter("table", table)
                .setParameter("column", column)
                .getSingleResult();

        return ((Number) count).longValue() > 0;
    }

    /**
     * Eski ustunni ixtiyoriy qiladi.
     *
     * <p>Bu yerga faqat eski PostgreSQL bazasi kelib tushadi, shuning uchun
     * sintaksis ham PostgreSQL niki. Xato yuz bersa uni yutib yuborish
     * mumkin emas: PostgreSQL da muvaffaqiyatsiz amal butun tranzaksiyani
     * to'xtatadi, ya'ni ko'chirishning qolgani baribir bajarilmasdi.
     */
    private void dropNotNull(String table, String column) {
        if (columnExists(table, column)) {
            execute("alter table " + table + " alter column " + column + " drop not null");
        }
    }

    private int execute(String sql) {
        return entityManager.createNativeQuery(sql).executeUpdate();
    }
}
