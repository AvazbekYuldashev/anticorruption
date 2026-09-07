package api.anticorruption.complaint.dto;

/**
 * Fakultet kesimidagi ko'rsatkichlar.
 *
 * <p>Ataylab yagona "ball" hisoblanmaydi. Murojaatlar soni ko'p bo'lishi ikki
 * xil narsani anglatishi mumkin: fakultetda muammo ko'p, yoki aksincha -
 * odamlar tizimga ishonadi va xabar berishdan qo'rqmaydi. Bir raqamga
 * siqib qo'yilsa, ochiq fakultet nohaq yomon ko'rinib qolardi.
 * Shuning uchun xom ko'rsatkichlar beriladi va talqin o'quvchiga qoldiriladi.
 *
 * @param resolutionRate yakunlangan murojaatlar ulushi, foizda -
 *                       bu ko'rsatkich esa aniq ma'noga ega: ish qanchalik oxiriga yetkazilgan
 */
public record FacultyRatingResponse(
        Long facultyId,
        String facultyName,
        long total,
        long resolved,
        long closed,
        long open,
        double resolutionRate,
        Double averageResolutionDays
) {
}
