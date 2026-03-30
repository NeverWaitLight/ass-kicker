package com.github.waitlight.asskicker;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;

/**
 * 仅用于 ChannelProvider 嵌入式 Mongo 测试，避免 {@code @DataMongoTest} 向上解析到
 * {@link com.github.waitlight.asskicker.AssKickerApplication} 拉起完整应用。
 * 使用完整 {@link EnableAutoConfiguration} 以保证 Flapdoodle 嵌入式 Mongo 与
 * {@code MongoReactive*} 的装配顺序与生产一致；
 * {@link ImportAutoConfiguration} 手工列表会破坏
 * {@code EmbeddedMongoAutoConfiguration} 与
 * {@code MongoReactiveAutoConfiguration} 的先后关系，
 * 导致 {@code MongoReactiveDataAutoConfiguration} 的
 * {@code @ConditionalOnBean(MongoClient)} 不满足。
 */
@SpringBootConfiguration
@Import(ChannelObjectMapperConfig.class)
@EnableAutoConfiguration(exclude = {
        KafkaAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class,
        WebFluxAutoConfiguration.class,
        ReactiveWebServerFactoryAutoConfiguration.class,
        MailSenderAutoConfiguration.class
})
public class AssKickerTestApplication {

    @Bean
    WebClient webClient() {
        return WebClient.create();
    }
}
