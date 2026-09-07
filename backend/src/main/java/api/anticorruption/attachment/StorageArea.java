package api.anticorruption.attachment;

/**
 * Fayllar saqlanadigan ikki zona.
 *
 * <p>Ajratish xavfsizlik uchun: ochiq zonadagi fayllarni hech qanday tekshiruvsiz
 * berish mumkin, yopiq zonadagilarni esa faqat huquq tekshirilgandan keyin.
 * Ikkalasi bitta papkada yotsa, ochiq endpoint tasodifan dalil fayllarini ham
 * bera boshlashi mumkin edi.
 */
public enum StorageArea {

    /** Dalil fayllari. Faqat xodimlar va murojaat egasi ko'ra oladi. */
    PRIVATE("private"),

    /** Yangilik rasmlari, xodim suratlari. Hamma ko'radi. */
    PUBLIC("public");

    private final String folder;

    StorageArea(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
