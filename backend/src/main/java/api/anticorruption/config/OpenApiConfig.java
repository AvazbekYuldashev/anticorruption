package api.anticorruption.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI sozlamalari.
 * Hujjatlar: <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI anticorruptionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Korrupsiyaga qarshi kurash portali API")
                        .version("v1")
                        .description("""
                                Fuqarolar korrupsiya holatlari haqida murojaat yuboradigan portal API si.

                                Autentifikatsiya: /api/v1/auth/login orqali token oling, so'ng
                                yuqoridagi "Authorize" tugmasi orqali kiriting.

                                Ochiq yo'llar (token talab qilinmaydi): murojaat yuborish,
                                kuzatuv kodi bo'yicha holatni tekshirish, ma'lumotnomalar va ommaviy statistika.""")
                        .contact(new Contact().name("Anticorruption API")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Login javobidagi accessToken qiymatini kiriting")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
