package api.anticorruption.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Birinchi ishga tushirishda yaratiladigan moderator hisobi
 * (application.properties dagi "app.moderator.*").
 *
 * <p>Administratordan farqli o'laroq bu hisob majburiy emas: email yoki parol
 * berilmasa moderator umuman yaratilmaydi va tizim baribir ishlaydi -
 * administrator keyin uni admin panelidan o'zi qo'shadi.
 */
@ConfigurationProperties(prefix = "app.moderator")
public record ModeratorProperties(
        String email,
        String password,
        String fullName
) {
    public ModeratorProperties {
        if (fullName == null || fullName.isBlank()) {
            fullName = "Moderator";
        }
    }

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
