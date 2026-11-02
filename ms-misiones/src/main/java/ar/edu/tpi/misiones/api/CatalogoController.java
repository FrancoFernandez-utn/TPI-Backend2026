package ar.edu.tpi.misiones.api;

import ar.edu.tpi.misiones.api.dto.ModeloDronRequest;
import ar.edu.tpi.misiones.api.dto.ModeloDronResponse;
import ar.edu.tpi.misiones.api.dto.ProductoRequest;
import ar.edu.tpi.misiones.api.dto.ProductoResponse;
import ar.edu.tpi.misiones.api.dto.UmbralesRequest;
import ar.edu.tpi.misiones.api.dto.UmbralesTipoResponse;
import ar.edu.tpi.misiones.dominio.TipoMision;
import ar.edu.tpi.misiones.servicio.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogos. Escritura solo ADMIN; lectura para cualquier usuario autenticado,
 * porque el productor elige el producto al solicitar y el piloto el modelo al registrar un dron (D-07, D-13).
 */
@RestController
@Tag(name = "Catalogos")
public class CatalogoController {

    private final CatalogoService servicio;

    public CatalogoController(CatalogoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/api/modelos-dron")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Agrega un modelo de dron al catalogo")
    public ResponseEntity<ModeloDronResponse> crearModelo(@Valid @RequestBody ModeloDronRequest req) {
        ModeloDronResponse creado = servicio.crearModelo(req);
        return ResponseEntity.created(URI.create("/api/modelos-dron/" + creado.id())).body(creado);
    }

    @GetMapping("/api/modelos-dron")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista los modelos de dron")
    public List<ModeloDronResponse> modelos() {
        return servicio.modelos();
    }

    @PostMapping("/api/productos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Agrega un producto con su intervalo minimo y sus etapas prohibidas")
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest req) {
        ProductoResponse creado = servicio.crearProducto(req);
        return ResponseEntity.created(URI.create("/api/productos/" + creado.id())).body(creado);
    }

    @GetMapping("/api/productos")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista los productos del catalogo")
    public List<ProductoResponse> productos() {
        return servicio.productos();
    }

    @GetMapping("/api/umbrales")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Umbrales climaticos vigentes por tipo de mision")
    public List<UmbralesTipoResponse> umbrales() {
        return servicio.umbrales();
    }

    @PutMapping("/api/umbrales/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reemplaza los umbrales de un tipo de mision (una variable ausente queda sin restriccion)")
    public UmbralesTipoResponse reemplazarUmbrales(@PathVariable TipoMision tipo,
                                                   @Valid @RequestBody UmbralesRequest req) {
        return servicio.reemplazarUmbrales(tipo, req);
    }
}
