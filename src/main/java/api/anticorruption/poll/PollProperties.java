package api.anticorruption.poll;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * So'rovnoma sozlamalari (application.properties dagi "app.poll.*").
 *
 * @param voteSalt ovoz beruvchi belgisini xeshlashda ishlatiladigan maxfiy tuz.
 *                 Tuzsiz xeshni oddiy IP ro'yxati bilan taqqoslab, kim ovoz
 *                 berganini aniqlab olish mumkin bo'lardi.
 */
@ConfigurationProperties(prefix = "app.poll")
public record PollProperties(String voteSalt) {

    public PollProperties {
        if (voteSalt == null || voteSalt.isBlank()) {
            voteSalt = "anticorruption-default-poll-salt";
        }
    }
}
