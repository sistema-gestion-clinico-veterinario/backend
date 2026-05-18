package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prescripciones")
public class Prescripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Empleado veterinario;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "medicamento", nullable = false, length = 200)
    private String medicamento;

    @Column(name = "principio_activo", length = 200)
    private String principioActivo;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Column(nullable = false, length = 100)
    private String frecuencia;

    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(name = "via_administracion", length = 50)
    private String viaAdministracion;

    @Column(columnDefinition = "TEXT")
    private String instrucciones;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_catalogo_id", nullable = true)
    private Producto_Tienda productoCatalogo;

    @OneToOne(mappedBy = "prescripcion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DispensacionMedicamento dispensacion;
}
