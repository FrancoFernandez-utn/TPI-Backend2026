package ar.edu.tpi.misiones.api;

import ar.edu.tpi.commons.seguridad.UsuarioActual;
import ar.edu.tpi.misiones.api.dto.PilotoRequest;
import ar.edu.tpi.misiones.api.dto.PilotoResponse;
import ar.edu.tpi.misiones.servicio.RecursosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pilotos")
@Tag(name = "Pilotos")
public class PilotoController {

    private final RecursosService servicio;

    public PilotoController(RecursosService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @PreAuthorize("hasRole('PILOTO')")
    @Operation(summary = "Registra el perfil del piloto autenticado: licencia, horas y habilitaciones")
    public ResponseEntity<PilotoResponse> registrar(@Valid @RequestBody PilotoRequest req) {
        PilotoResponse creado = servicio.registrarPiloto(UsuarioActual.sub(), req);
        return ResponseEntity.created(URI.create("/api/pilotos/yo")).body(creado);
    }

    @GetMapping("/yo")
    @PreAuthorize("hasRole('PILOTO')")
    @Operation(summary = "Perfil del piloto autenticado")
    public PilotoResponse yo() {
        return servicio.perfilDe(UsuarioActual.sub());
    }
}
