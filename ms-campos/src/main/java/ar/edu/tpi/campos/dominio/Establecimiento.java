package ar.edu.tpi.campos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "establecimiento")
public class Establecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del productor titular (claim sub del JWT de Keycloak). */
    @Column(name = "productor_sub", nullable = false, length = 64)
    private String productorSub;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Establecimiento() {
    }

    public Establecimiento(String productorSub, String nombre, Instant creadoEn) {
        this.productorSub = productorSub;
        this.nombre = nombre;
        this.creadoEn = creadoEn;
    }

    public boolean perteneceA(String sub) {
        return productorSub.equals(sub);
    }

    public Long getId() {
        return id;
    }

    public String getProductorSub() {
        return productorSub;
    }

    public String getNombre() {
        return nombre;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
