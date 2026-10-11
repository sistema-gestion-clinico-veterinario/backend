package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.dto.request.VentaLibreItemRequest;
import veterinaria.vargasvet.dto.request.VentaLibreRequest;
import veterinaria.vargasvet.dto.response.VentaLibreResponse;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.SalidaLoteRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DataJpaTest
class VentaLibreServiceIntegrationTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private VentaLibreDetalleRepository ventaLibreDetalleRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private SalidaLoteRepository salidaLoteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private VentaLibreServiceImpl ventaLibreService;

    @BeforeEach
    void setUp() {
        ventaLibreService = new VentaLibreServiceImpl(
                purchaseRepository, ventaLibreDetalleRepository, productoRepository,
                mock(veterinaria.vargasvet.repository.ApoderadoRepository.class),
                companyRepository, mock(AuditLogService.class), mock(CajaService.class),
                new InventarioStockServiceImpl(productoRepository, loteRepository, salidaLoteRepository),
                usuarioRepository
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Registra una venta libre anónima, sin cita, y descuenta el stock del producto")
    void registrarVentaLibreAnonimaDescuentaStock() {
        Company company = crearCompany();
        Producto producto = crearProducto(company, "Alimento Premium 3kg", "25.00", 5);
        setCompanyContext(company.getId());

        VentaLibreRequest request = new VentaLibreRequest();
        request.setCompanyId(company.getId());
        request.setItems(List.of(item(producto.getId(), 2)));
        request.setMetodoPago(MetodoPago.EFECTIVO);
        request.setMontoRecibido(new BigDecimal("50.00"));

        VentaLibreResponse response = ventaLibreService.registrar(request);
        entityManager.clear();

        assertThat(response.getTotal()).isEqualByComparingTo("50.00");
        assertThat(response.getClienteNombre()).isNull();
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(3);

        var purchase = purchaseRepository.findById(response.getId()).orElseThrow();
        assertThat(purchase.getCita()).isNull();
        assertThat(purchase.getTipoPurchase()).isEqualTo(TipoPurchase.TIENDA);
        assertThat(purchase.getCompany().getId()).isEqualTo(company.getId());
    }

    @Test
    @DisplayName("Registra una venta libre con nombre de cliente no registrado")
    void registrarVentaLibreConClienteNoRegistrado() {
        Company company = crearCompany();
        Producto producto = crearProducto(company, "Shampoo antipulgas", "30.00", 3);
        setCompanyContext(company.getId());

        VentaLibreRequest request = new VentaLibreRequest();
        request.setCompanyId(company.getId());
        request.setClienteNombre("Juan Pérez (sin cuenta)");
        request.setItems(List.of(item(producto.getId(), 1)));
        request.setMetodoPago(MetodoPago.YAPE);

        VentaLibreResponse response = ventaLibreService.registrar(request);

        assertThat(response.getClienteNombre()).isEqualTo("Juan Pérez (sin cuenta)");
    }

    @Test
    @DisplayName("Rechaza vender un producto que pertenece a otra empresa")
    void rechazaProductoDeOtraEmpresa() {
        Company companyA = crearCompany();
        Company companyB = crearCompany();
        Producto productoDeB = crearProducto(companyB, "Producto de otra empresa", "10.00", 10);
        setCompanyContext(companyA.getId());

        VentaLibreRequest request = new VentaLibreRequest();
        request.setCompanyId(companyA.getId());
        request.setItems(List.of(item(productoDeB.getId(), 1)));
        request.setMetodoPago(MetodoPago.YAPE);

        assertThatThrownBy(() -> ventaLibreService.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece a esta empresa");

        assertThat(productoRepository.findById(productoDeB.getId()).orElseThrow().getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("Rechaza la venta si el stock no alcanza para la cantidad pedida")
    void rechazaVentaConStockInsuficiente() {
        Company company = crearCompany();
        Producto producto = crearProducto(company, "Producto con poco stock", "15.00", 1);
        setCompanyContext(company.getId());

        VentaLibreRequest request = new VentaLibreRequest();
        request.setCompanyId(company.getId());
        request.setItems(List.of(item(producto.getId(), 2)));
        request.setMetodoPago(MetodoPago.YAPE);

        assertThatThrownBy(() -> ventaLibreService.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("La venta deja registrado a quien la hizo, distinto del cliente que compra")
    void laVentaGuardaAlUsuarioQueLaRegistra() {
        Company company = crearCompany();
        Producto producto = crearProducto(company, "Collar", "25.00", 10);
        Usuario cajero = setCompanyContext(company.getId());

        VentaLibreResponse response = ventaLibreService.registrar(ventaDe(company, producto));
        entityManager.flush();
        entityManager.clear();

        Purchase guardada = purchaseRepository.findById(response.getId()).orElseThrow();
        assertThat(guardada.getUser().getId()).isEqualTo(cajero.getId());
        assertThat(guardada.getCliente()).isNull();
    }

    @Test
    @DisplayName("El reporte de ventas con alcance propio solo devuelve lo que registró ese usuario")
    void elReporteDeVentasConAlcancePropioNoMuestraLasVentasDeOtros() {
        Company company = crearCompany();
        Producto producto = crearProducto(company, "Juguete", "25.00", 10);
        Usuario ana = setCompanyContext(company.getId(), "ana@vargasvet.test");
        Long ventaDeAna = ventaLibreService.registrar(ventaDe(company, producto)).getId();
        Usuario beto = setCompanyContext(company.getId(), "beto@vargasvet.test");
        Long ventaDeBeto = ventaLibreService.registrar(ventaDe(company, producto)).getId();
        entityManager.flush();
        entityManager.clear();
        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(1);

        List<Long> soloAna = purchaseRepository.findForSalesReport(company.getId(), TipoPurchase.TIENDA,
                PaymentStatus.PAID, desde, hasta, ana.getId()).stream().map(Purchase::getId).toList();
        List<Long> soloBeto = purchaseRepository.findForSalesReport(company.getId(), TipoPurchase.TIENDA,
                PaymentStatus.PAID, desde, hasta, beto.getId()).stream().map(Purchase::getId).toList();
        List<Long> todas = purchaseRepository.findForSalesReport(company.getId(), TipoPurchase.TIENDA,
                PaymentStatus.PAID, desde, hasta, null).stream().map(Purchase::getId).toList();

        assertThat(soloAna).containsExactly(ventaDeAna);
        assertThat(soloBeto).containsExactly(ventaDeBeto);
        assertThat(todas).containsExactlyInAnyOrder(ventaDeAna, ventaDeBeto);
    }

    private VentaLibreItemRequest item(Long productoId, int cantidad) {
        VentaLibreItemRequest item = new VentaLibreItemRequest();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        return item;
    }

    private Company crearCompany() {
        Company company = new Company();
        company.setName("VargasVet Test");
        company.setSlug("vargasvet-test-" + UUID.randomUUID());
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Producto crearProducto(Company company, String nombre, String precio, int stock) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setCompany(company);
        categoria.setNombre("Alimento");
        categoria = categoriaProductoRepository.save(categoria);

        Producto producto = new Producto();
        producto.setCompany(company);
        producto.setCategoria(categoria);
        producto.setNombre(nombre);
        producto.setSku("PRD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        producto.setPrecio(new BigDecimal(precio));
        producto.setStock(stock);
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private Usuario setCompanyContext(Integer companyId) {
        return setCompanyContext(companyId, "cajero-" + UUID.randomUUID() + "@vargasvet.test");
    }

    private Usuario setCompanyContext(Integer companyId, String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setUsername(email);
        usuario.setCompany(companyRepository.findById(companyId).orElseThrow());
        usuario = usuarioRepository.save(usuario);
        UsuarioPrincipal principal = new UsuarioPrincipal(
                usuario.getId(), email, "n/a", List.of(), companyId,
                null, null, RolePurpose.COMPANY_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return usuario;
    }

    private VentaLibreRequest ventaDe(Company company, Producto producto) {
        VentaLibreRequest request = new VentaLibreRequest();
        request.setCompanyId(company.getId());
        request.setItems(List.of(item(producto.getId(), 1)));
        request.setMetodoPago(MetodoPago.EFECTIVO);
        request.setMontoRecibido(new BigDecimal("100.00"));
        return request;
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }
}
