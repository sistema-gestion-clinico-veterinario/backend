package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.MarcaProducto;
import veterinaria.vargasvet.domain.entity.Producto;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductoBuscarRepositoryTest {

    private static final Pageable PAGINA = PageRequest.of(0, 20, Sort.by("nombre").ascending());

    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaProductoRepository categoriaRepository;
    @Autowired private MarcaProductoRepository marcaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private EntityManager entityManager;

    private Company clinica;
    private Company otraClinica;
    private CategoriaProducto alimentos;
    private CategoriaProducto medicinas;
    private MarcaProducto royal;
    private MarcaProducto hills;

    @BeforeEach
    void datos() {
        clinica = clinica("A");
        otraClinica = clinica("B");
        alimentos = categoria(clinica, "Alimentos");
        medicinas = categoria(clinica, "Medicinas");
        royal = marca(clinica, "Royal Canin");
        hills = marca(clinica, "Hills");
    }

    @Test
    void devuelveSoloLosProductosDeLaEmpresaPedida() {
        Producto propio = producto(clinica, "Alimento adulto", "SKU-1", null, alimentos, royal, null, true);
        producto(otraClinica, "Alimento adulto", "SKU-9", null, categoria(otraClinica, "X"), marca(otraClinica, "Y"), null, true);

        assertThat(buscar(clinica, null, null, null).getContent()).extracting(Producto::getId).containsExactly(propio.getId());
    }

    @Test
    void losOrdenaPorNombreAscendente() {
        Producto c = producto(clinica, "Collar", "SKU-C", null, alimentos, royal, null, true);
        Producto a = producto(clinica, "Antipulgas", "SKU-A", null, alimentos, royal, null, true);
        Producto b = producto(clinica, "Bravecto", "SKU-B", null, alimentos, royal, null, true);

        assertThat(buscar(clinica, null, null, null).getContent()).extracting(Producto::getId)
                .containsExactly(a.getId(), b.getId(), c.getId());
    }

    @Test
    void buscaPorNombreSinImportarMayusculas() {
        Producto buscado = producto(clinica, "Shampoo Sensible", "SKU-1", null, alimentos, royal, null, true);
        producto(clinica, "Collar", "SKU-2", null, alimentos, royal, null, true);

        assertThat(buscar(clinica, "SHAMPOO", null, null).getContent()).extracting(Producto::getId).containsExactly(buscado.getId());
        assertThat(buscar(clinica, "ensib", null, null).getContent()).extracting(Producto::getId).containsExactly(buscado.getId());
    }

    @Test
    void buscaPorSku() {
        Producto buscado = producto(clinica, "Collar", "PRD-ABC123", null, alimentos, royal, null, true);
        producto(clinica, "Correa", "PRD-XYZ999", null, alimentos, royal, null, true);

        assertThat(buscar(clinica, "abc12", null, null).getContent()).extracting(Producto::getId).containsExactly(buscado.getId());
    }

    @Test
    void buscaPorNombreDeLaMarcaDelCatalogo() {
        Producto deRoyal = producto(clinica, "Alimento adulto", "SKU-1", null, alimentos, royal, null, true);
        producto(clinica, "Alimento senior", "SKU-2", null, alimentos, hills, null, true);

        assertThat(buscar(clinica, "royal", null, null).getContent()).extracting(Producto::getId).containsExactly(deRoyal.getId());
    }

    @Test
    void buscaPorCodigoDeBarras() {
        Producto buscado = producto(clinica, "Collar", "SKU-1", "7701234567890", alimentos, royal, null, true);
        producto(clinica, "Correa", "SKU-2", null, alimentos, royal, null, true);

        assertThat(buscar(clinica, "4567", null, null).getContent()).extracting(Producto::getId).containsExactly(buscado.getId());
    }

    @Test
    void unTextoVacioONuloNoFiltra() {
        producto(clinica, "Collar", "SKU-1", null, alimentos, royal, null, true);
        producto(clinica, "Correa", "SKU-2", null, alimentos, royal, null, true);

        assertThat(buscar(clinica, "", null, null).getTotalElements()).isEqualTo(2);
        assertThat(buscar(clinica, null, null, null).getTotalElements()).isEqualTo(2);
    }

    @Test
    void filtraPorCategoriaYPorEstado() {
        Producto activoDeAlimentos = producto(clinica, "Alimento", "SKU-1", null, alimentos, royal, null, true);
        producto(clinica, "Antibiotico", "SKU-2", null, medicinas, royal, null, true);
        Producto inactivo = producto(clinica, "Snack", "SKU-3", null, alimentos, royal, null, false);

        assertThat(buscar(clinica, null, alimentos.getId(), null).getContent()).extracting(Producto::getId)
                .containsExactlyInAnyOrder(activoDeAlimentos.getId(), inactivo.getId());
        assertThat(buscar(clinica, null, alimentos.getId(), true).getContent()).extracting(Producto::getId)
                .containsExactly(activoDeAlimentos.getId());
        assertThat(buscar(clinica, null, null, false).getContent()).extracting(Producto::getId)
                .containsExactly(inactivo.getId());
    }

    @Test
    void combinaTextoCategoriaYEstado() {
        Producto buscado = producto(clinica, "Alimento adulto", "SKU-1", null, alimentos, royal, null, true);
        producto(clinica, "Alimento adulto 2", "SKU-2", null, medicinas, royal, null, true);
        producto(clinica, "Alimento adulto 3", "SKU-3", null, alimentos, royal, null, false);

        assertThat(buscar(clinica, "adulto", alimentos.getId(), true).getContent()).extracting(Producto::getId)
                .containsExactly(buscado.getId());
    }

    @Test
    void paginaYCuentaElTotal() {
        for (int i = 1; i <= 5; i++) {
            producto(clinica, "Producto " + i, "SKU-" + i, null, alimentos, royal, null, true);
        }

        Page<Producto> segunda = productoRepository.buscar(clinica.getId(), null, null, null,
                PageRequest.of(1, 2, Sort.by("nombre").ascending()));

        assertThat(segunda.getTotalElements()).isEqualTo(5);
        assertThat(segunda.getTotalPages()).isEqualTo(3);
        assertThat(segunda.getContent()).extracting(Producto::getNombre).containsExactly("Producto 3", "Producto 4");
    }

    @Test
    void incluyeLosProductosSinMarcaDelCatalogo() {
        Producto sinMarca = producto(clinica, "Collar", "SKU-1", null, alimentos, null, null, true);
        Producto conMarca = producto(clinica, "Correa", "SKU-2", null, alimentos, royal, null, true);

        Page<Producto> pagina = buscar(clinica, null, null, null);

        assertThat(pagina.getContent()).extracting(Producto::getId).containsExactly(sinMarca.getId(), conMarca.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }

    @Test
    void buscaPorLaMarcaEnTextoCuandoNoHayMarcaDelCatalogo() {
        Producto antiguo = producto(clinica, "Collar", "SKU-1", null, alimentos, null, "Generica Vet", true);
        producto(clinica, "Correa", "SKU-2", null, alimentos, royal, null, true);

        assertThat(buscar(clinica, "generica", null, null).getContent()).extracting(Producto::getId).containsExactly(antiguo.getId());
    }

    private Page<Producto> buscar(Company empresa, String texto, Long categoriaId, Boolean activo) {
        entityManager.flush();
        entityManager.clear();
        return productoRepository.buscar(empresa.getId(), texto, categoriaId, activo, PAGINA);
    }

    private Company clinica(String sufijo) {
        Company company = new Company();
        company.setName("Clinica " + sufijo);
        company.setSlug("clinica-" + sufijo.toLowerCase() + "-" + UUID.randomUUID());
        company.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private CategoriaProducto categoria(Company empresa, String nombre) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setCompany(empresa);
        categoria.setNombre(nombre);
        categoria.setActivo(true);
        return categoriaRepository.save(categoria);
    }

    private MarcaProducto marca(Company empresa, String nombre) {
        MarcaProducto marca = new MarcaProducto();
        marca.setCompany(empresa);
        marca.setNombre(nombre);
        marca.setActivo(true);
        return marcaRepository.save(marca);
    }

    private Producto producto(Company empresa, String nombre, String sku, String codigoBarras, CategoriaProducto categoria,
                              MarcaProducto marca, String marcaEnTexto, boolean activo) {
        Producto producto = new Producto();
        producto.setCompany(empresa);
        producto.setNombre(nombre);
        producto.setSku(sku);
        producto.setCodigoBarras(codigoBarras);
        producto.setCategoria(categoria);
        producto.setMarcaProducto(marca);
        producto.setMarca(marcaEnTexto);
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setStock(5);
        producto.setStockMinimo(1);
        producto.setActivo(activo);
        return productoRepository.save(producto);
    }
}
