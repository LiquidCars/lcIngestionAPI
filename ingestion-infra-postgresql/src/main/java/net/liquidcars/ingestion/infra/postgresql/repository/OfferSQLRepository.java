package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface OfferSQLRepository extends JpaRepository<OfferEntity, UUID> {

        /**
         * Updates the batch status for all offers associated with a specific Job.
         * @param jobId the unique identifier of the batch execution.
         * @param status the new status to apply (e.g., 'COMPLETED', 'FAILED').
         * @return the number of updated records.
         */
        @Modifying
        @Query("UPDATE OfferEntity o SET o.batchStatus = :status WHERE o.jobIdentifier = :jobId")
        int updateBatchStatusByJobIdentifier(@Param("jobId") String jobId, @Param("status") String status);

        /**
         * Deletes all offers linked to a specific Job execution.
         * Used mainly for cleanup operations after a failed ingestion.
         * @param jobId the unique identifier of the batch execution.
         * @return the number of deleted records.
         */
        @Modifying
        int deleteByJobIdentifier(String jobId);

        /**
         * Count all offers linked to a specific Job execution.
         * @return the number of records.
         */
        int countByJobIdentifier(String jobIdentifier);

        /**
         * Removes offers that were not successfully completed and exceed the age threshold.
         * @param threshold the date and time limit for obsolescence.
         * @return the number of purged records.
         */
        @Modifying
        @Query("DELETE FROM OfferEntity o WHERE o.batchStatus != 'COMPLETED' AND o.updatedAt < :threshold")
        int deleteObsoleteOffers(@Param("threshold") OffsetDateTime threshold);
}
