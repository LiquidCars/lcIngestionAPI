package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.VehicleModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleModelSQLRepository extends JpaRepository<VehicleModelEntity, Long> {

    Optional<VehicleModelEntity> findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(String brand, String model, String version);
}
