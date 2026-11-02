package ar.edu.tpi.misiones.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraDuracionTest {

    @Test
    void cuarentaHectareasConDiezPorHoraYCincoPorVuelo() {
        Duracion d = CalculadoraDuracion.calcular(new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"), 10);

        assertThat(d.vuelos()).isEqualTo(8);
        // 240 min de vuelo + 7 recambios de 10 min
        assertThat(d.minutos()).isEqualTo(310);
        assertThat(d.franjasNecesarias()).isEqualTo(6);
    }

    @Test
    void losVuelosSeRedondeanHaciaArriba() {
        Duracion d = CalculadoraDuracion.calcular(new BigDecimal("12.5"), new BigDecimal("10"), new BigDecimal("5"), 10);

        assertThat(d.vuelos()).isEqualTo(3);
        assertThat(d.minutos()).isEqualTo(75 + 2 * 10);
    }

    @Test
    void unSoloVueloNoTieneRecambios() {
        Duracion d = CalculadoraDuracion.calcular(new BigDecimal("4"), new BigDecimal("8"), new BigDecimal("5"), 10);

        assertThat(d.vuelos()).isEqualTo(1);
        assertThat(d.minutos()).isEqualTo(30);
        assertThat(d.franjasNecesarias()).isEqualTo(1);
    }

    @Test
    void unaDuracionExactaDeUnaHoraOcupaUnaSolaFranja() {
        Duracion d = CalculadoraDuracion.calcular(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), 10);

        assertThat(d.minutos()).isEqualTo(60);
        assertThat(d.franjasNecesarias()).isEqualTo(1);
    }

    @Test
    void rechazaValoresNoPositivos() {
        assertThatThrownBy(() -> CalculadoraDuracion.calcular(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
