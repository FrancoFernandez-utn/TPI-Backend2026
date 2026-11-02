package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.misiones.api.dto.SolicitudMisionRequest;
import ar.edu.tpi.misiones.api.dto.VentanaPropuesta;
import ar.edu.tpi.misiones.cliente.CamposCliente;
import ar.edu.tpi.misiones.cliente.ContextoLote;
import ar.edu.tpi.misiones.clima.ProveedorPronostico;
import ar.edu.tpi.misiones.config.ReglasProperties;
import ar.edu.tpi.misiones.dominio.Dron;
import ar.edu.tpi.misiones.dominio.EstadoDron;
import ar.edu.tpi.misiones.dominio.Mision;
import ar.edu.tpi.misiones.dominio.ModeloDron;
import ar.edu.tpi.misiones.dominio.Motivo;
import ar.edu.tpi.misiones.dominio.Producto;
import ar.edu.tpi.misiones.dominio.ResultadoEvaluacion;
import ar.edu.tpi.misiones.dominio.TipoMision;
import ar.edu.tpi.misiones.dominio.UmbralClima;
import ar.edu.tpi.misiones.motor.BuscadorVentanas;
import ar.edu.tpi.misiones.motor.CalculadoraDuracion;
import ar.edu.tpi.misiones.motor.CondicionHora;
import ar.edu.tpi.misiones.motor.Duracion;
import ar.edu.tpi.misiones.motor.EvaluacionFranja;
import ar.edu.tpi.misiones.motor.EvaluadorFranjas;
import ar.edu.tpi.misiones.motor.Pronostico;
import ar.edu.tpi.misiones.motor.RangoUmbral;
import ar.edu.tpi.misiones.motor.ResultadoBusqueda;
import ar.edu.tpi.misiones.repositorio.DronRepository;
import ar.edu.tpi.misiones.repositorio.MisionRepository;
import ar.edu.tpi.misiones.repositorio.ProductoRepository;
import ar.edu.tpi.misiones.repositorio.UmbralClimaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Proceso central del MVP: solicitud de una mision hasta su autorizacion (PROGRAMADA) o su rechazo.
 *
 * <ol>
 *   <li>Contexto del lote desde ms-campos y titularidad (RN-07).</li>
 *   <li>Reglas del lote y del producto: etapa prohibida (RN-09) e intervalo minimo (RN-10).</li>
 *   <li>Drones aptos para el tipo de mision (RN-04 estado, RN-05).</li>
 *   <li>Pronostico, franjas aptas y ventanas por modelo de dron (RN-14 a RN-16).</li>
 *   <li>Asignacion de dron y piloto con bloqueo (RN-01 a RN-06) y programacion.</li>
 * </ol>
 * Los motivos de rechazo se acumulan: la respuesta informa todos los que se detectaron en esa etapa.
 */
@Service
public class SolicitudMisionService {

    private static final Comparator<VentanaCandidata> MEJOR_PRIMERO = Comparator
            .comparingDouble((VentanaCandidata v) -> v.ventana().margen()).reversed()
            .thenComparingDouble(v -> v.ventana().vientoMedio())
            .thenComparing(v -> v.ventana().inicio())
            .thenComparing(v -> v.modelo().getId());

    private final CamposCliente campos;
    private final ProveedorPronostico clima;
    private final EvaluadorFranjas evaluador;
    private final ProductoRepository productos;
    private final DronRepository drones;
    private final MisionRepository misiones;
    private final UmbralClimaRepository umbrales;
    private final AsignadorRecursos asignador;
    private final RegistroMisiones registro;
    private final ReglasProperties reglas;
    private final Json json;
    private final Clock reloj;

    public SolicitudMisionService(CamposCliente campos, ProveedorPronostico clima, EvaluadorFranjas evaluador,
                                  ProductoRepository productos, DronRepository drones, MisionRepository misiones,
                                  UmbralClimaRepository umbrales, AsignadorRecursos asignador,
                                  RegistroMisiones registro, ReglasProperties reglas, Json json, Clock reloj) {
        this.campos = campos;
        this.clima = clima;
        this.evaluador = evaluador;
        this.productos = productos;
        this.drones = drones;
        this.misiones = misiones;
        this.umbrales = umbrales;
        this.asignador = asignador;
        this.registro = registro;
        this.reglas = reglas;
        this.json = json;
        this.reloj = reloj;
    }

    public Mision solicitar(String productorSub, SolicitudMisionRequest req) {
        Instant ahora = Instant.now(reloj);
        Producto producto = req.tipo() == TipoMision.PULVERIZACION
                ? productos.findById(req.productoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Producto", req.productoId()))
                : null;

        ContextoLote contexto = campos.contexto(req.loteId());
        if (!productorSub.equals(contexto.titularSub())) {
            // RN-07: un lote ajeno se trata como inexistente, sin revelar que existe.
            throw new RecursoNoEncontradoException("Lote", req.loteId());
        }

        Mision mision = new Mision(req.loteId(), productorSub, req.tipo(), producto,
                req.recetaNro() == null ? null : req.recetaNro().trim(), req.fechaLimite(),
                json.escribir(contexto), ahora);

        Instant hasta = min(req.fechaLimite(), ahora.plus(reglas.horizonteHoras(), ChronoUnit.HOURS));
        Instant desde = proximaHora(ahora);

        List<Motivo> motivos = new ArrayList<>();
        if (producto != null && producto.prohibidoEn(contexto.etapa())) {
            motivos.add(new Motivo(Motivo.ETAPA_PROHIBIDA, "El producto " + producto.getNombre()
                    + " no puede aplicarse en la etapa " + contexto.etapa()));
        }
        if (producto != null) {
            Instant ultima = misiones.ultimaAplicacionCompletada(req.loteId(), producto.getId());
            if (ultima != null) {
                Instant habilitado = ultima.plus(Duration.ofDays(producto.getIntervaloDias()));
                desde = max(desde, proximaHora(habilitado));
                if (!desde.isBefore(hasta)) {
                    motivos.add(new Motivo(Motivo.INTERVALO_MINIMO, "La ultima aplicacion de "
                            + producto.getNombre() + " en el lote termino " + ultima + "; con un intervalo de "
                            + producto.getIntervaloDias() + " dias recien puede aplicarse desde " + habilitado));
                }
            }
        }
        if (!motivos.isEmpty()) {
            return rechazar(mision, motivos, null, ahora);
        }

        List<Dron> aptos = drones.findByEstadoOrderByIdAsc(EstadoDron.OPERATIVO).stream()
                .filter(d -> AsignadorRecursos.esApto(d, req.tipo()))
                .toList();
        if (aptos.isEmpty()) {
            return rechazar(mision, List.of(new Motivo(Motivo.SIN_DRON_APTO,
                    "No hay drones operativos aptos para " + req.tipo())), null, ahora);
        }

        Pronostico pronostico = clima.obtener(contexto.lat().doubleValue(), contexto.lon().doubleValue());
        List<RangoUmbral> rangos = umbrales.buscarPorTipo(req.tipo()).stream().map(this::aRango).toList();

        Map<Long, ModeloDron> modelos = new LinkedHashMap<>();
        aptos.forEach(d -> modelos.putIfAbsent(d.getModelo().getId(), d.getModelo()));

        List<VentanaCandidata> candidatas = new ArrayList<>();
        ResultadoBusqueda masPermisivo = null;
        ModeloDron modeloMasPermisivo = null;
        for (ModeloDron modelo : modelos.values()) {
            Duracion duracion = CalculadoraDuracion.calcular(contexto.hectareas(), modelo.getHaPorHora(),
                    modelo.getHaPorVuelo(), reglas.minutosRecambioBateria());
            List<EvaluacionFranja> franjas = evaluador.evaluar(pronostico, rangos,
                    modelo.getLimiteRafagaKmh().doubleValue());
            ResultadoBusqueda resultado = BuscadorVentanas.buscar(franjas, desde, hasta, duracion);
            resultado.ventanas().forEach(v -> candidatas.add(new VentanaCandidata(v, modelo, duracion)));
            if (modeloMasPermisivo == null
                    || modelo.getLimiteRafagaKmh().compareTo(modeloMasPermisivo.getLimiteRafagaKmh()) > 0) {
                modeloMasPermisivo = modelo;
                masPermisivo = resultado;
            }
        }
        candidatas.sort(MEJOR_PRIMERO);

        String variables = json.escribir(horasAuditadas(pronostico, desde, hasta));
        String ventanas = json.escribir(candidatas.stream().limit(reglas.ventanasGuardadas())
                .map(c -> new VentanaPropuesta(c.ventana().inicio(), c.ventana().fin(), c.ventana().margen(),
                        c.ventana().vientoMedio(), c.modelo().getId(), c.modelo().getNombre()))
                .toList());

        if (candidatas.isEmpty()) {
            Motivo sinVentana = sinVentana(masPermisivo);
            DatosEvaluacion evaluacion = new DatosEvaluacion(ahora, clima.nombre(), resultadoSinVentana(masPermisivo),
                    variables, ventanas, json.escribir(List.of(sinVentana)));
            return rechazar(mision, List.of(sinVentana), evaluacion, ahora);
        }

        DatosEvaluacion apta = new DatosEvaluacion(ahora, clima.nombre(), ResultadoEvaluacion.APTA, variables,
                ventanas, "[]");
        Optional<Motivo> sinRecursos = asignador.asignarYProgramar(mision, candidatas,
                aptos.stream().map(Dron::getId).toList(), m -> apta, ahora);
        if (sinRecursos.isPresent()) {
            return rechazar(mision, List.of(sinRecursos.get()), apta, ahora);
        }
        return mision;
    }

    private Mision rechazar(Mision mision, List<Motivo> motivos, DatosEvaluacion evaluacion, Instant ahora) {
        mision.rechazar(json.escribir(motivos), ahora);
        return registro.guardar(mision, evaluacion);
    }

    /** RN-16: informa la variable que mas franjas bloqueo (con el modelo mas permisivo en rafagas). */
    private static Motivo sinVentana(ResultadoBusqueda resultado) {
        String detalle = resultado.causaPrincipal()
                .map(causa -> "Ninguna ventana cumple las condiciones antes de la fecha limite. Variable que mas"
                        + " impidio el vuelo: " + causa + " (" + resultado.bloqueos().get(causa) + " franjas)")
                .orElse("No hay franjas con luz solar dentro del rango de busqueda (fecha limite u horizonte de"
                        + " pronostico)");
        return new Motivo(Motivo.SIN_VENTANA_CLIMATICA, detalle);
    }

    private static ResultadoEvaluacion resultadoSinVentana(ResultadoBusqueda resultado) {
        boolean soloFaltanDatos = !resultado.bloqueos().isEmpty()
                && resultado.bloqueos().keySet().equals(java.util.Set.of(ResultadoBusqueda.SIN_DATOS));
        return soloFaltanDatos ? ResultadoEvaluacion.SIN_DATOS : ResultadoEvaluacion.NO_APTA;
    }

    /** Horas del pronostico que intervinieron en la decision (el rango de busqueda mas la lluvia siguiente). */
    private List<CondicionHora> horasAuditadas(Pronostico p, Instant desde, Instant hasta) {
        Instant fin = hasta.plus(reglas.horasLluviaSiguientes() + 1L, ChronoUnit.HOURS);
        return p.horas().stream()
                .filter(h -> !h.instante().isBefore(desde) && !h.instante().isAfter(fin))
                .toList();
    }

    private RangoUmbral aRango(UmbralClima u) {
        return new RangoUmbral(u.getVariable(), aDouble(u.getMinimo()), u.isMinInclusivo(),
                aDouble(u.getMaximo()), u.isMaxInclusivo());
    }

    private static Double aDouble(BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }

    /** Las ventanas empiezan en hora en punto: la primera franja posible es la proxima hora completa. */
    static Instant proximaHora(Instant t) {
        Instant truncada = t.truncatedTo(ChronoUnit.HOURS);
        return truncada.equals(t) ? t : truncada.plus(1, ChronoUnit.HOURS);
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
