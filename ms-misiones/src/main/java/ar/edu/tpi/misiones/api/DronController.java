package ar.edu.tpi.misiones.api;

import ar.edu.tpi.misiones.api.dto.DronRequest;
import ar.edu.tpi.misiones.api.dto.DronResponse;
import ar.edu.tpi.misiones.servicio.RecursosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drones")
@Tag(name = "Drones")
public class DronController {

    private final RecursosService servicio;

    public DronController(RecursosService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PILOTO')")
    @Operation(summary = "Registra un dron; queda OPERATIVO")
    public ResponseEntity<DronResponse> registrar(@Valid @RequestBody DronRequest req) {
        DronResponse creado = servicio.registrarDron(req);
        return ResponseEntity.created(URI.create("/api/drones/" + creado.id())).body(creado);
    }
}
