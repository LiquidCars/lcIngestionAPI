package net.liquidcars.ingestion.config.mongo;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "liquibase.mongodb", name = "enabled", havingValue = "true")
public class MongoConfig {

    @Value("${liquibase.mongodb.url}")
    private String url;

    @Value("${liquibase.mongodb.user}")
    private String user;

    @Value("${liquibase.mongodb.password}")
    private String password;

    @Value("${liquibase.mongodb.change-log}")
    private String changeLog;

    @Bean
    //@DependsOn("liquibase") // Ejecuta después de Liquibase de PostgreSQL
    public Liquibase mongoLiquibase() throws Exception {
        log.info("Initializing MongoDB Liquibase with changelog: {}", changeLog);
        log.info("MongoDB URL: {}", url);

        String changeLogPath = changeLog.replace("classpath:", "");

        ClassLoaderResourceAccessor classLoaderAccessor = new ClassLoaderResourceAccessor();

        MongoLiquibaseDatabase database = (MongoLiquibaseDatabase) DatabaseFactory
                .getInstance()
                .openDatabase(url, user, password, null, classLoaderAccessor);

        Liquibase liquibase = new Liquibase(changeLogPath, classLoaderAccessor, database);

        liquibase.update(new Contexts());

        log.info("MongoDB Liquibase migration completed successfully");
        return liquibase;
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoMappingContext context,
            MongoCustomConversions conversions) {

        MappingMongoConverter converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE,
                context
        );

        converter.setCustomConversions(conversions);

        // Delete field _class
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

        return converter;
    }
}