package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductoRepositoryIntegrationTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void descontarStockRechazaLaSegundaVentaCuandoSoloQuedaUnaUnidad() {
        Producto producto = crearProductoConStock(1);

        int primeraVenta = productoRepository.descontarStock(producto.getId(), 1);
        int segundaVenta = productoRepository.descontarStock(producto.getId(), 1);
        entityManager.clear();

        assertThat(primeraVenta).isEqualTo(1);
        assertThat(segundaVenta).isEqualTo(0);
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(0);
    }

    @Test
    void descontarStockPermiteVenderHastaAgotarElStockDisponible() {
        Producto producto = crearProductoConStock(5);

        int resultado = productoRepository.descontarStock(producto.getId(), 5);
        entityManager.clear();

        assertThat(resultado).isEqualTo(1);
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(0);
    }

    @Test
    void restaurarStockDevuelveLaCantidadAlCancelarUnaVenta() {
        Producto producto = crearProductoConStock(3);
        productoRepository.descontarStock(producto.getId(), 2);

        productoRepository.restaurarStock(producto.getId(), 2);
        entityManager.clear();

        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void findEspeciesPorProductoIdsDevuelveSoloLasEspeciesDeLosProductosSolicitados() {
        Producto producto = crearProductoConStock(2);
        producto.setAplicacionEspecie(TipoAplicacionProducto.ESPECIES_ESPECIFICAS);
        producto.setEspecies(new HashSet<>(Set.of(EspecieMascota.PERRO, EspecieMascota.GATO)));
        productoRepository.save(producto);

        Producto otro = crearProductoConStock(1);
        otro.setAplicacionEspecie(TipoAplicacionProducto.ESPECIES_ESPECIFICAS);
        otro.setEspecies(new HashSet<>(Set.of(EspecieMascota.AVE)));
        productoRepository.save(otro);

        entityManager.flush();
        entityManager.clear();

        List<Object[]> filas = productoRepository.findEspeciesPorProductoIds(List.of(producto.getId()));

        assertThat(filas).hasSize(2);
        assertThat(filas).allSatisfy(fila ->
                assertThat(((Number) fila[0]).longValue()).isEqualTo(producto.getId()));
        assertThat(filas.stream().map(fila -> (String) fila[1]).toList())
                .containsExactlyInAnyOrder("PERRO", "GATO");
    }

    private Producto crearProductoConStock(int stock) {
        Company company = new Company();
        company.setName("VargasVet Test");
        company.setSlug("vargasvet-test-" + UUID.randomUUID());
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setCompany(company);
        categoria.setNombre("Alimento");
        categoria = categoriaProductoRepository.save(categoria);

        Producto producto = new Producto();
        producto.setCompany(company);
        producto.setCategoria(categoria);
        producto.setNombre("Alimento Premium 3kg");
        producto.setSku("PRD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        producto.setPrecio(new BigDecimal("50.00"));
        producto.setStock(stock);
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }
}
