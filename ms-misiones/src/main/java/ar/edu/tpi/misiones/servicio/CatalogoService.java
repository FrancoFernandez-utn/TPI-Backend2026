package ar.edu.tpi.misiones.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoException;
import ar.edu.tpi.commons.excepcion.SolicitudInvalidaException;
import ar.edu.tpi.misiones.api.dto.ModeloDronRequest;
import ar.edu.tpi.misiones.api.dto.ModeloDronResponse;
import ar.edu.tpi.misiones.api.dto.ProductoRequest;
import ar.edu.tpi.misiones.api.dto.ProductoResponse;
import ar.edu.tpi.misiones.api.dto.UmbralDto;
import ar.edu.tpi.misiones.api.dto.UmbralesRequest;
import ar.edu.tpi.misiones.api.dto.UmbralesTipoResponse;
import ar.edu.tpi.misiones.dominio.ModeloDron;
import ar.edu.tpi.misiones.dominio.Producto;
import ar.edu.tpi.misiones.dominio.TipoMision;
import ar.edu.tpi.misiones.dominio.UmbralClima;
import ar.edu.tpi.misiones.dominio.VariableClima;
import ar.edu.tpi.misiones.repositorio.ModeloDronRepository;
import ar.edu.tpi.misiones.repositorio.ProductoRepository;
import ar.edu.tpi.misiones.repositorio.UmbralClimaRepository;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catalogos que administra el ADMIN: modelos de dron, productos y umbrales climaticos. */
@Service
public class CatalogoService {

    private final ModeloDronRepository modelos;
    private final ProductoRepository productos;
    private final UmbralClimaRepository umbrales;

    public CatalogoService(ModeloDronRepository modelos, ProductoRepository productos,
                           UmbralClimaRepository umbrales) {
        this.modelos = modelos;
        this.productos = productos;
        this.umbrales = umbrales;
    }

    @Transactional
    public ModeloDronResponse crearModelo(ModeloDronRequest req) {
        String nombre = req.nombre().trim();
        if (modelos.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("MODELO_DUPLICADO", "Ya existe un modelo llamado " + nombre);
        }
        ModeloDron modelo = modelos.save(new ModeloDron(nombre, req.capacidadL(), req.haPorHora(),
                req.haPorVuelo(), req.limiteRafagaKmh(), req.tieneCamara()));
        return ModeloDronResponse.de(modelo);
    }

    @Transactional(readOnly = true)
    public List<ModeloDronResponse> modelos() {
        return modelos.findAll(Sort.by("nombre")).stream().map(ModeloDronResponse::de).toList();
    }

    @Transactional
    public ProductoResponse crearProducto(ProductoRequest req) {
        String nombre = req.nombre().trim();
        if (productos.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("PRODUCTO_DUPLICADO", "Ya existe un producto llamado " + nombre);
        }
        Producto producto = productos.save(new Producto(nombre, req.intervaloDias(),
                req.etapasProhibidas() == null ? Set.of() : req.etapasProhibidas()));
        return ProductoResponse.de(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> productos() {
        return productos.findAll(Sort.by("nombre")).stream().map(ProductoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<UmbralesTipoResponse> umbrales() {
        return Arrays.stream(TipoMision.values()).map(this::umbralesDe).toList();
    }

    /** Reemplaza el conjunto completo de umbrales de un tipo de mision. */
    @Transactional
    public UmbralesTipoResponse reemplazarUmbrales(TipoMision tipo, UmbralesRequest req) {
        Set<VariableClima> vistas = EnumSet.noneOf(VariableClima.class);
        for (UmbralDto u : req.umbrales()) {
            if (!vistas.add(u.variable())) {
                throw new SolicitudInvalidaException("La variable " + u.variable() + " aparece mas de una vez");
            }
        }
        umbrales.borrarPorTipo(tipo);
        umbrales.flush();
        umbrales.saveAll(req.umbrales().stream()
                .map(u -> new UmbralClima(tipo, u.variable(), u.minimo(), u.minInclusivo(), u.maximo(),
                        u.maxInclusivo()))
                .toList());
        return umbralesDe(tipo);
    }

    private UmbralesTipoResponse umbralesDe(TipoMision tipo) {
        return new UmbralesTipoResponse(tipo, umbrales.buscarPorTipo(tipo).stream().map(UmbralDto::de).toList());
    }
}
