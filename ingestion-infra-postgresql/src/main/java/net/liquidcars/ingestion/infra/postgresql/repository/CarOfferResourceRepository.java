package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.CarOfferResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CarOfferResourceRepository extends JpaRepository<CarOfferResourceEntity, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByOfferId(UUID offerId);
}
