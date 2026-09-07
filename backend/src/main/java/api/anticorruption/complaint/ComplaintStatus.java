package api.anticorruption.complaint;

import api.anticorruption.common.LabeledEnum;

import java.util.Set;

/**
 * Murojaatning ko'rib chiqilish bosqichlari.
 *
 * <p>Nomlar tarjima faylida: {@code enum.ComplaintStatus.*}.
 */
public enum ComplaintStatus implements LabeledEnum {

    /** Yangi kelib tushgan, hali hech kim ko'rmagan. */
    NEW,

    /** Moderator ko'rib chiqmoqda. */
    IN_REVIEW,

    /** Murojaatchidan qo'shimcha ma'lumot so'ralgan. */
    NEED_INFO,

    /** Ko'rib chiqildi va chora ko'rildi. */
    RESOLVED,

    /** Asossiz yoki vakolat doirasidan tashqari deb topildi. */
    REJECTED;

    /** Yakuniy holatlar - bundan keyin murojaat qayta ochilmaydi. */
    public boolean isFinal() {
        return this == RESOLVED || this == REJECTED;
    }

    /**
     * Shu holatdan qaysi holatlarga o'tish mumkinligini qaytaradi.
     * Bu tarixning izchil bo'lishini ta'minlaydi.
     */
    public Set<ComplaintStatus> allowedTransitions() {
        return switch (this) {
            case NEW -> Set.of(IN_REVIEW, NEED_INFO, RESOLVED, REJECTED);
            case IN_REVIEW -> Set.of(NEED_INFO, RESOLVED, REJECTED);
            case NEED_INFO -> Set.of(IN_REVIEW, RESOLVED, REJECTED);
            case RESOLVED, REJECTED -> Set.of();
        };
    }
}
