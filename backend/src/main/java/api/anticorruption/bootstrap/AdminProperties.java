package api.anticorruption.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Birinchi ishga tushirishda yaratiladigan administrator hisobi
 * (application.properties dagi "app.admin.*").
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        String email,
        String password,
        String fullName
) {
    public AdminProperties {
        if (fullName == null || fullName.isBlank()) {
            fullName = "Bosh administrator";
        }
    }

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
