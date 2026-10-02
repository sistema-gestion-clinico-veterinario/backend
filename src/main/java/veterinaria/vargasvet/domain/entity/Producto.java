package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;
import veterinaria.vargasvet.domain.enums.TipoControlStock;

@Data
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 160)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoria;

    @Column(name = "precio", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "costo", precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(name = "marca", length = 80)
    private String marca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id")
    private MarcaProducto marcaProducto;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_stock", nullable = false, length = 20)
    private TipoControlStock controlStock = TipoControlStock.DIRECTO;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 0;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "sku", nullable = false, length = 30)
    private String sku;

    @Column(name = "codigo_barras", length = 64)
    private String codigoBarras;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "requiere_receta", nullable = false)
    private Boolean requiereReceta = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_medida_id")
    private UnidadMedida unidadMedida;

    @Enumerated(EnumType.STRING)
    @Column(name = "aplicacion_especie", nullable = false, length = 30)
    private TipoAplicacionProducto aplicacionEspecie = TipoAplicacionProducto.NO_ESPECIFICADO;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "producto_especie", joinColumns = @JoinColumn(name = "producto_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "especie", nullable = false, length = 30)
    private Set<EspecieMascota> especies = new HashSet<>();

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = veterinaria.vargasvet.util.AppClock.now();
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }
}
