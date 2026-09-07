-- =============================================================================
-- Yangilash tokeni nega bekor qilingani.
--
-- Bekor qilingan token qaytib kelsa, javob sababga bog'liq. Aylantirilgan
-- token mijozda qolmasligi kerak edi - uning qaytishi o'g'irlanish alomati va
-- foydalanuvchining barcha seanslari uziladi. Ataylab bekor qilingan token
-- (chiqish, parol almashtirish) esa shunchaki eskirgan cookie: uni rad etish
-- kifoya, boshqa seanslarga tegmaydi.
--
-- Ustun faqat bekor qilingan yozuvlarda to'ladi: token amal qilayotgan bo'lsa
-- null turadi.
-- =============================================================================

alter table refresh_tokens
    add column revoked_reason varchar(20);

-- Eski yozuvlarning sababi ma'lum emas. Ular ROTATED deb belgilanadi: bekor
-- qilingan tokenlarning aksariyati aynan aylantirishdan qoladi, va noaniq
-- holatda qattiqroq javobni saqlab qolgan ma'qul.
update refresh_tokens
   set revoked_reason = 'ROTATED'
 where revoked_at is not null;
