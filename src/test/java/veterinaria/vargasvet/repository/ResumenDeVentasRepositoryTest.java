package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.MarcaProducto;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.VentaLibreDetalle;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;
import veterinaria.vargasvet.domain.enums.TipoControlStock;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.repository.PurchaseRepository.VentaAgrupada;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository.ProductoVendidoEnElPeriodo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class ResumenDeVentasRepositoryTest {

    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 7, 1, 0, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2026, 8, 1, 0, 0);

    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private VentaLibreDetalleRepository detalleRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaProductoRepository categoriaRepository;
    @Autowired private MarcaProductoRepository marcaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EntityManager entityManager;

    private Company clinica;
    private Company otraClinica;
    private Usuario ana;
    private Usuario beto;
    private CategoriaProducto categoria1;
    private MarcaProducto marca1;
    private Producto productoA;
    private Producto productoB;
    private Producto productoC;

    @BeforeEach
    void datos() {
        clinica = clinica("Clinica Uno");
        otraClinica = clinica("Clinica Dos");
        ana = usuario(clinica, "ana");
        beto = usuario(clinica, "beto");
        categoria1 = categoria(clinica, "Alimentos");
        CategoriaProducto categoria2 = categoria(clinica, "Juguetes");
        marca1 = marca(clinica, "Marca uno");
        MarcaProducto marca2 = marca(clinica, "Marca dos");
        productoA = producto(clinica, "A", categoria1, marca1);
        productoB = producto(clinica, "B", categoria1, marca2);
        productoC = producto(clinica, "C", categoria2, marca1);

        venta(clinica, ana, MetodoPago.EFECTIVO, "2026-07-10T10:00", "100.00", linea(productoA, 2, "60.00"), linea(productoB, 1, "40.00"));
        venta(clinica, ana, MetodoPago.EFECTIVO, "2026-07-10T15:00", "30.00", linea(productoA, 1, "30.00"));
        venta(clinica, ana, MetodoPago.YAPE, "2026-07-11T09:00", "50.00", linea(productoC, 2, "50.00"));
        venta(clinica, ana, null, "2026-07-12T09:00", "20.00", linea(productoB, 1, "20.00"));
        venta(clinica, ana, MetodoPago.EFECTIVO, "2026-07-01T00:00", "10.00", linea(productoA, 1, "10.00"));
        venta(clinica, beto, MetodoPago.EFECTIVO, "2026-07-10T11:00", "70.00", linea(productoC, 1, "70.00"));

        ventaExcluida(clinica, TipoPurchase.TIENDA, PaymentStatus.PENDING, "2026-07-10T10:00");
        ventaExcluida(clinica, TipoPurchase.SERVICIO_CITA, PaymentStatus.PAID, "2026-07-10T10:00");
        ventaExcluida(otraClinica, TipoPurchase.TIENDA, PaymentStatus.PAID, "2026-07-10T10:00");
        ventaExcluida(clinica, TipoPurchase.TIENDA, PaymentStatus.PAID, "2026-06-30T23:59");
        ventaExcluida(clinica, TipoPurchase.TIENDA, PaymentStatus.PAID, "2026-08-01T00:00");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void agrupaLasVentasPorMetodoYDiaSinContarLasQueNoCorresponden() {
        List<VentaAgrupada> filas = resumirVentas(null, null);

        assertThat(filas).extracting(f -> f.getMetodoPago(), f -> f.getDia().toString(), VentaAgrupada::getVentas, f -> f.getMonto().intValue())
                .containsExactlyInAnyOrder(
                        tuple(MetodoPago.EFECTIVO, "2026-07-01", 1L, 10),
                        tuple(MetodoPago.EFECTIVO, "2026-07-10", 3L, 200),
                        tuple(MetodoPago.YAPE, "2026-07-11", 1L, 50),
                        tuple(null, "2026-07-12", 1L, 20));
    }

    @Test
    void elPeriodoIncluyeElInstanteDeInicioYExcluyeElDeFin() {
        List<VentaAgrupada> filas = resumirVentas(null, null);

        assertThat(filas).extracting(f -> f.getDia().toString()).contains("2026-07-01").doesNotContain("2026-06-30", "2026-08-01");
    }

    @Test
    void filtraLasVentasRegistradasPorUnUsuario() {
        List<VentaAgrupada> filas = resumirVentas(ana.getId(), null);

        assertThat(filas.stream().mapToLong(VentaAgrupada::getVentas).sum()).isEqualTo(5);
        assertThat(filas).filteredOn(f -> f.getDia().toString().equals("2026-07-10"))
                .singleElement().satisfies(f -> {
                    assertThat(f.getVentas()).isEqualTo(2);
                    assertThat(f.getMonto()).isEqualByComparingTo("130.00");
                });
    }

    @Test
    void filtraPorMetodoDePago() {
        List<VentaAgrupada> filas = resumirVentas(null, MetodoPago.YAPE);

        assertThat(filas).singleElement().satisfies(f -> {
            assertThat(f.getMetodoPago()).isEqualTo(MetodoPago.YAPE);
            assertThat(f.getVentas()).isEqualTo(1);
        });
    }

    @Test
    void unaVentaConVariasLineasDelProductoFiltradoSeCuentaUnaSolaVezYSumaSoloEsasLineas() {
        List<VentaAgrupada> filas = conProductos(null, null, null, categoria1.getId(), null);

        assertThat(filas).extracting(f -> f.getMetodoPago(), f -> f.getDia().toString(), VentaAgrupada::getVentas, f -> f.getMonto().intValue())
                .containsExactlyInAnyOrder(
                        tuple(MetodoPago.EFECTIVO, "2026-07-01", 1L, 10),
                        tuple(MetodoPago.EFECTIVO, "2026-07-10", 2L, 130),
                        tuple(null, "2026-07-12", 1L, 20));
    }

    @Test
    void filtraPorProductoSumandoSoloLasLineasDeEseProducto() {
        List<VentaAgrupada> filas = conProductos(null, productoB.getId(), null, null, null);

        assertThat(filas).extracting(f -> f.getDia().toString(), VentaAgrupada::getVentas, f -> f.getMonto().intValue())
                .containsExactlyInAnyOrder(tuple("2026-07-10", 1L, 40), tuple("2026-07-12", 1L, 20));
    }

    @Test
    void filtraPorMarca() {
        List<VentaAgrupada> filas = conProductos(null, null, marca1.getId(), null, null);

        assertThat(filas.stream().mapToLong(VentaAgrupada::getVentas).sum()).isEqualTo(5);
        assertThat(filas.stream().map(VentaAgrupada::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("220.00");
    }

    @Test
    void combinaLosFiltrosDeCategoriaYMarca() {
        List<VentaAgrupada> filas = conProductos(null, null, marca1.getId(), categoria1.getId(), null);

        assertThat(filas.stream().map(VentaAgrupada::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
    }

    @Test
    void conFiltroDeProductoTambienRespetaElMetodoDePagoYElUsuario() {
        List<VentaAgrupada> porMetodo = conProductos(null, null, marca1.getId(), null, MetodoPago.YAPE);
        List<VentaAgrupada> porUsuario = conProductos(beto.getId(), null, marca1.getId(), null, null);

        assertThat(porMetodo).singleElement().satisfies(f -> assertThat(f.getVentas()).isEqualTo(1));
        assertThat(porUsuario).singleElement().satisfies(f -> assertThat(f.getMonto()).isEqualByComparingTo("70.00"));
    }

    @Test
    void unProductoSinVentasNoDevuelveFilas() {
        assertThat(conProductos(null, 999_999L, null, null, null)).isEmpty();
    }

    @Test
    void sumaLasUnidadesYElMontoPorProducto() {
        List<ProductoVendidoEnElPeriodo> filas = productos(null, null, null, null, null);

        assertThat(filas).extracting(ProductoVendidoEnElPeriodo::getSku, ProductoVendidoEnElPeriodo::getCantidad,
                        f -> f.getMonto().intValue())
                .containsExactlyInAnyOrder(tuple("A", 4L, 100), tuple("B", 2L, 60), tuple("C", 3L, 120));
    }

    @Test
    void losProductosVendidosRespetanLosFiltrosDeLaCompra() {
        assertThat(productos(ana.getId(), null, null, null, null))
                .extracting(ProductoVendidoEnElPeriodo::getSku, ProductoVendidoEnElPeriodo::getCantidad)
                .containsExactlyInAnyOrder(tuple("A", 4L), tuple("B", 2L), tuple("C", 2L));
        assertThat(productos(null, MetodoPago.EFECTIVO, null, null, null))
                .extracting(ProductoVendidoEnElPeriodo::getSku, ProductoVendidoEnElPeriodo::getCantidad)
                .containsExactlyInAnyOrder(tuple("A", 4L), tuple("B", 1L), tuple("C", 1L));
    }

    @Test
    void losProductosVendidosRespetanLosFiltrosDeProducto() {
        assertThat(productos(null, null, null, categoria1.getId(), null))
                .extracting(ProductoVendidoEnElPeriodo::getSku).containsExactlyInAnyOrder("A", "B");
        assertThat(productos(null, null, productoC.getId(), null, null))
                .extracting(ProductoVendidoEnElPeriodo::getSku).containsExactly("C");
    }

    @Test
    void noMezclaVentasDeOtraClinica() {
        List<VentaAgrupada> filas = purchaseRepository.resumirVentas(otraClinica.getId(), TipoPurchase.TIENDA,
                PaymentStatus.PAID, DESDE, HASTA, null, null);

        assertThat(filas).singleElement().satisfies(f -> assertThat(f.getVentas()).isEqualTo(1));
    }

    private List<VentaAgrupada> resumirVentas(Integer usuario, MetodoPago metodo) {
        return purchaseRepository.resumirVentas(clinica.getId(), TipoPurchase.TIENDA, PaymentStatus.PAID,
                DESDE, HASTA, usuario, metodo);
    }

    private List<VentaAgrupada> conProductos(Integer usuario, Long productoId, Long marcaId, Long categoriaId, MetodoPago metodo) {
        return detalleRepository.resumirVentasConProductos(clinica.getId(), TipoPurchase.TIENDA, PaymentStatus.PAID,
                DESDE, HASTA, usuario, metodo, productoId, categoriaId, marcaId);
    }

    private List<ProductoVendidoEnElPeriodo> productos(Integer usuario, MetodoPago metodo, Long productoId,
                                                       Long categoriaId, Long marcaId) {
        return detalleRepository.resumirProductosVendidos(clinica.getId(), TipoPurchase.TIENDA, PaymentStatus.PAID,
                DESDE, HASTA, usuario, metodo, productoId, categoriaId, marcaId);
    }

    private record Linea(Producto producto, int cantidad, String subtotal) { }

    private Linea linea(Producto producto, int cantidad, String subtotal) {
        return new Linea(producto, cantidad, subtotal);
    }

    private void venta(Company empresa, Usuario registrador, MetodoPago metodo, String fecha, String total, Linea... lineas) {
        Purchase compra = compra(empresa, registrador, TipoPurchase.TIENDA, PaymentStatus.PAID, metodo, fecha, total);
        for (Linea linea : lineas) {
            VentaLibreDetalle detalle = new VentaLibreDetalle();
            detalle.setPurchase(compra);
            detalle.setProducto(linea.producto());
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(new BigDecimal("10.00"));
            detalle.setSubtotal(new BigDecimal(linea.subtotal()));
            detalleRepository.save(detalle);
        }
    }

    private void ventaExcluida(Company empresa, TipoPurchase tipo, PaymentStatus estado, String fecha) {
        Purchase compra = compra(empresa, ana, tipo, estado, MetodoPago.EFECTIVO, fecha, "999.00");
        VentaLibreDetalle detalle = new VentaLibreDetalle();
        detalle.setPurchase(compra);
        detalle.setProducto(productoA);
        detalle.setCantidad(9);
        detalle.setPrecioUnitario(new BigDecimal("10.00"));
        detalle.setSubtotal(new BigDecimal("999.00"));
        detalleRepository.save(detalle);
    }

    private Purchase compra(Company empresa, Usuario registrador, TipoPurchase tipo, PaymentStatus estado,
                            MetodoPago metodo, String fecha, String total) {
        Purchase compra = new Purchase();
        compra.setCompany(empresa);
        compra.setUser(registrador);
        compra.setTipoPurchase(tipo);
        compra.setPaymentStatus(estado);
        compra.setMetodoPago(metodo);
        compra.setCreatedAt(LocalDateTime.parse(fecha));
        compra.setTotal(new BigDecimal(total));
        return purchaseRepository.save(compra);
    }

    private Company clinica(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("c-" + UUID.randomUUID());
        company.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Usuario usuario(Company company, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setEmail(nombre + "-" + UUID.randomUUID() + "@clinica.test");
        usuario.setUsername(nombre + "-" + UUID.randomUUID());
        usuario.setNombre(nombre);
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
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

    private Producto producto(Company empresa, String sku, CategoriaProducto categoria, MarcaProducto marca) {
        Producto producto = new Producto();
        producto.setCompany(empresa);
        producto.setNombre("Producto " + sku);
        producto.setCategoria(categoria);
        producto.setMarcaProducto(marca);
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setStock(10);
        producto.setControlStock(TipoControlStock.DIRECTO);
        producto.setStockMinimo(2);
        producto.setSku(sku);
        producto.setRequiereReceta(false);
        producto.setAplicacionEspecie(TipoAplicacionProducto.USO_GENERAL);
        producto.setEspecies(Set.of(EspecieMascota.PERRO));
        producto.setActivo(true);
        return productoRepository.save(producto);
    }
}
