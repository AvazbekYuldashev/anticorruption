package api.anticorruption.poll.dto;

import java.time.Instant;

/**
 * So'rovnomani qayta o'tkazish.
 *
 * <p>Ikkala sana ham ixtiyoriy: qo'yilmasa yangi o'tkazish darhol boshlanadi
 * va qo'lda to'xtatilgunicha davom etadi.
 */
public record RestartPollRequest(Instant startsAt, Instant endsAt) {
}
