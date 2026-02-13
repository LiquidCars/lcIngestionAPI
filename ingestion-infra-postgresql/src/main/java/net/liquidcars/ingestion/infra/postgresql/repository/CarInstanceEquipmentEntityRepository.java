package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.CarInstanceEquipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarInstanceEquipmentEntityRepository extends JpaRepository<CarInstanceEquipmentEntity, Integer> {
    void deleteByVehicleInstanceId(Long vehicleInstanceId);
}
