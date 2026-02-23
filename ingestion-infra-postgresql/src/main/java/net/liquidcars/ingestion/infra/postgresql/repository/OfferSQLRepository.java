package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.ExternalIdInfoProjection;
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

    @Query(value = """
        SELECT o.* FROM inv_ofr_offer o
        JOIN inv_ninof_inventoryoffers ni ON ni.ofr_co_id = o.ofr_co_id
        WHERE ni.nin_co_id = :inventoryId
          AND (:ownerRef IS NULL OR o.ofr_ownerref = :ownerRef)
          AND (:dealerRef IS NULL OR o.ofr_dealerref = :dealerRef)
          AND (:channelRef IS NULL OR o.ofr_channelref = :channelRef)
        LIMIT 1
        """, nativeQuery = true)
    Optional<OfferEntity> findByInventoryIdAndReferences(
            @Param("inventoryId") UUID inventoryId,
            @Param("ownerRef") String ownerRef,
            @Param("dealerRef") String dealerRef,
            @Param("channelRef") String channelRef
    );

    // --- RECURSOS ---
    @Modifying
    @Query(value = "DELETE FROM inv_cr_carresources WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId)", nativeQuery = true)
    void deleteResourcesByInventoryId(@Param("invId") UUID invId);

    @Modifying
    @Query(value = "DELETE FROM inv_cr_carresources WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId AND ofr_co_id NOT IN (:ids))", nativeQuery = true)
    void deleteResourcesByInventoryExcluding(@Param("invId") UUID invId, @Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = """
        DELETE FROM inv_cr_carresources WHERE ofr_co_id IN (
            SELECT ofr_co_id FROM inv_ofr_offer
            WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId)
            AND (ofr_ownerref IN (:refs) OR ofr_dealerref IN (:refs) OR ofr_channelref IN (:refs))
        )
        """, nativeQuery = true)
    void deleteResourcesByReferences(@Param("invId") UUID invId, @Param("refs") List<String> refs);

    // --- TINY LOCATORS (FK que causaba el error) ---
    @Modifying
    @Query(value = "DELETE FROM or_poc_preorders_cart WHERE tl_co_id IN (SELECT tl_co_id FROM tl_tl_tiny_locators WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId))", nativeQuery = true)
    void deletePreordersCartByInventoryId(@Param("invId") UUID invId);

    @Modifying
    @Query(value = "DELETE FROM tl_tl_tiny_locators WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId)", nativeQuery = true)
    void deleteTinyLocatorsByInventoryId(@Param("invId") UUID invId);

    @Modifying
    @Query(value = "DELETE FROM or_poc_preorders_cart WHERE tl_co_id IN (SELECT tl_co_id FROM tl_tl_tiny_locators WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ofr_offer WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId) AND (ofr_ownerref IN (:refs) OR ofr_dealerref IN (:refs) OR ofr_channelref IN (:refs))))", nativeQuery = true)
    void deletePreordersCartByReferences(@Param("invId") UUID invId, @Param("refs") List<String> refs);

    @Modifying
    @Query(value = "DELETE FROM tl_tl_tiny_locators WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ofr_offer WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId) AND (ofr_ownerref IN (:refs) OR ofr_dealerref IN (:refs) OR ofr_channelref IN (:refs)))", nativeQuery = true)
    void deleteTinyLocatorsByReferences(@Param("invId") UUID invId, @Param("refs") List<String> refs);

    @Modifying
    @Query(value = "DELETE FROM or_poc_preorders_cart WHERE tl_co_id IN (SELECT tl_co_id FROM tl_tl_tiny_locators WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId) AND ofr_co_id NOT IN (:ids))", nativeQuery = true)
    void deletePreordersCartByInventoryExcluding(@Param("invId") UUID invId, @Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM tl_tl_tiny_locators WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId) AND ofr_co_id NOT IN (:ids)", nativeQuery = true)
    void deleteTinyLocatorsByInventoryExcluding(@Param("invId") UUID invId, @Param("ids") List<UUID> ids);

    // --- CARLOAN PREVIEW ---
    @Modifying
    @Query(value = "DELETE FROM cache_carloan_preview WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId)", nativeQuery = true)
    void deleteCarloanPreviewByInventoryId(@Param("invId") UUID invId);

    @Modifying
    @Query(value = "DELETE FROM cache_carloan_preview WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId AND ofr_co_id NOT IN (:ids))", nativeQuery = true)
    void deleteCarloanPreviewByInventoryExcluding(@Param("invId") UUID invId, @Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM cache_carloan_preview WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ofr_offer WHERE ofr_co_id IN (SELECT ofr_co_id FROM inv_ninof_inventoryoffers WHERE nin_co_id = :invId) AND (ofr_ownerref IN (:refs) OR ofr_dealerref IN (:refs) OR ofr_channelref IN (:refs)))", nativeQuery = true)
    void deleteCarloanPreviewByReferences(@Param("invId") UUID invId, @Param("refs") List<String> refs);

    // --- OFERTA PRINCIPAL (CTE corregidas) ---
    @Modifying
    @Query(value = """
        WITH target_offers AS (
            SELECT o.ofr_co_id, o.obj_co_jsoncaroffer_id
            FROM inv_ofr_offer o
            JOIN inv_ninof_inventoryoffers ni ON ni.ofr_co_id = o.ofr_co_id
            WHERE ni.nin_co_id = :invId
        ),
        del_join AS (
            DELETE FROM inv_ninof_inventoryoffers WHERE ofr_co_id IN (SELECT ofr_co_id FROM target_offers)
        ),
        del_offers AS (
            DELETE FROM inv_ofr_offer WHERE ofr_co_id IN (SELECT ofr_co_id FROM target_offers)
        )
        DELETE FROM op_jobj_json_object
        WHERE obj_co_id IN (SELECT obj_co_jsoncaroffer_id FROM target_offers WHERE obj_co_jsoncaroffer_id IS NOT NULL)
        """, nativeQuery = true)
    int deleteMainOfferData(@Param("invId") UUID invId);

    @Modifying
    @Query(value = """
        WITH target_offers AS (
            SELECT o.ofr_co_id, o.obj_co_jsoncaroffer_id
            FROM inv_ofr_offer o
            JOIN inv_ninof_inventoryoffers ni ON ni.ofr_co_id = o.ofr_co_id
            WHERE ni.nin_co_id = :invId
            AND o.ofr_co_id NOT IN (:ids)
        ),
        del_join AS (
            DELETE FROM inv_ninof_inventoryoffers WHERE ofr_co_id IN (SELECT ofr_co_id FROM target_offers)
        ),
        del_offers AS (
            DELETE FROM inv_ofr_offer WHERE ofr_co_id IN (SELECT ofr_co_id FROM target_offers)
        )
        DELETE FROM op_jobj_json_object
        WHERE obj_co_id IN (SELECT obj_co_jsoncaroffer_id FROM target_offers WHERE obj_co_jsoncaroffer_id IS NOT NULL)
        """, nativeQuery = true)
    int deleteMainOfferDataExcluding(@Param("invId") UUID invId, @Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = """
        WITH target_offers AS (
            SELECT o.ofr_co_id, o.obj_co_jsoncaroffer_id
            FROM inv_ofr_offer o
            JOIN inv_ninof_inventoryoffers ni ON ni.ofr_co_id = o.ofr_co_id
            WHERE ni.nin_co_id = :invId
            AND (o.ofr_ownerref IN (:refs) OR o.ofr_dealerref IN (:refs) OR o.ofr_channelref IN (:refs))
        ),
        del_join AS (
            DELETE FROM inv_ninof_inventoryoffers WHERE ofr_co_id IN (SELECT ofr_co_id FROM target_offers)
        ),
        del_offers AS (
            DELETE FROM inv_ofr_offer WHERE ofr_co_id IN (SELECT ofr_co_id FROM target_offers)
        )
        DELETE FROM op_jobj_json_object
        WHERE obj_co_id IN (SELECT obj_co_jsoncaroffer_id FROM target_offers WHERE obj_co_jsoncaroffer_id IS NOT NULL)
        """, nativeQuery = true)
    int deleteMainOfferDataByRefs(@Param("invId") UUID invId, @Param("refs") List<String> refs);

    // CarOfferResourceRepository
    @Modifying
    @Query("DELETE FROM CarOfferResourceEntity r WHERE r.offer.id IN :offerIds")
    void deleteByOfferIdIn(@Param("offerIds") List<UUID> offerIds);

    // CarInstanceEquipmentEntityRepository
    @Modifying
    @Query("DELETE FROM CarInstanceEquipmentEntity e WHERE e.vehicleInstance.id IN :vehicleInstanceIds")
    void deleteByVehicleInstanceIdIn(@Param("vehicleInstanceIds") List<Long> vehicleInstanceIds);

    @Query(value = """
    SELECT o.* FROM inv_ofr_offer o
    JOIN inv_ninof_inventoryoffers ni ON ni.ofr_co_id = o.ofr_co_id
    WHERE ni.nin_co_id = :inventoryId
    AND o.ofr_ownerref IN (:ownerRefs)
    """, nativeQuery = true)
    List<OfferEntity> findByInventoryIdAndOwnerRefs(
            @Param("inventoryId") UUID inventoryId,
            @Param("ownerRefs") List<String> ownerRefs
    );

    @Query(value = """
    SELECT DISTINCT o.ofr_co_id
    FROM inv_ofr_offer o
    JOIN inv_ninof_inventoryoffers ni ON o.ofr_co_id = ni.ofr_co_id
    JOIN op_bok_bookings b ON o.ofr_co_id = b.ofr_co_id
    WHERE ni.nin_co_id = :invId
    AND b.bs_co_id = 'active'
    AND b.bok_bo_enabled = true
    """, nativeQuery = true)
    List<UUID> findActiveBookedOfferIds(@Param("invId") UUID inventoryId);

    @Query(value = """
    SELECT o.ofr_ownerref, o.ofr_dealerref, o.ofr_channelref
    FROM inv_ofr_offer o
    WHERE o.ofr_co_id IN (:ids)
    """, nativeQuery = true)
    List<ExternalIdInfoProjection> findExternalRefsByOfferIds(@Param("ids") List<UUID> ids);
}