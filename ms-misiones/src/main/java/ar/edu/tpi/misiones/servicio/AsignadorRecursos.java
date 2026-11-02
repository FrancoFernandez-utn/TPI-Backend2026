package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.misiones.clima.ClimaProperties;
import ar.edu.tpi.misiones.config.ReglasProperties;
import ar.edu.tpi.misiones.dominio.Dron;
import ar.edu.tpi.misiones.dominio.EstadoDron;
import ar.edu.tpi.misiones.dominio.EstadoMision;
import ar.edu.tpi.misiones.dominio.EstadoPiloto;
import ar.edu.tpi.misiones.dominio.Mision;
import ar.edu.tpi.misiones.dominio.Motivo;
import ar.edu.tpi.misiones.dominio.Piloto;
import ar.edu.tpi.misiones.dominio.TipoMision;
import ar.edu.tpi.misiones.repositorio.DronRepository;
import ar.edu.tpi.misiones.repositorio.MisionRepository;
import ar.edu.tpi.misiones.repositorio.PilotoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna piloto y dron a una mision (RN-01 a RN-06) y la programa, todo en una sola transaccion.
 *
 * <p>Concurrencia (D-15): al empezar bloquea con SELECT ... FOR UPDATE los drones candidatos y los pilotos
 * activos, siempre ordenados por id. Si dos solicitudes compiten por los mismos recursos, la segunda espera
 * a que la primera confirme y recien entonces verifica los solapamientos, que ya incluyen la reserva de la
 * primera. El orden fijo de bloqueo evita que dos transacciones se bloqueen mutuamente.
 *
 * <p>Las llamadas a ms-campos y a Open-Meteo ocurren antes de entrar aqui: no se retienen bloqueos mientras
 * se espera la red.
 */
@Service
public class AsignadorRecursos {

    private final DronRepository drones;
    private final PilotoRepository pilotos;
    private final MisionRepository misiones;
    private final RegistroMisiones registro;
    private final ReglasProperties reglas;
    private final ZoneId zona;

    public AsignadorRecursos(DronRepository drones, PilotoRepository pilotos, MisionRepository misiones,
                             RegistroMisiones registro, ReglasProperties reglas, ClimaProperties clima) {
        this.drones = drones;
        this.pilotos = pilotos;
        this.misiones = misiones;
        this.registro = registro;
        this.reglas = reglas;
        this.zona = ZoneId.of(clima.zonaHoraria());
    }

    /** RN-04 (estado) y RN-05 (aptitud): drones que pueden hacer este tipo de mision. */
    public static boolean esApto(Dron dron, TipoMision tipo) {
        if (dron.getEstado() != EstadoDron.OPERATIVO) {
            return false;
        }
        return switch (tipo) {
            case PULVERIZACION -> dron.getModelo().getCapacidadL().compareTo(BigDecimal.ZERO) > 0;
            case MONITOREO -> dron.getModelo().isTieneCamara();
            // RN-05 no define requisitos de equipamiento para SIEMBRA_COBERTURA (D-08).
            case SIEMBRA_COBERTURA -> true;
        };
    }

    /**
     * Recorre las ventanas de mejor a peor y programa la mision con el primer trio ventana-dron-piloto valido.
     * Si no hay ninguno, devuelve el motivo de rechazo.
     */
    @Transactional
    public Optional<Motivo> asignarYProgramar(Mision mision, List<VentanaCandidata> ventanas,
                                              Collection<Long> dronesCandidatos,
                                              Function<Mision, DatosEvaluacion> evaluacion, Instant ahora) {
        List<Dron> dronesBloqueados = drones.bloquear(dronesCandidatos).stream()
                .filter(d -> esApto(d, mision.getTipo()))
                .toList();
        List<Piloto> pilotosBloqueados = pilotos.bloquearPorEstado(EstadoPiloto.ACTIVO).stream()
                .filter(p -> habilitado(p, mision.getTipo()))
                .toList();

        boolean huboDronVigente = false;
        boolean huboDronLibre = false;
        for (VentanaCandidata candidata : ventanas) {
            Instant inicio = candidata.ventana().inicio();
            Instant fin = candidata.ventana().fin();
            LocalDate diaVuelo = inicio.atZone(zona).toLocalDate();

            for (Dron dron : dronesBloqueados) {
                if (!Objects.equals(dron.getModelo().getId(), candidata.modelo().getId())
                        || !vigenteEl(dron, diaVuelo)) {
                    continue;
                }
                huboDronVigente = true;
                if (misiones.dronOcupado(dron.getId(), inicio, fin, EstadoMision.RESERVAN_RECURSOS)) {
                    continue;
                }
                huboDronLibre = true;
                Optional<Piloto> piloto = pilotosBloqueados.stream()
                        .filter(p -> disponible(p, diaVuelo, inicio, fin, candidata.duracion().minutos()))
                        .findFirst();
                if (piloto.isPresent()) {
                    mision.autorizar(inicio, fin, candidata.duracion().vuelos(), candidata.duracion().minutos(),
                            ahora);
                    mision.programar(piloto.get(), dron, ahora);
                    registro.guardar(mision, evaluacion.apply(mision));
                    return Optional.empty();
                }
                // El piloto no depende del dron: si no hay piloto para esta ventana, se pasa a la siguiente.
                break;
            }
        }
        if (!huboDronVigente) {
            return Optional.of(new Motivo(Motivo.SIN_DRON_APTO,
                    "Ningun dron apto tiene registro y seguro vigentes en las fechas de las ventanas encontradas"));
        }
        if (!huboDronLibre) {
            return Optional.of(new Motivo(Motivo.SIN_DRON_DISPONIBLE,
                    "Todos los drones aptos tienen otra mision en las ventanas encontradas"));
        }
        return Optional.of(new Motivo(Motivo.SIN_PILOTO_DISPONIBLE,
                "Ningun piloto habilitado tiene licencia vigente, horario libre y horas disponibles en esas ventanas"));
    }

    /** RN-02: habilitacion para el tipo y, para PULVERIZACION, horas minimas de vuelo. */
    private boolean habilitado(Piloto piloto, TipoMision tipo) {
        if (!piloto.getHabilitaciones().contains(tipo)) {
            return false;
        }
        return tipo != TipoMision.PULVERIZACION
                || piloto.getHorasVuelo().compareTo(BigDecimal.valueOf(reglas.horasMinimasPulverizacion())) >= 0;
    }

    /** RN-01 (licencia vigente el dia del vuelo) y RN-03 (sin solapamiento y tope diario de horas). */
    private boolean disponible(Piloto piloto, LocalDate diaVuelo, Instant inicio, Instant fin, int minutos) {
        if (piloto.getLicenciaVence().isBefore(diaVuelo)) {
            return false;
        }
        if (misiones.pilotoOcupado(piloto.getId(), inicio, fin, EstadoMision.RESERVAN_RECURSOS)) {
            return false;
        }
        Instant desdeDia = diaVuelo.atStartOfDay(zona).toInstant();
        Instant hastaDia = diaVuelo.plusDays(1).atStartOfDay(zona).toInstant();
        long yaProgramados = misiones.minutosProgramados(piloto.getId(), desdeDia, hastaDia,
                EstadoMision.RESERVAN_RECURSOS);
        return yaProgramados + minutos <= reglas.maxHorasPilotoDia() * 60L;
    }

    /** RN-04: registro y seguro vigentes el dia del vuelo. */
    private static boolean vigenteEl(Dron dron, LocalDate diaVuelo) {
        return !dron.getRegistroVence().isBefore(diaVuelo) && !dron.getSeguroVence().isBefore(diaVuelo);
    }
}
