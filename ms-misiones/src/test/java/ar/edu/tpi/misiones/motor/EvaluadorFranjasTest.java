package ar.edu.tpi.misiones.motor;

import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.ZONA;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.conLluvia;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.conRafaga;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.conTemperatura;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.conViento;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.dias;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.hora;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.tpi.misiones.dominio.VariableClima;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluadorFranjasTest {

    /** Umbrales de PULVERIZACION de la seccion 5.3 del dominio. */
    static final List<RangoUmbral> PULVERIZACION = List.of(
            new RangoUmbral(VariableClima.VIENTO, 3.0, true, 15.0, true),
            new RangoUmbral(VariableClima.RAFAGA, null, false, 22.0, false),
            new RangoUmbral(VariableClima.TEMPERATURA, null, false, 30.0, false),
            new RangoUmbral(VariableClima.HUMEDAD, 50.0, false, null, false),
            new RangoUmbral(VariableClima.PRECIPITACION, null, false, 0.0, true));

    private final EvaluadorFranjas evaluador = new EvaluadorFranjas(ZONA, 4);

    @Test
    void condicionesBuenasDeDiaSonAptasYDeNocheNo() {
        List<EvaluacionFranja> franjas = evaluador.evaluar(dias(2, c -> c), PULVERIZACION, 35);

        assertThat(franja(franjas, hora(0, 10)).apta()).isTrue();
        assertThat(franja(franjas, hora(0, 2)).diurna()).isFalse();
        assertThat(franja(franjas, hora(0, 2)).apta()).isFalse();
    }

    @Test
    void laFranjaDebeCaerCompletaDentroDeLaLuzSolar() {
        // Luz de 06:30 a 19:30: la franja de 06:00 empieza antes de la salida y la de 19:00 termina despues de la puesta.
        List<EvaluacionFranja> franjas = evaluador.evaluar(dias(2, c -> c), PULVERIZACION, 35);

        assertThat(franja(franjas, hora(0, 6)).diurna()).isFalse();
        assertThat(franja(franjas, hora(0, 7)).diurna()).isTrue();
        assertThat(franja(franjas, hora(0, 18)).diurna()).isTrue();
        assertThat(franja(franjas, hora(0, 19)).diurna()).isFalse();
    }

    @Test
    void vientoInstantaneoDebeCumplirAlInicioYAlFinDeLaFranja() {
        Instant once = hora(0, 11);
        Pronostico p = dias(2, c -> c.instante().equals(once) ? conViento(c, 16) : c);

        List<EvaluacionFranja> franjas = evaluador.evaluar(p, PULVERIZACION, 35);

        // El viento de las 11:00 afecta a la franja 10-11 (como fin) y a la 11-12 (como inicio).
        assertThat(franja(franjas, hora(0, 10)).fallidas()).containsExactly(VariableClima.VIENTO);
        assertThat(franja(franjas, hora(0, 11)).fallidas()).containsExactly(VariableClima.VIENTO);
        assertThat(franja(franjas, hora(0, 12)).apta()).isTrue();
    }

    @Test
    void calmaTotalTambienImpidePulverizar() {
        Pronostico p = dias(2, c -> conViento(c, 2));

        assertThat(franja(evaluador.evaluar(p, PULVERIZACION, 35), hora(0, 10)).fallidas())
                .containsExactly(VariableClima.VIENTO);
    }

    @Test
    void laRafagaDeLaMarcaSiguienteEsLaQueCuentaParaLaFranja() {
        // La rafaga informada a las 11:00 es el maximo de 10:00 a 11:00.
        Instant once = hora(0, 11);
        Pronostico p = dias(2, c -> c.instante().equals(once) ? conRafaga(c, 25) : c);

        List<EvaluacionFranja> franjas = evaluador.evaluar(p, PULVERIZACION, 35);

        assertThat(franja(franjas, hora(0, 10)).fallidas()).containsExactly(VariableClima.RAFAGA);
        assertThat(franja(franjas, hora(0, 11)).apta()).isTrue();
    }

    @Test
    void elLimiteDeRafagaDelModeloEsMasExigenteQueElDelTipo() {
        Pronostico p = dias(2, c -> conRafaga(c, 19));

        assertThat(franja(evaluador.evaluar(p, PULVERIZACION, 35), hora(0, 10)).apta()).isTrue();
        assertThat(franja(evaluador.evaluar(p, PULVERIZACION, 18), hora(0, 10)).fallidas())
                .containsExactly(VariableClima.RAFAGA);
    }

    @Test
    void lluviaEnLaHoraOEnLasCuatroSiguientesImpideLaFranja() {
        // La marca de las 15:00 acumula la lluvia de 14:00 a 15:00.
        Instant quince = hora(0, 15);
        Pronostico p = dias(2, c -> c.instante().equals(quince) ? conLluvia(c, 0.5) : c);

        List<EvaluacionFranja> franjas = evaluador.evaluar(p, PULVERIZACION, 35);

        // La franja 10-11 mira las marcas 11 a 15: la lluvia de las 15 la bloquea.
        assertThat(franja(franjas, hora(0, 10)).fallidas()).containsExactly(VariableClima.PRECIPITACION);
        assertThat(franja(franjas, hora(0, 14)).fallidas()).containsExactly(VariableClima.PRECIPITACION);
        // La franja 9-10 mira las marcas 10 a 14: no llega a la lluvia.
        assertThat(franja(franjas, hora(0, 9)).apta()).isTrue();
        // La franja 15-16 mira desde las 16: la lluvia ya paso.
        assertThat(franja(franjas, hora(0, 15)).apta()).isTrue();
    }

    @Test
    void unValorFaltanteDejaLaFranjaSinDatosYNoApta() {
        Instant doce = hora(0, 12);
        Pronostico p = dias(2, c -> c.instante().equals(doce) ? conTemperatura(c, null) : c);

        EvaluacionFranja f = franja(evaluador.evaluar(p, PULVERIZACION, 35), hora(0, 12));

        assertThat(f.sinDatos()).isTrue();
        assertThat(f.apta()).isFalse();
    }

    @Test
    void sinLasCuatroHorasSiguientesDeLluviaAlFinalDelPronosticoNoHayDatos() {
        Pronostico p = dias(1, c -> c);

        assertThat(franja(evaluador.evaluar(p, PULVERIZACION, 35), hora(0, 20)).sinDatos()).isTrue();
    }

    @Test
    void elMargenReflejaLaCercaniaAlLimiteMasExigente() {
        Pronostico tranquilo = dias(2, c -> conViento(c, 9));
        Pronostico alLimite = dias(2, c -> conViento(c, 14));

        double margenTranquilo = franja(evaluador.evaluar(tranquilo, PULVERIZACION, 35), hora(0, 10)).margen();
        double margenAlLimite = franja(evaluador.evaluar(alLimite, PULVERIZACION, 35), hora(0, 10)).margen();

        assertThat(margenTranquilo).isGreaterThan(margenAlLimite);
    }

    static EvaluacionFranja franja(List<EvaluacionFranja> franjas, Instant inicio) {
        return franjas.stream().filter(f -> f.inicio().equals(inicio)).findFirst().orElseThrow();
    }
}
