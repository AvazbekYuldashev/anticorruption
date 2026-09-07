package api.anticorruption.user;

import api.anticorruption.common.LabeledEnum;

/**
 * Tizim foydalanuvchilarining rollari.
 *
 * <p>Nomlar tarjima faylida: {@code enum.Role.*}.
 */
public enum Role implements LabeledEnum {

    /** Oddiy foydalanuvchi: murojaat yuboradi va o'z murojaatlarini ko'radi. */
    CITIZEN,

    /** Moderator: murojaatlarni ko'rib chiqadi, holatini o'zgartiradi, javob yozadi. */
    MODERATOR,

    /** Administrator: moderatorning barcha huquqlari + tuzilma, kontent va foydalanuvchilar. */
    ADMIN;

    /** Spring Security kutadigan "ROLE_" prefiksli nom. */
    public String authority() {
        return "ROLE_" + name();
    }
}
