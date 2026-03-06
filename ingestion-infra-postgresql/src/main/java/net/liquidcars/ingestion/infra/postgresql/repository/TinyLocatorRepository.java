package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.TinyLocatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TinyLocatorRepository extends JpaRepository<TinyLocatorEntity, String> {

    @Modifying
    @Query("DELETE FROM TinyLocatorEntity t WHERE t.offerId IN :offerIds")
    void deleteByOfferIdIn(@Param("offerIds") List<UUID> offerIds);
}