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
 * Evaluacion climatica guardada para auditar la decision: que datos se usaron, que ventanas se
 * encontraron y con que resultado.
 */
@Entity
@Table(name = "evaluacion_mision")
public class EvaluacionMision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mision_id", nullable = false)
    private Mision mision;

    @Column(name = "consultada_en", nullable = false)
    private Instant consultadaEn;

    @Column(nullable = false, length = 30)
    private String fuente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoEvaluacion resultado;

    @Column(name = "variables_json", columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String variablesJson;

    @Column(name = "ventanas_json", columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String ventanasJson;

    @Column(name = "motivos_json", columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String motivosJson;

    protected EvaluacionMision() {
    }

    public EvaluacionMision(Mision mision, Instant consultadaEn, String fuente, ResultadoEvaluacion resultado,
                            String variablesJson, String ventanasJson, String motivosJson) {
        this.mision = mision;
        this.consultadaEn = consultadaEn;
        this.fuente = fuente;
        this.resultado = resultado;
        this.variablesJson = variablesJson;
        this.ventanasJson = ventanasJson;
        this.motivosJson = motivosJson;
    }

    public Long getId() {
        return id;
    }

    public Mision getMision() {
        return mision;
    }

    public Instant getConsultadaEn() {
        return consultadaEn;
    }

    public String getFuente() {
        return fuente;
    }

    public ResultadoEvaluacion getResultado() {
        return resultado;
    }

    public String getVariablesJson() {
        return variablesJson;
    }

    public String getVentanasJson() {
        return ventanasJson;
    }

    public String getMotivosJson() {
        return motivosJson;
    }
}
