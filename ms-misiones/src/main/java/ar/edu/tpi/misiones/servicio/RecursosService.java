package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.misiones.api.dto.DronRequest;
import ar.edu.tpi.misiones.api.dto.DronResponse;
import ar.edu.tpi.misiones.api.dto.PilotoRequest;
import ar.edu.tpi.misiones.api.dto.PilotoResponse;
import ar.edu.tpi.misiones.dominio.Dron;
import ar.edu.tpi.misiones.dominio.ModeloDron;
import ar.edu.tpi.misiones.dominio.Piloto;
import ar.edu.tpi.misiones.repositorio.DronRepository;
import ar.edu.tpi.misiones.repositorio.ModeloDronRepository;
import ar.edu.tpi.misiones.repositorio.PilotoRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta y consulta de la capacidad operativa: perfiles de piloto y drones. */
@Service
public class RecursosService {

    private final PilotoRepository pilotos;
    private final DronRepository drones;
    private final ModeloDronRepository modelos;
    private final Clock reloj;

    public RecursosService(PilotoRepository pilotos, DronRepository drones, ModeloDronRepository modelos,
                           Clock reloj) {
        this.pilotos = pilotos;
        this.drones = drones;
        this.modelos = modelos;
        this.reloj = reloj;
    }

    /** Cada usuario PILOTO tiene un unico perfil, asociado a su sub. */
    @Transactional
    public PilotoResponse registrarPiloto(String usuarioSub, PilotoRequest req) {
        if (pilotos.existsByUsuarioSub(usuarioSub)) {
            throw new ConflictoException("PERFIL_EXISTENTE", "El usuario ya tiene un perfil de piloto");
        }
        Piloto piloto = pilotos.save(new Piloto(usuarioSub, req.licenciaNro().trim(), req.licenciaVence(),
                req.horasVuelo(), req.habilitaciones(), Instant.now(reloj)));
        return PilotoResponse.de(piloto);
    }

    @Transactional(readOnly = true)
    public PilotoResponse perfilDe(String usuarioSub) {
        return pilotos.findByUsuarioSub(usuarioSub)
                .map(PilotoResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de piloto del usuario", usuarioSub));
    }

    @Transactional
    public DronResponse registrarDron(DronRequest req) {
        ModeloDron modelo = modelos.findById(req.modeloId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Modelo de dron", req.modeloId()));
        String matricula = req.matricula().trim().toUpperCase();
        if (drones.existsByMatriculaIgnoreCase(matricula)) {
            throw new ConflictoException("MATRICULA_DUPLICADA", "Ya existe un dron con la matricula " + matricula);
        }
        Dron dron = drones.save(new Dron(modelo, matricula, req.registroVence(), req.seguroVence(),
                Instant.now(reloj)));
        return DronResponse.de(dron);
    }
}
