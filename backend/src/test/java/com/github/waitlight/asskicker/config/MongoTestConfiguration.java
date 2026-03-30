package com.github.waitlight.asskicker.config;

import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.converter.ChannelProviderConverterImpl;
import com.github.waitlight.asskicker.converter.ChannelProviderPropertiesMapper;
import com.github.waitlight.asskicker.converter.MessageTemplateConverterImpl;
import com.github.waitlight.asskicker.service.impl.ChannelProviderServiceImpl;
import com.github.waitlight.asskicker.service.impl.MessageTemplateServiceImpl;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * 嵌入式 Mongo 测试切片：装配 MapStruct 转换、JsonNode 与 Document
 * 互转、以及 ChannelProvider / MessageTemplate 等服务实现。
 * 使用 Flapdoodle 启动本机 mongod 进程（见
 * {@code de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration}），
 * 而非 Fongo：Fongo 面向旧版同步 Java 驱动，无法与 {@code ReactiveMongoRepository} 共用。
 * <p>
 * 若出现 {@code ClassNotFoundException}（例如仅类名 {@code ChannelProviderEntity} 无包名）或
 * MapStruct 实现类无法内省，多为 {@code target} 下过时字节码，请先执行 {@code mvn clean test}。
 */
@TestConfiguration
@EnableReactiveMongoRepositories(basePackages = "com.github.waitlight.asskicker.repository")
@Import({
        JacksonAutoConfiguration.class,
        MongoJsonNodeConfig.class,
        ChannelProviderPropertiesMapper.class,
        ChannelProviderConverterImpl.class,
        ChannelProviderServiceImpl.class,
        ChannelFactory.class,
        ChannelManager.class,
        MessageTemplateConverterImpl.class,
        MessageTemplateServiceImpl.class
})
public class MongoTestConfiguration {

    @Bean
    SnowflakeIdGenerator testSnowflakeIdGenerator() {
        return new SnowflakeIdGenerator(0, 0);
    }
}
