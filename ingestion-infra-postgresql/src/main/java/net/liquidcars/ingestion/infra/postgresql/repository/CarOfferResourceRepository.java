package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.CarOfferResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface CarOfferResourceRepository extends JpaRepository<CarOfferResourceEntity, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByOfferId(UUID offerId);
}
