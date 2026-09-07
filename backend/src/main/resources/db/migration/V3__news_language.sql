-- =============================================================================
-- Yangiliklarni tilga bog'lash.
--
-- Har bir til uchun alohida yozuv bo'ladi: ruscha maqola ko'pincha
-- o'zbekchasidan qisqaroq bo'ladi va rasmlar ham boshqacha tanlanadi,
-- shuning uchun "bitta maqola - to'rtta tarjima maydoni" modeli
-- muharrir uchun noqulay bo'lardi.
--
-- Bir maqolaning turli tildagi nusxalari `translation_group` orqali
-- bog'lanadi. Mavjud yangiliklar o'zbekcha deb belgilanadi va har biri
-- o'ziga alohida guruh oladi.
-- =============================================================================

alter table news add column language varchar(10);
alter table news add column translation_group varchar(36);

update news set language = 'UZ' where language is null;
update news set translation_group = gen_random_uuid()::text where translation_group is null;

alter table news alter column language set not null;
alter table news alter column translation_group set not null;

alter table news
    add constraint news_language_check
    check (language in ('UZ', 'UZ_CYRL', 'RU', 'EN'));

-- Bitta guruhda bir tildan faqat bitta nusxa bo'ladi.
create unique index idx_news_translation on news (translation_group, language);
