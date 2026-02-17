package net.liquidcars.ingestion.application.service.parser.model.JSON;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
public class GPSLocationJSONModelTest {

    @Test
    @DisplayName("Debe calcular la distancia correctamente entre dos puntos conocidos (Madrid y Barcelona)")
    void shouldCalculateDistanceBetweenTwoPoints() {
        GPSLocationJSONModel madrid = TestDataFactory.createGPSLocationJSONModel("Madrid", 40.4168, -3.7038);

        GPSLocationJSONModel barcelona = TestDataFactory.createGPSLocationJSONModel("Barcelona", 41.3851, 2.1734);

        double distance = madrid.distanceTo(barcelona);

        assertThat(distance).isCloseTo(505000.0, within(5000.0));
    }

    @Test
    @DisplayName("La distancia a sí mismo debe ser cero")
    void distanceToSelfShouldBeZero() {
        GPSLocationJSONModel point = TestDataFactory.createGPSLocationJSONModel("Test", 10.0, 10.0);

        assertThat(point.distanceTo(point)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Debe verificar que el método toString funciona correctamente")
    void toStringShouldFormatCorrectly() {
        GPSLocationJSONModel point = TestDataFactory.createGPSLocationJSONModel("Test", 1.23, 4.56);

        assertThat(point.toString()).isEqualTo("Test (1.23, 4.56)");
    }

    @Test
    @DisplayName("Debe verificar que Lombok genera correctamente getters y setters")
    void lombokDataShouldWork() {
        GPSLocationJSONModel location = TestDataFactory.createGPSLocationJSONModel("Test", 48.8566, 2.3522);

        assertThat(location.getName()).isEqualTo("Test");
        assertThat(location.getLatitude()).isEqualTo(48.8566);
        assertThat(location.getLongitude()).isEqualTo(2.3522);
    }

}
