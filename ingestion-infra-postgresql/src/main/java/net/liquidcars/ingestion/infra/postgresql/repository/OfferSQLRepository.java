package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferSQLRepository extends JpaRepository<OfferEntity, UUID> {

    Optional<OfferEntity> findByHash(Integer hash);

    // For REPLACEMENT logic
    // 1. FULL REPLACEMENT: Delete all offers linked to this inventory
    @Modifying
    @Query(value = """
        WITH deleted_rows AS (
            DELETE FROM inv_ofr_offer 
            WHERE ofr_co_id IN (
                SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :inventoryId
            )
            RETURNING ofr_co_id
        )
        DELETE FROM inv_ninof_inventoryoffers WHERE ofr_co_id IN (SELECT ofr_co_id FROM deleted_rows);
        SELECT count(*) FROM deleted_rows;
    """, nativeQuery = true)
    long deleteByInventoryId(@Param("inventoryId") UUID inventoryId);

    // 2. DELTA REPLACEMENT: Delete offers NOT present in the current job
    @Modifying
    @Query(value = """
        WITH deleted_rows AS (
            DELETE FROM inv_ofr_offer 
            WHERE ofr_co_id IN (
                SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :inventoryId
            )
            AND ofr_co_id NOT IN (:ids)
            RETURNING ofr_co_id
        )
        DELETE FROM inv_ninof_inventoryoffers WHERE ofr_co_id IN (SELECT ofr_co_id FROM deleted_rows);
        SELECT count(*) FROM deleted_rows;
    """, nativeQuery = true)
    long deleteByInventoryIdAndIdNotIn(@Param("inventoryId") UUID inventoryId, @Param("ids") List<UUID> ids);

    // 3. EXPLICIT DELETIONS: Delete specific references from JSON
    @Modifying
    @Query(value = """
        WITH deleted_rows AS (
            DELETE FROM inv_ofr_offer 
            WHERE ofr_co_id IN (
                SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :inventoryId
            )
            AND (
                ofr_ownerref IN (:references) 
                OR ofr_dealerref IN (:references) 
                OR ofr_channelref IN (:references)
            )
            RETURNING ofr_co_id
        )
        DELETE FROM inv_ninof_inventoryoffers WHERE ofr_co_id IN (SELECT ofr_co_id FROM deleted_rows);
        SELECT count(*) FROM deleted_rows;
    """, nativeQuery = true)
    long deleteByInventoryIdAndReferencesIn(
            @Param("inventoryId") UUID inventoryId,
            @Param("references") List<String> references
    );
}
