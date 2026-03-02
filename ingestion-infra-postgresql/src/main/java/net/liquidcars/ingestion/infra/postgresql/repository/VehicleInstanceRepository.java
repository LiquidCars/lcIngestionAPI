package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.VehicleInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleInstanceRepository extends JpaRepository<VehicleInstanceEntity, Long> {

    Optional<VehicleInstanceEntity> findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(String plate, String chassisNumber);
}
