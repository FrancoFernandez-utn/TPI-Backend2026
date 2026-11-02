package ar.edu.tpi.misiones.motor;

import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.ZONA;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.conViento;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.dias;
import static ar.edu.tpi.misiones.motor.PronosticoDePrueba.hora;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuscadorVentanasTest {

    private final EvaluadorFranjas evaluador = new EvaluadorFranjas(ZONA, 4);

    @Test
    void encuentraVentanasDeFranjasConsecutivasConLaDuracionPedida() {
        List<EvaluacionFranja> franjas = evaluador.evaluar(dias(3, c -> c), EvaluadorFranjasTest.PULVERIZACION, 35);

        ResultadoBusqueda r = BuscadorVentanas.buscar(franjas, hora(0, 0), hora(2, 0), new Duracion(4, 150));

        assertThat(r.ventanas()).isNotEmpty();
        Ventana v = r.ventanas().getFirst();
        assertThat(ChronoUnit.MINUTES.between(v.inicio(), v.fin())).isEqualTo(150);
        // Necesita 3 franjas diurnas consecutivas: la ultima que entra en el dia empieza 16:00 y termina 18:30.
        assertThat(r.ventanas()).allSatisfy(w -> assertThat(w.inicio().atZone(ZONA).getHour()).isBetween(7, 16));
    }

    @Test
    void unaFranjaNoAptaEnElMedioCortaLaVentana() {
        // Viento fuerte toda la tarde del dia 0: solo quedan 4 franjas a la manana (7 a 11).
        Pronostico p = dias(1, c -> {
            int h = c.instante().atZone(ZONA).getHour();
            return h >= 12 ? conViento(c, 20) : c;
        });
        List<EvaluacionFranja> franjas = evaluador.evaluar(p, EvaluadorFranjasTest.PULVERIZACION, 35);

        ResultadoBusqueda cuatroHoras = BuscadorVentanas.buscar(franjas, hora(0, 0), hora(1, 0), new Duracion(5, 240));
        ResultadoBusqueda cincoHoras = BuscadorVentanas.buscar(franjas, hora(0, 0), hora(1, 0), new Duracion(6, 300));

        assertThat(cuatroHoras.ventanas()).extracting(Ventana::inicio).containsExactly(hora(0, 7));
        assertThat(cincoHoras.ventanas()).isEmpty();
    }

    @Test
    void respetaElInicioMinimoYElLimite() {
        List<EvaluacionFranja> franjas = evaluador.evaluar(dias(3, c -> c), EvaluadorFranjasTest.PULVERIZACION, 35);
        Instant desde = hora(1, 9);
        Instant hasta = hora(1, 12);

        ResultadoBusqueda r = BuscadorVentanas.buscar(franjas, desde, hasta, new Duracion(1, 60));

        assertThat(r.ventanas()).extracting(Ventana::inicio)
                .containsExactlyInAnyOrder(hora(1, 9), hora(1, 10), hora(1, 11));
    }

    @Test
    void ordenaPorMayorMargenYLuegoPorLaMasTemprana() {
        // Dia 0 con viento cerca del limite; dia 1 con viento ideal (9 km/h, centro del rango 3-15).
        Pronostico p = dias(3, c -> c.instante().isBefore(hora(1, 0)) ? conViento(c, 14) : conViento(c, 9));
        List<EvaluacionFranja> franjas = evaluador.evaluar(p, EvaluadorFranjasTest.PULVERIZACION, 35);

        ResultadoBusqueda r = BuscadorVentanas.buscar(franjas, hora(0, 0), hora(2, 0), new Duracion(1, 60));

        assertThat(r.ventanas().getFirst().inicio()).isEqualTo(hora(1, 7));
    }

    @Test
    void sinVentanaInformaLaCausaQueMasFranjasBloqueo() {
        Pronostico ventoso = dias(2, c -> conViento(c, 30));
        List<EvaluacionFranja> franjas = evaluador.evaluar(ventoso, EvaluadorFranjasTest.PULVERIZACION, 35);

        ResultadoBusqueda r = BuscadorVentanas.buscar(franjas, hora(0, 0), hora(1, 0), new Duracion(1, 60));

        assertThat(r.ventanas()).isEmpty();
        assertThat(r.causaPrincipal()).contains("VIENTO");
        // 12 franjas diurnas completas (07 a 19) bloqueadas por viento
        assertThat(r.bloqueos().get("VIENTO")).isEqualTo(12);
    }

    @Test
    void lasFranjasNocturnasNoCuentanComoBloqueos() {
        Pronostico p = dias(2, c -> {
            int h = c.instante().atZone(ZONA).getHour();
            return h < 6 ? conViento(c, 30) : c;
        });
        List<EvaluacionFranja> franjas = evaluador.evaluar(p, EvaluadorFranjasTest.PULVERIZACION, 35);

        ResultadoBusqueda r = BuscadorVentanas.buscar(franjas, hora(0, 0), hora(1, 0), new Duracion(1, 60));

        assertThat(r.bloqueos()).doesNotContainKey("VIENTO");
    }
}
