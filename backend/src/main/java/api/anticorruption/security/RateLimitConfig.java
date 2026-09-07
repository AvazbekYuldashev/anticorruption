package api.anticorruption.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * So'rov chegarasi qayerda hisoblanishini tanlaydi.
 *
 * <p>Standart holat - xotira: ishlab chiqish uchun Redis o'rnatish shart emas.
 * {@code app.rate-limit.store=redis} qo'yilsa hisob umumiy omborga ko'chadi.
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitStore rateLimitStore(RateLimitProperties properties,
                                         ObjectProvider<StringRedisTemplate> redisTemplate) {

        InMemoryRateLimitStore memory = new InMemoryRateLimitStore(properties.maxTrackedClients());

        if (properties.store() != RateLimitProperties.Store.REDIS) {
            return memory;
        }

        StringRedisTemplate template = redisTemplate.getIfAvailable();
        if (template == null) {
            log.warn("app.rate-limit.store=redis, lekin Redis sozlanmagan - "
                    + "chegara xotirada hisoblanadi (faqat shu nusxa uchun)");
            return memory;
        }

        log.info("So'rov chegarasi Redis da hisoblanadi - chegara barcha nusxalar uchun umumiy");
        return new RedisRateLimitStore(template, memory);
    }
}
