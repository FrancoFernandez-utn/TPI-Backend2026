package ar.edu.tpi.campos.api;

import ar.edu.tpi.campos.api.dto.ContextoLoteResponse;
import ar.edu.tpi.campos.servicio.CamposService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos: solo los consume otro microservicio con rol SERVICIO
 * (token de client credentials). El Gateway no expone /internal/**.
 */
@RestController
@RequestMapping("/internal/lotes")
@Tag(name = "Interno")
public class ContextoInternoController {

    private final CamposService servicio;

    public ContextoInternoController(CamposService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}/contexto")
    @PreAuthorize("hasRole('SERVICIO')")
    @Operation(summary = "Contexto del lote para decidir una mision: titular, ubicacion, superficie, cultivo y etapa")
    public ContextoLoteResponse contexto(@PathVariable Long id) {
        return servicio.contexto(id);
    }
}
