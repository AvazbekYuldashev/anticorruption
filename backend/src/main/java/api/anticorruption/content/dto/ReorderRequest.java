package api.anticorruption.content.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Yangi tartib: barcha yozuvlarning id lari, ko'rsatiladigan tartibda.
 *
 * <p>To'liq ro'yxat yuboriladi - bitta yozuvni siljitish ham qolganlarining
 * o'rnini o'zgartiradi.
 */
public record ReorderRequest(

        @NotNull(message = "{validation.required}")
        @Size(max = 100, message = "{validation.size.max}")
        List<Long> ids
) {
}
