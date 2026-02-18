package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VehicleInstanceTest {

    @Test
    @DisplayName("Cobertura total: Setters, Métodos Fluídos, Listas y Getters")
    void fullCoverageTest() {
        VehicleInstance instance = new VehicleInstance();
        VehicleModel model = new VehicleModel();
        KeyValue color = new KeyValue().key("RED").value("Rojo");
        KeyValue state = new KeyValue().key("NEW").value("Nuevo");
        List<CarInstanceEquipment> equipmentList = new ArrayList<>();
        CarInstanceEquipment item = new CarInstanceEquipment();
        equipmentList.add(item);

        // 1. PROBAMOS MÉTODOS FLUÍDOS (Incluyendo addEquipmentsItem)
        instance.id(1L)
                .vehicleModel(model)
                .plate("1234ABC")
                .color(color)
                .mileage(50000)
                .registrationYear(2022)
                .registrationMonth(5)
                .isMetallicPaint(true)
                .chassisNumber("VIN123456789")
                .equipments(equipmentList) // Reset para probar addEquipmentsItem
                .state(state);

        // 2. PROBAMOS SETTERS ESTÁNDAR
        instance.setId(100L);
        instance.setVehicleModel(model);
        instance.setPlate("1234BBB");
        instance.setColor(color);
        instance.setMileage(15000);
        instance.setRegistrationYear(2023);
        instance.setRegistrationMonth(12);
        instance.setIsMetallicPaint(true);
        instance.setChassisNumber("CH-999");
        instance.setEquipments(equipmentList);
        instance.setState(state);

        // 3. VERIFICACIÓN DE GETTERS
        assertThat(instance.getId()).isEqualTo(100L);
        assertThat(instance.getVehicleModel()).isEqualTo(model);
        assertThat(instance.getPlate()).isEqualTo("1234BBB");
        assertThat(instance.getColor()).isEqualTo(color);
        assertThat(instance.getMileage()).isEqualTo(15000);
        assertThat(instance.getRegistrationYear()).isEqualTo(2023);
        assertThat(instance.getRegistrationMonth()).isEqualTo(12);
        assertThat(instance.getIsMetallicPaint()).isTrue();
        assertThat(instance.getChassisNumber()).isEqualTo("CH-999");
        assertThat(instance.getEquipments()).isEqualTo(equipmentList);
        assertThat(instance.getState()).isEqualTo(state);
    }

    @Test
    @DisplayName("Cobertura de lógica de lista (addEquipmentsItem con null)")
    void testListNullSafety() {
        VehicleInstance instance = new VehicleInstance();
        // Forzamos que la lista sea null para entrar en el 'if (this.equipments == null)'
        instance.setEquipments(null);

        instance.addEquipmentsItem(new CarInstanceEquipment());

        assertThat(instance.getEquipments()).isNotNull().hasSize(1);
    }

    @Test
    @DisplayName("Cobertura total de equals, hashCode y toString")
    void testLogicMethods() {
        VehicleInstance v1 = new VehicleInstance().id(10L).plate("TEST");
        VehicleInstance v2 = new VehicleInstance().id(10L).plate("TEST");
        VehicleInstance v3 = new VehicleInstance().id(20L).plate("DIFF");

        // Equals: Identidad (this == o) -> Cubre return true
        assertThat(v1).isEqualTo(v1);

        // Equals: Mismos valores
        assertThat(v1).isEqualTo(v2);

        // Equals: Casos false (Nulo, Clase distinta, Valores distintos)
        assertThat(v1).isNotEqualTo(null);
        assertThat(v1).isNotEqualTo(new Object());
        assertThat(v1).isNotEqualTo(v3);

        // HashCode
        assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        assertThat(v1.hashCode()).isNotEqualTo(v3.hashCode());

        // ToString (Cubre toIndentedString con valores y con nulls)
        assertThat(v1.toString()).contains("id: 10");
        assertThat(new VehicleInstance().toString()).contains("plate: null");
    }
}
