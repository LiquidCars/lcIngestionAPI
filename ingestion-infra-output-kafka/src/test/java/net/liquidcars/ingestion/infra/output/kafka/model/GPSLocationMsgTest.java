package net.liquidcars.ingestion.infra.output.kafka.model;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
public class GPSLocationMsgTest {

    @Test
    @DisplayName("Debe calcular la distancia correctamente entre dos puntos conocidos (Madrid y Barcelona)")
    void shouldCalculateDistanceBetweenTwoPoints() {
        GPSLocationMsg madrid = TestDataFactory.createGPSLocationMsg("Madrid", 40.4168, -3.7038);

        GPSLocationMsg barcelona = TestDataFactory.createGPSLocationMsg("Barcelona", 41.3851, 2.1734);

        double distance = madrid.distanceTo(barcelona);

        assertThat(distance).isCloseTo(505000.0, within(5000.0));
    }

    @Test
    @DisplayName("La distancia a sí mismo debe ser cero")
    void distanceToSelfShouldBeZero() {
        GPSLocationMsg point = TestDataFactory.createGPSLocationMsg("Test", 10.0, 10.0);

        assertThat(point.distanceTo(point)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Debe verificar que el método toString funciona correctamente")
    void toStringShouldFormatCorrectly() {
        GPSLocationMsg point = TestDataFactory.createGPSLocationMsg("Test", 1.23, 4.56);

        assertThat(point.toString()).isEqualTo("Test (1.23, 4.56)");
    }

    @Test
    @DisplayName("Debe verificar que Lombok genera correctamente getters y setters")
    void lombokDataShouldWork() {
        GPSLocationMsg location = TestDataFactory.createGPSLocationMsg("Test", 48.8566, 2.3522);

        assertThat(location.getName()).isEqualTo("Test");
        assertThat(location.getLatitude()).isEqualTo(48.8566);
        assertThat(location.getLongitude()).isEqualTo(2.3522);
    }
}
