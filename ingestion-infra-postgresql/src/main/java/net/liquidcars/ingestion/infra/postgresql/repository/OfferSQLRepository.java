package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface OfferSQLRepository extends JpaRepository<OfferEntity, String> {

    Optional<OfferEntity> findByExternalId(String externalId);

    @Modifying
    @Query("UPDATE OfferEntity o SET o.batchStatus = :status WHERE o.jobIdentifier = :jobId")
    void updateBatchStatusByJobIdentifier(@Param("jobId") String jobId, @Param("status") String status);

    @Modifying
    void deleteByJobIdentifier(String jobId);

    @Modifying
    @Query("DELETE FROM OfferEntity o WHERE o.batchStatus != 'COMPLETED' AND o.updatedAt < :threshold")
    void deleteObsoleteOffers(@Param("threshold") OffsetDateTime threshold);
}
