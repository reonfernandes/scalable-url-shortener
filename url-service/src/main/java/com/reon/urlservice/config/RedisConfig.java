package com.reon.urlservice.config;

import tools.jackson.databind.DeserializationFeature;
import com.reon.urlservice.dto.CachedUrlDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CachedUrlDTO> urlCachedRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, CachedUrlDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        JacksonJsonRedisSerializer<CachedUrlDTO> valueSerializer = new JacksonJsonRedisSerializer<>(jsonMapper(), CachedUrlDTO.class);
        template.setValueSerializer(valueSerializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
