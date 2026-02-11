package net.liquidcars.ingestion.infra.mongodb.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferNoSqlRepository extends MongoRepository<OfferNoSQLEntity, String> {

    /**
     * Updates batch status in offers of a job
     * @param jobIdentifier job id
     * @param batchStatus batch job status
     */
    @Query("{ 'jobIdentifier' : ?0 }")
    @Update("{ '$set' : { 'batchStatus' : ?1 } }")
    void updateBatchStatusByJobIdentifier(String jobIdentifier, String batchStatus);

    /**
     * Deletes offers of a job.
     * Changed return type to long to detect if deletion was effective.
     * * @param jobIdentifier job id
     * @return count of deleted documents
     */
    long deleteByJobIdentifier(String jobIdentifier);

    /**
     * Counts offers of a job.
     * Used to detect race conditions before cleanup.
     * @param jobIdentifier job id
     * @return count of documents with the given jobIdentifier
     */
    long countByJobIdentifier(UUID jobIdentifier);

    /**
     * Deletes offers where batchStatus is not 'COMPLETED'
     * and updatedAt is older than the provided threshold.
     * * @param threshold time limit
     * @return count of deleted documents
     */
    @Query(value = "{ 'batchStatus': { $ne: 'COMPLETED' }, 'updatedAt': { $lt: ?0 } }", delete = true)
    long deleteByBatchStatusNotCompletedAndUpdatedAtBefore(Instant threshold);
}