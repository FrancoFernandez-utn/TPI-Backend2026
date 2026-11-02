package ar.edu.tpi.misiones.motor;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.tpi.misiones.dominio.VariableClima;
import org.junit.jupiter.api.Test;

class RangoUmbralTest {

    @Test
    void vientoDePulverizacionEsInclusivoEnAmbosExtremos() {
        RangoUmbral viento = new RangoUmbral(VariableClima.VIENTO, 3.0, true, 15.0, true);

        assertThat(viento.cumple(3)).isTrue();
        assertThat(viento.cumple(15)).isTrue();
        assertThat(viento.cumple(2.9)).isFalse();
        assertThat(viento.cumple(15.1)).isFalse();
    }

    @Test
    void menorAEsEstricto() {
        RangoUmbral rafaga = new RangoUmbral(VariableClima.RAFAGA, null, false, 22.0, false);

        assertThat(rafaga.cumple(21.9)).isTrue();
        assertThat(rafaga.cumple(22)).isFalse();
    }

    @Test
    void mayorAEsEstricto() {
        RangoUmbral humedad = new RangoUmbral(VariableClima.HUMEDAD, 50.0, false, null, false);

        assertThat(humedad.cumple(50)).isFalse();
        assertThat(humedad.cumple(50.1)).isTrue();
    }

    @Test
    void ceroMilimetrosSoloAceptaCero() {
        RangoUmbral lluvia = new RangoUmbral(VariableClima.PRECIPITACION, null, false, 0.0, true);

        assertThat(lluvia.cumple(0)).isTrue();
        assertThat(lluvia.cumple(0.1)).isFalse();
        assertThat(lluvia.margen(0)).isNull();
    }

    @Test
    void elMargenEsMaximoEnElCentroDelRangoYCeroEnElBorde() {
        RangoUmbral viento = new RangoUmbral(VariableClima.VIENTO, 3.0, true, 15.0, true);

        assertThat(viento.margen(9)).isEqualTo(1.0);
        assertThat(viento.margen(3)).isEqualTo(0.0);
        assertThat(viento.margen(12)).isEqualTo(0.5);
    }

    @Test
    void elLimiteDelDronSoloAjustaSiEsMasExigente() {
        RangoUmbral rafaga = new RangoUmbral(VariableClima.RAFAGA, null, false, 22.0, false);

        assertThat(rafaga.conMaximoEstricto(18).maximo()).isEqualTo(18.0);
        assertThat(rafaga.conMaximoEstricto(30).maximo()).isEqualTo(22.0);
    }
}
