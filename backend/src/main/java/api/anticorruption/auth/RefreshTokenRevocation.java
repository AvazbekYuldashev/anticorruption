package api.anticorruption.auth;

/**
 * Yangilash tokeni nega bekor qilingani.
 *
 * <p>Bekor qilingan token qaytib kelganda nima qilish kerakligini shu hal
 * qiladi. Aylantirilgan token egasida qolmasligi kerak edi: mijozda endi
 * uning o'rnini bosgan yangisi turadi. Shuning uchun eskisining qaytib
 * kelishi zanjir ikki qo'lda ekanini bildiradi - ya'ni o'g'irlanish
 * alomati.
 *
 * <p>Ataylab bekor qilingan token (chiqish, parol almashtirish) esa hech
 * kimda ishlamaydi va uning qaytib kelishi hech narsani anglatmaydi -
 * bu shunchaki eskirgan cookie. Aks holda parolini almashtirgan odam
 * boshqa qurilmasi navbatdagi yangilashga urinishi bilanoq o'zi ham
 * tizimdan chiqib qolardi.
 */
public enum RefreshTokenRevocation {

    /** Yangilash paytida o'rniga yangisi berildi. */
    ROTATED,

    /** Ataylab uzildi: chiqish, parol almashtirish yoki o'g'irlanishga javob. */
    REVOKED
}
