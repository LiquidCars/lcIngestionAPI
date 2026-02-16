package net.liquidcars.ingestion.infra.mongodb.repository;

import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DraftOfferNoSqlRepository extends MongoRepository<DraftOfferNoSQLEntity, UUID> {


    /**
     * Deletes offers of a job report.
     * Changed return type to long to detect if deletion was effective.
     * * @param ingestionReportId job id
     * @return count of deleted documents
     */
    long deleteByIngestionReportId(UUID ingestionReportId);

    /**
     * Find offers of a job report.
     * * @param ingestionReportId job id
     * @return List of documents
     */
    List<DraftOfferNoSQLEntity> findByJobIdentifier(UUID ingestionReportId);

    /**
     * Counts offers of a job.
     * Used to detect race conditions before cleanup.
     * @param jobIdentifier job id
     * @return count of documents with the given jobIdentifier
     */
    long countByJobIdentifier(UUID jobIdentifier);

    /**
     * Counts offers of a initiated job.
     * Used to detect race conditions.
     * @param ingestionReportId report job id
     * @return count of documents with the given ingestionReportId
     */
    long countByIngestionReportId(UUID ingestionReportId);

    /**
     * Deletes offers where batchStatus is not 'COMPLETED'
     * and updatedAt is older than the provided threshold.
     * * @param threshold time limit
     * @return count of deleted documents
     */
    @Query(value = "{ 'batchStatus': { $ne: 'COMPLETED' }, 'updatedAt': { $lt: ?0 } }", delete = true)
    long deleteByBatchStatusNotCompletedAndUpdatedAtBefore(Instant threshold);
}