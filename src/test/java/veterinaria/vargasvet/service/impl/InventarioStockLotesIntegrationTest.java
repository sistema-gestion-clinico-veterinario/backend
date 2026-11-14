package veterinaria.vargasvet.service.impl;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Lote;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;
import veterinaria.vargasvet.domain.enums.TipoControlStock;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(InventarioStockServiceImpl.class)
class InventarioStockLotesIntegrationTest {

    @Autowired private InventarioStockServiceImpl inventario;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private LoteRepository loteRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CategoriaProductoRepository categoriaRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private TransactionTemplate transaccion;
    private Company clinica;
    private CategoriaProducto categoria;

    @BeforeEach
    void datos() {
        transaccion = new TransactionTemplate(transacciones);
        clinica = new Company();
        clinica.setName("Clinica Patitas");
        clinica.setSlug("patitas-" + UUID.randomUUID());
        clinica.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        clinica.setActivo(true);
        clinica = companyRepository.save(clinica);
        categoria = new CategoriaProducto();
        categoria.setCompany(clinica);
        categoria.setNombre("Categoria " + UUID.randomUUID());
        categoria.setActivo(true);
        categoria = categoriaRepository.save(categoria);
    }

    @Test
    void descuentaVariosLotesEnOrdenDeVencimientoYDejaElUltimoParcial() {
        Long producto = productoConLotes(5, 10);

        descontar(producto, 25);

        assertThat(cantidades(producto)).containsExactly(0, 0, 5, 10, 10);
        assertThat(salidas(producto)).containsExactly(10, 10, 5);
    }

    @Test
    void descuentaMasLotesQueLosQueCabenEnUnTramoLeyendoElSiguiente() {
        Long producto = productoConLotes(12, 10);

        descontar(producto, 60);

        assertThat(cantidades(producto)).containsExactly(0, 0, 0, 0, 0, 0, 10, 10, 10, 10, 10, 10);
        assertThat(salidas(producto)).containsExactly(10, 10, 10, 10, 10, 10);
    }

    @Test
    void sePuedeVaciarTodoElStockDelProducto() {
        Long producto = productoConLotes(12, 10);

        descontar(producto, 120);

        assertThat(cantidades(producto)).containsOnly(0);
        assertThat(salidas(producto)).hasSize(12);
    }

    @Test
    void soloCargaUnTramoDeLotesAunqueElProductoTengaMuchos() {
        Long producto = productoConLotes(60, 10);
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        descontar(producto, 5);

        assertThat(estadisticas.getEntityStatistics(Lote.class.getName()).getLoadCount()).isLessThanOrEqualTo(5);
        assertThat(cantidades(producto).getFirst()).isEqualTo(5);
    }

    @Test
    void siNoAlcanzaElStockLaBaseQuedaIntacta() {
        Long producto = productoConLotes(3, 10);

        assertThatThrownBy(() -> descontar(producto, 35))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock por lotes insuficiente");

        assertThat(cantidades(producto)).containsExactly(10, 10, 10);
        assertThat(salidas(producto)).isEmpty();
    }

    @Test
    void ignoraLosLotesVencidosInactivosOAgotados() {
        Long producto = productoConLotes(0, 0);
        crearLote(producto, "VENCIDO", 10, LocalDate.now().minusDays(1), true);
        crearLote(producto, "INACTIVO", 10, LocalDate.now().plusDays(20), false);
        crearLote(producto, "AGOTADO", 0, LocalDate.now().plusDays(21), true);
        crearLote(producto, "VIGENTE", 10, LocalDate.now().plusDays(30), true);

        descontar(producto, 4);

        assertThat(jdbcTemplate.queryForList("SELECT numero_lote || ':' || cantidad FROM lote WHERE producto_id = ? ORDER BY numero_lote",
                String.class, producto)).containsExactly("AGOTADO:0", "INACTIVO:10", "VENCIDO:10", "VIGENTE:6");
        assertThat(salidas(producto)).containsExactly(4);
    }

    @Test
    void siSoloHayLotesNoDisponiblesLanzaElError() {
        Long producto = productoConLotes(0, 0);
        crearLote(producto, "VENCIDO", 10, LocalDate.now().minusDays(1), true);

        assertThatThrownBy(() -> descontar(producto, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock por lotes insuficiente");
    }

    @Test
    void conIgualVencimientoDescuentaPrimeroElDeIngresoMasAntiguoYLuegoElDeMenorId() {
        Long producto = productoConLotes(0, 0);
        LocalDate vence = LocalDate.now().plusDays(30);
        crearLote(producto, "C-RECIENTE", 10, vence, true, LocalDate.now());
        crearLote(producto, "A-ANTIGUO-1", 10, vence, true, LocalDate.now().minusDays(5));
        crearLote(producto, "B-ANTIGUO-2", 10, vence, true, LocalDate.now().minusDays(5));

        descontar(producto, 25);

        assertThat(jdbcTemplate.queryForList("SELECT numero_lote || ':' || cantidad FROM lote WHERE producto_id = ? ORDER BY numero_lote",
                String.class, producto)).containsExactly("A-ANTIGUO-1:0", "B-ANTIGUO-2:0", "C-RECIENTE:5");
    }

    private void descontar(Long productoId, int cantidad) {
        transaccion.executeWithoutResult(status ->
                inventario.descontar(productoRepository.findById(productoId).orElseThrow(), cantidad, "VENTA_LIBRE_DETALLE", 7L));
    }

    private Long productoConLotes(int lotes, int cantidadPorLote) {
        Producto producto = new Producto();
        producto.setCompany(clinica);
        producto.setNombre("Vacuna");
        producto.setCategoria(categoria);
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setStock(0);
        producto.setControlStock(TipoControlStock.LOTES);
        producto.setStockMinimo(1);
        producto.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        producto.setRequiereReceta(false);
        producto.setAplicacionEspecie(TipoAplicacionProducto.USO_GENERAL);
        producto.setActivo(true);
        Long id = productoRepository.save(producto).getId();
        List<Lote> lista = new ArrayList<>();
        for (int i = 0; i < lotes; i++) {
            lista.add(lote(id, "L-%03d".formatted(i), cantidadPorLote, LocalDate.now().plusDays(30L + i), true, LocalDate.now()));
        }
        loteRepository.saveAll(lista);
        return id;
    }

    private void crearLote(Long productoId, String numero, int cantidad, LocalDate vence, boolean activo) {
        crearLote(productoId, numero, cantidad, vence, activo, LocalDate.now());
    }

    private void crearLote(Long productoId, String numero, int cantidad, LocalDate vence, boolean activo, LocalDate ingreso) {
        loteRepository.save(lote(productoId, numero, cantidad, vence, activo, ingreso));
    }

    private Lote lote(Long productoId, String numero, int cantidad, LocalDate vence, boolean activo, LocalDate ingreso) {
        Lote lote = new Lote();
        lote.setCompany(clinica);
        lote.setProducto(productoRepository.getReferenceById(productoId));
        lote.setNumeroLote(numero);
        lote.setFechaVencimiento(vence);
        lote.setFechaIngreso(ingreso);
        lote.setCantidad(cantidad);
        lote.setCantidadInicial(cantidad);
        lote.setActivo(activo);
        return lote;
    }

    private List<Integer> cantidades(Long productoId) {
        return jdbcTemplate.queryForList("SELECT cantidad FROM lote WHERE producto_id = ? ORDER BY fecha_vencimiento, id",
                Integer.class, productoId);
    }

    private List<Integer> salidas(Long productoId) {
        return jdbcTemplate.queryForList("SELECT s.cantidad FROM salida_lote s JOIN lote l ON l.id = s.lote_id "
                + "WHERE s.producto_id = ? ORDER BY l.fecha_vencimiento, l.id", Integer.class, productoId);
    }
}
