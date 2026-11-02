package ar.edu.tpi.misiones.clima;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.tpi.commons.excepcion.ServicioNoDisponibleException;
import ar.edu.tpi.misiones.dominio.VariableClima;
import ar.edu.tpi.misiones.motor.BuscadorVentanas;
import ar.edu.tpi.misiones.motor.Duracion;
import ar.edu.tpi.misiones.motor.EvaluadorFranjas;
import ar.edu.tpi.misiones.motor.Pronostico;
import ar.edu.tpi.misiones.motor.RangoUmbral;
import ar.edu.tpi.misiones.motor.ResultadoBusqueda;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimuladoProveedorTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Instant AHORA = Instant.parse("2026-11-20T11:00:00Z");
    private static final List<RangoUmbral> PULVERIZACION = List.of(
            new RangoUmbral(VariableClima.VIENTO, 3.0, true, 15.0, true),
            new RangoUmbral(VariableClima.RAFAGA, null, false, 22.0, false),
            new RangoUmbral(VariableClima.TEMPERATURA, null, false, 30.0, false),
            new RangoUmbral(VariableClima.HUMEDAD, 50.0, false, null, false),
            new RangoUmbral(VariableClima.PRECIPITACION, null, false, 0.0, true));

    private final SimuladoProveedor proveedor = new SimuladoProveedor(new ClimaProperties("simulado",
            ZONA.getId(),
            new ClimaProperties.OpenMeteo("x", 4, Duration.ofSeconds(1), Duration.ofSeconds(1), 0, Duration.ZERO),
            new ClimaProperties.Simulado(List.of(
                    new ClimaProperties.EscenarioPorUbicacion(-33.0, -63.0, EscenarioClima.VENTOSO),
                    new ClimaProperties.EscenarioPorUbicacion(-34.0, -62.0, EscenarioClima.SIN_SERVICIO)))),
            Clock.fixed(AHORA, ZONA));

    @Test
    void generaCuatroDiasHorariosDesdeHoyConSalidaYPuestaDelSol() {
        Pronostico p = proveedor.obtener(-31.42, -64.19);

        assertThat(p.horas()).hasSize(96);
        assertThat(p.luz()).hasSize(4);
        assertThat(p.horas().getFirst().instante().atZone(ZONA).toLocalDate())
                .isEqualTo(AHORA.atZone(ZONA).toLocalDate());
    }

    @Test
    void elEscenarioBuenoPermiteUnaVentanaDePulverizacion() {
        ResultadoBusqueda r = buscar(proveedor.obtener(-31.42, -64.19));

        assertThat(r.ventanas()).isNotEmpty();
    }

    @Test
    void elEscenarioVentosoNoTieneVentanaYLaCausaEsElViento() {
        ResultadoBusqueda r = buscar(proveedor.obtener(-33.0, -63.0));

        assertThat(r.ventanas()).isEmpty();
        assertThat(r.causaPrincipal()).isPresent();
        assertThat(r.bloqueos()).containsKeys("VIENTO", "RAFAGA");
    }

    @Test
    void elEscenarioSinServicioSimulaLaCaidaDeLaApi() {
        assertThatThrownBy(() -> proveedor.obtener(-34.0, -62.0)).isInstanceOf(ServicioNoDisponibleException.class);
    }

    private static ResultadoBusqueda buscar(Pronostico p) {
        var franjas = new EvaluadorFranjas(ZONA, 4).evaluar(p, PULVERIZACION, 35);
        return BuscadorVentanas.buscar(franjas, AHORA, AHORA.plusSeconds(72 * 3600), new Duracion(4, 150));
    }
}
