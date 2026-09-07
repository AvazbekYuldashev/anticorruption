package api.anticorruption.poll;

import api.anticorruption.common.LabeledEnum;

/**
 * O'tkaziladigan so'rovning turi.
 *
 * <p>Ikkalasining tuzilishi bir xil - savollar va variantlar - farqi
 * baholashda: so'rovnomada to'g'ri javob tushunchasi yo'q, testda esa
 * variantlarning bir qismi to'g'ri deb belgilanadi va ishtirokchi
 * yakunda o'z natijasini ko'radi.
 *
 * <p>Nomlar tarjima faylida: {@code enum.PollType.*}.
 */
public enum PollType implements LabeledEnum {

    /** Fikr so'rash: javoblar sanaladi, to'g'ri-noto'g'risi yo'q. */
    SURVEY,

    /** Test yoki viktorina: to'g'ri javoblar belgilanadi, natija ball bilan chiqadi. */
    QUIZ
}
