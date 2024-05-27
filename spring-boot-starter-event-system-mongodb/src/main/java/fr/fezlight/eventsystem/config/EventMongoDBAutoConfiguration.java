package fr.fezlight.eventsystem.config;

import com.mongodb.client.MongoClient;
import fr.fezlight.eventsystem.config.properties.EventMongodbProperties;
import fr.fezlight.eventsystem.converters.ClassConverters;
import fr.fezlight.eventsystem.converters.ZonedDateTimeConverters;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.ArrayList;
import java.util.List;

@ConditionalOnProperty(
        value = "events.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(EventMongodbProperties.class)
@AutoConfiguration
@AutoConfigureAfter(EventAutoConfiguration.class)
@Import({EventSchedulingTaskConfig.class, EventSchedulerLockConfig.class})
public class EventMongoDBAutoConfiguration {
    @Bean
    @ConditionalOnProperty(value = "events.scheduled-task.lock-enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public LockProvider lockProvider(MongoClient mongo, EventMongodbProperties eventMongodbProperties) {
        return new MongoLockProvider(mongo.getDatabase(eventMongodbProperties.getDatabaseName()));
    }

    @Bean
    @ConditionalOnMissingBean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory mongoDbFactory, MongoMappingContext mongoMappingContext) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        converter.setCustomConversions(customConversions());
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new ZonedDateTimeConverters.ZonedDateTimeReadConverter());
        converters.add(new ZonedDateTimeConverters.ZonedDateTimeWriteConverter());
        converters.add(new ClassConverters.ClassReadConverter());
        converters.add(new ClassConverters.ClassWriteConverter());
        return new MongoCustomConversions(converters);
    }
}
