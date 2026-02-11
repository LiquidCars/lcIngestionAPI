package net.liquidcars.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Ingestion API microservice.
 * This application handles vehicle offer ingestion from multiple sources
 * using Spring Batch, Kafka, MongoDB, and PostgreSQL.
 */
@SpringBootApplication
@EnableScheduling
public class IngestionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionApiApplication.class, args);
    }
}
