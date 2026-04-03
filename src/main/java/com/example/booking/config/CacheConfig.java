package com.example.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.annotation.EnableCaching;

// For Redis
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;

// For Ehcache (JSR-107)
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.URISyntaxException;

//@Configuration
//@EnableCaching
public class CacheConfig {

    // Example Ehcache (JCache) CacheManager bean:
    // Uncomment to use Ehcache (ensure ehcache.xml is on classpath)
    /*@Bean
    public CacheManager cacheManager() throws URISyntaxException {
        CachingProvider provider = Caching.getCachingProvider();
        return provider.getCacheManager(
            getClass().getResource("/ehcache.xml").toURI(),
            getClass().getClassLoader()
        );
    }*/

    // Example Redis CacheManager bean:
    // Uncomment to use Redis cache
    /*
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory).build();
    }
    */
}
