package veterinaria.vargasvet.citas;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.shared.EstadoCita;
import veterinaria.vargasvet.pacientes.Mascota;
import veterinaria.vargasvet.admin.Empleado;
import veterinaria.vargasvet.admin.Usuario;
import veterinaria.vargasvet.servicios.ServiciosVeterinarios;
import veterinaria.vargasvet.pagos.Purchase;
import veterinaria.vargasvet.clinica.Consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = true)
    private ServiciosVeterinarios servicio;

    @Column(name = "motivo_cita", nullable = false)
    private String motivoCita;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCita estado = EstadoCita.PROGRAMADA;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "motivo_reprogramacion")
    private String motivoReprogramacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reprogramado_por_user_id")
    private Usuario reprogramadoPor;

    @Column(name = "reprogramado_at")
    private LocalDateTime reprogramadoAt;

    @Column(name = "total_servicio", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalServicio;

    @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Column(name = "es_emergencia")
    private Boolean esEmergencia = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_user_id")
    private Usuario creadoPor;

    @Column(name = "eliminada", nullable = false, columnDefinition = "boolean default false")
    private Boolean eliminada = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eliminado_por_user_id")
    private Usuario eliminadoPor;

    @Column(name = "eliminado_at")
    private LocalDateTime eliminadoAt;

    @OneToMany(mappedBy = "cita", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Purchase> pagos = new ArrayList<>();

    @OneToOne(mappedBy = "cita", fetch = FetchType.LAZY)
    private Consulta consulta;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean estaPagadaCompleta() {
        return montoPagado != null && totalServicio != null
                && montoPagado.compareTo(totalServicio) >= 0;
    }
}
