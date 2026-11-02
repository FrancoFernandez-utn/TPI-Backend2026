package ar.edu.tpi.campos.api;

import ar.edu.tpi.campos.api.dto.CultivoRequest;
import ar.edu.tpi.campos.api.dto.LoteResponse;
import ar.edu.tpi.campos.servicio.CamposService;
import ar.edu.tpi.commons.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lotes")
@Tag(name = "Lotes")
public class LoteController {

    private final CamposService servicio;

    public LoteController(CamposService servicio) {
        this.servicio = servicio;
    }

    @PutMapping("/{id}/cultivo")
    @PreAuthorize("hasRole('PRODUCTOR')")
    @Operation(summary = "Cambia el cultivo y la etapa de un lote propio (agrega un registro al historial)")
    public LoteResponse cambiarCultivo(@PathVariable Long id, @Valid @RequestBody CultivoRequest req) {
        return servicio.cambiarCultivo(UsuarioActual.sub(), id, req);
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('PRODUCTOR')")
    @Operation(summary = "Lotes del productor autenticado, con su cultivo y etapa actual")
    public List<LoteResponse> misLotes() {
        return servicio.lotesDe(UsuarioActual.sub());
    }
}
