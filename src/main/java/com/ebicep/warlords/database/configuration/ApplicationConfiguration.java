package com.ebicep.warlords.database.configuration;

import com.ebicep.warlords.database.DatabaseManager;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@ComponentScan(basePackages = "com.ebicep.warlords.database")
@EnableMongoRepositories({"com.ebicep.warlords.database.repositories"})
public class ApplicationConfiguration extends AbstractMongoClientConfiguration {

    public static String key;

    @Bean
    public MongoTemplate mongoTemplate() {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient(), getDatabaseName());
        MappingMongoConverter mongoMapping = (MappingMongoConverter) mongoTemplate.getConverter();
        mongoMapping.setCustomConversions(customConversions()); // tell mongodb to use the custom converters
        mongoMapping.afterPropertiesSet();
        return mongoTemplate;
    }

    @Nonnull
    @Bean
    @Override
    public MongoClient mongoClient() {
        System.out.println("Getting mongoClient");
        MongoClientSettings mongoClientSettings = MongoClientSettings
                .builder()
                .applyConnectionString(new ConnectionString(key))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyToSocketSettings(builder -> builder.connectTimeout(20, TimeUnit.SECONDS)
                                                         .readTimeout(20, TimeUnit.SECONDS))
                .build();
        MongoClient mongoClient = MongoClients.create(mongoClientSettings);
        DatabaseManager.mongoClient = mongoClient;
        DatabaseManager.warlordsDatabase = mongoClient.getDatabase("Warlords");

        return mongoClient;
    }

    @Nonnull
    @Override
    protected String getDatabaseName() {
        return "Warlords";
    }

    @Override
    public boolean autoIndexCreation() {
        return true;
    }

    @Override
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new StringToSpendableConverter()
        ));
    }
}
