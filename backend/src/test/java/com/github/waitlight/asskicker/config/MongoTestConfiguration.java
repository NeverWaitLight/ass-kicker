package com.github.waitlight.asskicker.config;

import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.converter.ChannelConverterImpl;
import com.github.waitlight.asskicker.converter.ChannelPropertiesMapper;
import com.github.waitlight.asskicker.converter.TemplateConverterImpl;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.service.ChannelService;
import com.github.waitlight.asskicker.service.TemplateService;
import com.github.waitlight.asskicker.service.UserService;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheConfig;
import com.github.waitlight.asskicker.config.jackson.MongoJsonNodeConfig;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
@EnableReactiveMongoRepositories(basePackages = "com.github.waitlight.asskicker.repository")
@Import({
        JacksonAutoConfiguration.class,
        MongoJsonNodeConfig.class,
        ChannelPropertiesMapper.class,
        ChannelConverterImpl.class,
        ChannelRepository.class,
        ChannelService.class,
        ChannelFactory.class,
        ChannelManager.class,
        TemplateConverterImpl.class,
        TemplateRepository.class,
        TemplateService.class,
        UserRepository.class,
        UserService.class,
        CaffeineCacheConfig.class
})
public class MongoTestConfiguration {

    @Bean
    SnowflakeIdGenerator testSnowflakeIdGenerator() {
        return new SnowflakeIdGenerator(0, 0);
    }

    @Bean
    PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}