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
import java.time.LocalDate;

@Entity
@Table(name = "dron")
public class Dron {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private ModeloDron modelo;

    @Column(nullable = false, unique = true, length = 30)
    private String matricula;

    @Column(name = "registro_vence", nullable = false)
    private LocalDate registroVence;

    @Column(name = "seguro_vence", nullable = false)
    private LocalDate seguroVence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDron estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Dron() {
    }

    public Dron(ModeloDron modelo, String matricula, LocalDate registroVence, LocalDate seguroVence,
                Instant creadoEn) {
        this.modelo = modelo;
        this.matricula = matricula;
        this.registroVence = registroVence;
        this.seguroVence = seguroVence;
        this.estado = EstadoDron.OPERATIVO;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public ModeloDron getModelo() {
        return modelo;
    }

    public String getMatricula() {
        return matricula;
    }

    public LocalDate getRegistroVence() {
        return registroVence;
    }

    public LocalDate getSeguroVence() {
        return seguroVence;
    }

    public EstadoDron getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
