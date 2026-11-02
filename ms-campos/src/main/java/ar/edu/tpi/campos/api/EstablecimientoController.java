package ar.edu.tpi.campos.api;

import ar.edu.tpi.campos.api.dto.EstablecimientoRequest;
import ar.edu.tpi.campos.api.dto.EstablecimientoResponse;
import ar.edu.tpi.campos.api.dto.LoteRequest;
import ar.edu.tpi.campos.api.dto.LoteResponse;
import ar.edu.tpi.campos.servicio.CamposService;
import ar.edu.tpi.commons.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/establecimientos")
@Tag(name = "Establecimientos")
public class EstablecimientoController {

    private final CamposService servicio;

    public EstablecimientoController(CamposService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @PreAuthorize("hasRole('PRODUCTOR')")
    @Operation(summary = "Crea un establecimiento del productor autenticado")
    public ResponseEntity<EstablecimientoResponse> crear(@Valid @RequestBody EstablecimientoRequest req) {
        EstablecimientoResponse creado = servicio.crearEstablecimiento(UsuarioActual.sub(), req);
        return ResponseEntity.created(URI.create("/api/establecimientos/" + creado.id())).body(creado);
    }

    @PostMapping("/{id}/lotes")
    @PreAuthorize("hasRole('PRODUCTOR')")
    @Operation(summary = "Crea un lote con su cultivo y etapa actual en un establecimiento propio")
    public ResponseEntity<LoteResponse> crearLote(@PathVariable Long id, @Valid @RequestBody LoteRequest req) {
        LoteResponse creado = servicio.crearLote(UsuarioActual.sub(), id, req);
        return ResponseEntity.created(URI.create("/api/lotes/" + creado.id())).body(creado);
    }
}
