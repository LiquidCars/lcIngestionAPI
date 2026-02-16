package net.liquidcars.ingestion.infra.mongodb.repository;

import net.liquidcars.ingestion.infra.mongodb.entity.VehicleOfferNoSQLEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleOfferNoSqlRepository extends MongoRepository<VehicleOfferNoSQLEntity, UUID> {
    // For REPLACEMENT: Delete offers that are NOT in the new batch
    void deleteByInventoryIdAndIdNotIn(UUID inventoryId, List<UUID> idsToKeep);
}
