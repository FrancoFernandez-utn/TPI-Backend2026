package ar.edu.tpi.misiones.api;

import ar.edu.tpi.misiones.api.dto.MisionResponse;
import ar.edu.tpi.misiones.api.dto.SolicitudMisionRequest;
import ar.edu.tpi.misiones.api.dto.VentanaPropuesta;
import ar.edu.tpi.misiones.servicio.MisionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/misiones")
@Tag(name = "Misiones")
public class MisionController {

    private final MisionService servicio;

    public MisionController(MisionService servicio) {
        this.servicio = servicio;
    }

    /**
     * Devuelve 201 tanto si la mision quedo PROGRAMADA como RECHAZADA: en ambos casos se creo el recurso,
     * y un rechazo por reglas de negocio no es un error de la peticion (D-16). Los motivos van en el cuerpo.
     */
    @PostMapping
    @PreAuthorize("hasRole('PRODUCTOR')")
    @Operation(summary = "Solicita una mision: la evalua y la deja PROGRAMADA (con ventana, piloto y dron) o RECHAZADA con motivos")
    public ResponseEntity<MisionResponse> solicitar(@Valid @RequestBody SolicitudMisionRequest req) {
        MisionResponse mision = servicio.solicitar(req);
        return ResponseEntity.created(URI.create("/api/misiones/" + mision.id())).body(mision);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUCTOR','PILOTO','ADMIN')")
    @Operation(summary = "Consulta una mision con su evaluacion y sus motivos")
    public MisionResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('PRODUCTOR','PILOTO')")
    @Operation(summary = "Misiones del productor autenticado, o asignadas al piloto autenticado")
    public List<MisionResponse> mias() {
        return servicio.mias();
    }

    @GetMapping("/{id}/ventanas")
    @PreAuthorize("hasAnyRole('PRODUCTOR','ADMIN')")
    @Operation(summary = "Ventanas aptas encontradas en la evaluacion climatica, de mejor a peor")
    public List<VentanaPropuesta> ventanas(@PathVariable Long id) {
        return servicio.ventanas(id);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('PRODUCTOR')")
    @Operation(summary = "Cancela una mision AUTORIZADA o PROGRAMADA y libera su piloto y su dron")
    public MisionResponse cancelar(@PathVariable Long id) {
        return servicio.cancelar(id);
    }
}
