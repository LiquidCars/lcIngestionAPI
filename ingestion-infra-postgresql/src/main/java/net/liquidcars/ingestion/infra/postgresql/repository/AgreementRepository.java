package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.AgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<AgreementEntity, UUID> {

    @Query("""
           SELECT DISTINCT a FROM AgreementEntity a 
           JOIN FETCH a.vehicleSeller vs
           LEFT JOIN FETCH a.channels ch
           WHERE a.id IN (
               SELECT ia.id.agreementId 
               FROM InventoryAgreementEntity ia 
               WHERE ia.id.inventoryId = :inventoryId
                 AND ia.enabled = true
           )
           AND a.enabled = true
           """)
    List<AgreementEntity> findByInventoryId(@Param("inventoryId") UUID inventoryId);
}