package net.liquidcars.ingestion.infra.mongodb.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OfferNoSqlRepository extends MongoRepository<OfferNoSQLEntity, String> {

    Optional<OfferNoSQLEntity> findByExternalId(String externalId);

    /**
     * Updates batch status in offers of a job
     * @param jobIdentifier job id
     * @param batchStatus batch job status
     */
    @Query("{ 'jobIdentifier' : ?0 }")
    @Update("{ '$set' : { 'batchStatus' : ?1 } }")
    void updateBatchStatusByJobIdentifier(String jobIdentifier, String batchStatus);

    /**
     * Deletes offers of a job
     * @param jobIdentifier job id
     */
    void deleteByJobIdentifier(String jobIdentifier);

    /**
     * Deletes offers where batchStatus is not 'COMPLETED'
     * and updatedAt is older than the provided threshold.
     */
    @Query(value = "{ 'batchStatus': { $ne: 'COMPLETED' }, 'updatedAt': { $lt: ?0 } }", delete = true)
    void deleteByBatchStatusNotCompletedAndUpdatedAtBefore(Instant threshold);
}