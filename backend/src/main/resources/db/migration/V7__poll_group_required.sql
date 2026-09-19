-- =============================================================================
-- Guruh majburiy bo'ldi.
--
-- Guruh - bu bitta o'tkazish: "Korrupsiyaga qarshi kurash oyligi 2026" da
-- bir guruh test va bir guruh so'rovnoma o'tkaziladi, keyingi yilgisi esa
-- alohida guruhda bo'ladi. Guruhsiz so'rovnoma bu tartibdan tushib qoladi,
-- shuning uchun endi har bir so'rovnoma guruhga tegishli.
--
-- V6 guruhni ixtiyoriy qilib qo'shgan edi, shuning uchun bazada guruhsiz
-- yozuvlar bo'lishi mumkin. Ular o'chirilmaydi: har bir tur uchun bittadan
-- guruh ochiladi va eski yozuvlar o'sha yerga o'tadi. Administrator keyin
-- ularni qayta nomlaydi yoki boshqa guruhlarga taqsimlaydi.
-- =============================================================================

insert into poll_groups (name, description, type, display_order, created_at, updated_at)
select 'Umumiy so''rovnomalar',
       'Guruhlar joriy etilgunga qadar yaratilgan so''rovnomalar',
       'SURVEY',
       0,
       now(),
       now()
 where exists (select 1 from polls
                where group_id is null
                  and (type is null or type = 'SURVEY'));

insert into poll_groups (name, description, type, display_order, created_at, updated_at)
select 'Umumiy testlar',
       'Guruhlar joriy etilgunga qadar yaratilgan testlar',
       'QUIZ',
       0,
       now(),
       now()
 where exists (select 1 from polls where group_id is null and type = 'QUIZ');

-- Turi ko'rsatilmagan eski yozuvlar so'rovnoma hisoblanadi (Poll.typeOrSurvey).
update polls
   set group_id = (select id from poll_groups where type = 'SURVEY' and name = 'Umumiy so''rovnomalar')
 where group_id is null
   and (type is null or type = 'SURVEY');

update polls
   set group_id = (select id from poll_groups where type = 'QUIZ' and name = 'Umumiy testlar')
 where group_id is null
   and type = 'QUIZ';

alter table polls
    alter column group_id set not null;

-- Guruh endi bo'sh qiymatga tushmaydi, shuning uchun "o'chirilsa null qo'y"
-- qoidasi ham ishlamaydi: to'ldirilgan guruhni o'chirish taqiqlanadi
-- (PollGroupService). Cheklovni oddiy tashqi kalitga almashtiramiz.
alter table polls
    drop constraint fk_polls_group;

alter table polls
    add constraint fk_polls_group
    foreign key (group_id) references poll_groups (id);
