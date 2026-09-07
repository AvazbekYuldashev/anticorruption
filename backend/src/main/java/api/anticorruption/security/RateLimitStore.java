package api.anticorruption.security;

import java.time.Duration;

/**
 * So'rov chegarasi hisobi.
 *
 * <p>Ikkita amalga oshirilishi bor: xotiradagi (bitta nusxa uchun) va
 * Redis dagi (bir nechta nusxa uchun umumiy). Filtr qaysi biri ishlayotganini
 * bilmaydi - shuning uchun serverni bir nusxadan bir nechtaga o'tkazish
 * sozlamani almashtirishdan iborat bo'ladi.
 */
public interface RateLimitStore {

    /**
     * Mijozning joriy oynadagi urinishini hisobga oladi.
     *
     * @param client mijoz belgisi (odatda IP manzil)
     * @param window oyna uzunligi
     * @return shu oynadagi urinishlar soni, joriy urinish ham hisobga olingan
     */
    long increment(String client, Duration window);
}
