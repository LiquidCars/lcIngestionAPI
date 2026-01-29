package net.liquidcars.ingestion.infra.mongodb.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;

import java.util.Optional;

@Repository
public interface OfferNoSqlRepository extends MongoRepository<OfferNoSQLEntity, String> {

}