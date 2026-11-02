package ar.edu.tpi.misiones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Mision solicitada por un productor sobre uno de sus lotes.
 *
 * <p>Los cambios de estado pasan siempre por {@link #transicionarA}, que valida la transicion contra
 * {@link EstadoMision}: no hay forma de asignar un estado arbitrario.
 */
@Entity
@Table(name = "mision")
public class Mision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia logica al lote de ms-campos (sin clave foranea entre esquemas). */
    @Column(name = "lote_id", nullable = false)
    private Long loteId;

    @Column(name = "productor_sub", nullable = false, length = 64)
    private String productorSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMision tipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "receta_nro", length = 40)
    private String recetaNro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "piloto_id")
    private Piloto piloto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dron_id")
    private Dron dron;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoMision estado;

    @Column(name = "inicio_prev")
    private Instant inicioPrev;

    @Column(name = "fin_prev")
    private Instant finPrev;

    private Integer vuelos;

    @Column(name = "duracion_min")
    private Integer duracionMin;

    @Column(name = "fecha_limite", nullable = false)
    private Instant fechaLimite;

    /** Copia del contexto del lote con el que se decidio (auditoria). */
    @Column(name = "contexto_json", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String contextoJson;

    @Column(name = "motivos_json", columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String motivosJson = "[]";

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    protected Mision() {
    }

    public Mision(Long loteId, String productorSub, TipoMision tipo, Producto producto, String recetaNro,
                  Instant fechaLimite, String contextoJson, Instant ahora) {
        this.loteId = loteId;
        this.productorSub = productorSub;
        this.tipo = tipo;
        this.producto = producto;
        this.recetaNro = recetaNro;
        this.fechaLimite = fechaLimite;
        this.contextoJson = contextoJson;
        this.estado = EstadoMision.SOLICITADA;
        this.creadaEn = ahora;
        this.actualizadaEn = ahora;
    }

    /** Unico punto de cambio de estado: valida la transicion antes de aplicarla. */
    public void transicionarA(EstadoMision destino, Instant ahora) {
        if (!estado.puedePasarA(destino)) {
            throw new TransicionInvalidaException(estado, destino);
        }
        this.estado = destino;
        this.actualizadaEn = ahora;
    }

    public void rechazar(String motivosJson, Instant ahora) {
        transicionarA(EstadoMision.RECHAZADA, ahora);
        this.motivosJson = motivosJson;
    }

    /** Hay ventana, dron y piloto: se autoriza con la ventana propuesta. */
    public void autorizar(Instant inicio, Instant fin, int vuelos, int duracionMin, Instant ahora) {
        transicionarA(EstadoMision.AUTORIZADA, ahora);
        this.inicioPrev = inicio;
        this.finPrev = fin;
        this.vuelos = vuelos;
        this.duracionMin = duracionMin;
    }

    /** Se confirma la reserva de piloto y dron en la misma transaccion. */
    public void programar(Piloto piloto, Dron dron, Instant ahora) {
        transicionarA(EstadoMision.PROGRAMADA, ahora);
        this.piloto = piloto;
        this.dron = dron;
    }

    public void cancelar(Instant ahora) {
        transicionarA(EstadoMision.CANCELADA, ahora);
    }

    public boolean perteneceA(String sub) {
        return productorSub.equals(sub);
    }

    public boolean asignadaA(String pilotoSub) {
        return piloto != null && piloto.getUsuarioSub().equals(pilotoSub);
    }

    public Long getId() {
        return id;
    }

    public Long getLoteId() {
        return loteId;
    }

    public String getProductorSub() {
        return productorSub;
    }

    public TipoMision getTipo() {
        return tipo;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getRecetaNro() {
        return recetaNro;
    }

    public Piloto getPiloto() {
        return piloto;
    }

    public Dron getDron() {
        return dron;
    }

    public EstadoMision getEstado() {
        return estado;
    }

    public Instant getInicioPrev() {
        return inicioPrev;
    }

    public Instant getFinPrev() {
        return finPrev;
    }

    public Integer getVuelos() {
        return vuelos;
    }

    public Integer getDuracionMin() {
        return duracionMin;
    }

    public Instant getFechaLimite() {
        return fechaLimite;
    }

    public String getContextoJson() {
        return contextoJson;
    }

    public String getMotivosJson() {
        return motivosJson;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }
}
