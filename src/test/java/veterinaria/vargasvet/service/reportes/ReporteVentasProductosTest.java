package veterinaria.vargasvet.service.reportes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.dto.request.ReporteVentasProductosFiltro;
import veterinaria.vargasvet.dto.response.ReporteVentasProductosDTO;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.PurchaseRepository.VentaAgrupada;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository.ProductoVendidoEnElPeriodo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteVentasProductosTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 7, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 7, 31);

    @Mock private PurchaseRepository purchaseRepository;
    @Mock private VentaLibreDetalleRepository ventaLibreDetalleRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private AlcanceReportes alcance;

    private ReporteVentasProductos reporte;

    @BeforeEach
    void preparar() {
        reporte = new ReporteVentasProductos(purchaseRepository, ventaLibreDetalleRepository, productoRepository, alcance);
        lenient().when(alcance.resolverEmpresa(any())).thenReturn(1);
        lenient().when(alcance.tienePermiso(any())).thenReturn(true);
        lenient().when(alcance.tieneAlcanceEmpresa()).thenReturn(true);
    }

    @Test
    void sinFiltroDeProductoCuentaLasVentasDirectamenteDeLasCompras() {
        ventasSinFiltro(venta(MetodoPago.EFECTIVO, "2026-07-10", 2, "100.00"), venta(MetodoPago.YAPE, "2026-07-11", 1, "50.00"));
        productos(producto(1L, "A", 3, "150.00"));

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getResumen().getVentas()).isEqualTo(3);
        assertThat(resultado.getResumen().getIngresos()).isEqualByComparingTo("150.00");
        assertThat(resultado.getResumen().getUnidadesVendidas()).isEqualTo(3);
        assertThat(resultado.getResumen().getTicketPromedio()).isEqualByComparingTo("50.00");
        verify(ventaLibreDetalleRepository, never()).resumirVentasConProductos(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void conFiltroDeProductoUsaLasVentasCalculadasSobreLosDetalles() {
        VentaAgrupada ventaDelPeriodo = venta(MetodoPago.EFECTIVO, "2026-07-10", 1, "30.00");
        when(ventaLibreDetalleRepository.resumirVentasConProductos(
                anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(ventaDelPeriodo), List.of());
        productos(producto(1L, "A", 2, "30.00"));

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().categoriaId(10L).build());

        assertThat(resultado.getResumen().getIngresos()).isEqualByComparingTo("30.00");
        verify(purchaseRepository, never()).resumirVentas(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void agrupaPorMetodoSumandoLosDiasYOrdenaAlfabeticamenteConLasVentasSinMetodoAlFinal() {
        ventasSinFiltro(
                venta(MetodoPago.YAPE, "2026-07-12", 1, "10.00"),
                venta(MetodoPago.EFECTIVO, "2026-07-10", 2, "20.00"),
                venta(MetodoPago.EFECTIVO, "2026-07-11", 3, "30.00"),
                venta(null, "2026-07-10", 1, "5.00"));
        productos();

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getVentasPorMetodo()).extracting(ReporteVentasProductosDTO.ItemMonto::getNombre)
                .containsExactly("EFECTIVO", "SIN ESPECIFICAR", "YAPE");
        assertThat(resultado.getVentasPorMetodo().get(0).getCantidad()).isEqualTo(5);
        assertThat(resultado.getVentasPorMetodo().get(0).getMonto()).isEqualByComparingTo("50.00");
    }

    @Test
    void agrupaPorDiaSumandoLosMetodosYOrdenaCronologicamente() {
        ventasSinFiltro(
                venta(MetodoPago.YAPE, "2026-07-12", 1, "10.00"),
                venta(MetodoPago.EFECTIVO, "2026-07-10", 2, "20.00"),
                venta(MetodoPago.YAPE, "2026-07-10", 1, "5.00"));
        productos();

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getVentasPorDia()).extracting(ReporteVentasProductosDTO.ItemMonto::getNombre)
                .containsExactly("2026-07-10", "2026-07-12");
        assertThat(resultado.getVentasPorDia().get(0).getCantidad()).isEqualTo(3);
        assertThat(resultado.getVentasPorDia().get(0).getMonto()).isEqualByComparingTo("25.00");
    }

    @Test
    void losProductosMasVendidosSeOrdenanPorCantidadYAIgualCantidadPorId() {
        ventasSinFiltro(venta(MetodoPago.EFECTIVO, "2026-07-10", 1, "10.00"));
        productos(producto(9L, "I", 5, "1.00"), producto(3L, "C", 5, "1.00"), producto(7L, "G", 8, "1.00"), producto(1L, "A", 2, "1.00"));

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getProductosMasVendidos()).extracting(ReporteVentasProductosDTO.ProductoVendido::getSku)
                .containsExactly("G", "C", "I", "A");
    }

    @Test
    void soloMuestraLosDiezProductosMasVendidos() {
        ventasSinFiltro(venta(MetodoPago.EFECTIVO, "2026-07-10", 1, "10.00"));
        List<ProductoVendidoEnElPeriodo> filas = new ArrayList<>();
        for (long i = 1; i <= 12; i++) {
            filas.add(producto(i, "P" + i, i, "1.00"));
        }
        productos(filas.toArray(new ProductoVendidoEnElPeriodo[0]));

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getProductosMasVendidos()).hasSize(10);
        assertThat(resultado.getProductosMasVendidos().get(0).getSku()).isEqualTo("P12");
        assertThat(resultado.getProductosMasVendidos().get(9).getSku()).isEqualTo("P3");
    }

    @Test
    void sinVentasNoConsultaLosProductosVendidosYElTicketEsCero() {
        ventasSinFiltro();

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getResumen().getVentas()).isZero();
        assertThat(resultado.getResumen().getTicketPromedio()).isEqualByComparingTo("0");
        assertThat(resultado.getProductosMasVendidos()).isEmpty();
        verify(ventaLibreDetalleRepository, never()).resumirProductosVendidos(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void elTicketPromedioSeRedondeaADosDecimales() {
        ventasSinFiltro(venta(MetodoPago.EFECTIVO, "2026-07-10", 3, "100.00"));
        productos();

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getResumen().getTicketPromedio()).isEqualByComparingTo("33.33");
    }

    @Test
    void sinPermisoDePagosNoConsultaVentasNiMuestraCifras() {
        when(alcance.tienePermiso("VISTA_PAGOS")).thenReturn(false);

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getResumen().getVentas()).isNull();
        assertThat(resultado.getResumen().getIngresos()).isNull();
        assertThat(resultado.getVentasPorMetodo()).isNull();
        assertThat(resultado.getVentasPorDia()).isNull();
        assertThat(resultado.getProductosMasVendidos()).isNull();
        verifyNoInteractions(purchaseRepository, ventaLibreDetalleRepository);
    }

    @Test
    void sinPermisoDeProductosMuestraLasVentasPeroNoLasUnidadesNiLosProductos() {
        when(alcance.tienePermiso("VISTA_PRODUCTOS")).thenReturn(false);
        ventasSinFiltro(venta(MetodoPago.EFECTIVO, "2026-07-10", 2, "100.00"));

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getResumen().getVentas()).isEqualTo(2);
        assertThat(resultado.getResumen().getUnidadesVendidas()).isNull();
        assertThat(resultado.getProductosMasVendidos()).isNull();
        verify(ventaLibreDetalleRepository, never()).resumirProductosVendidos(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void conFiltroDeProductoYSinPermisoDeProductosNoHayVentas() {
        when(alcance.tienePermiso("VISTA_PRODUCTOS")).thenReturn(false);

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().marcaId(4L).build());

        assertThat(resultado.getResumen().getVentas()).isZero();
        verifyNoInteractions(purchaseRepository, ventaLibreDetalleRepository);
    }

    @Test
    void sinEmpresaDevuelveElReporteVacio() {
        when(alcance.resolverEmpresa(any())).thenReturn(null);

        ReporteVentasProductosDTO resultado = reporte.obtener(filtro().build());

        assertThat(resultado.getResumen().getVentas()).isZero();
        assertThat(resultado.getVentasPorMetodo()).isEmpty();
        verifyNoInteractions(purchaseRepository, ventaLibreDetalleRepository, productoRepository);
    }

    @Test
    void consultaElPeriodoActualYElAnteriorDeIgualDuracion() {
        ventasSinFiltro();

        reporte.obtener(filtro().build());

        verify(purchaseRepository).resumirVentas(any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(DESDE.atStartOfDay()), org.mockito.ArgumentMatchers.eq(HASTA.plusDays(1).atStartOfDay()), any(), any());
        verify(purchaseRepository).resumirVentas(any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(DESDE.minusDays(31).atStartOfDay()), org.mockito.ArgumentMatchers.eq(DESDE.atStartOfDay()), any(), any());
    }

    private ReporteVentasProductosFiltro.ReporteVentasProductosFiltroBuilder filtro() {
        return ReporteVentasProductosFiltro.builder().fechaDesde(DESDE).fechaHasta(HASTA);
    }

    private void ventasSinFiltro(VentaAgrupada... actuales) {
        when(purchaseRepository.resumirVentas(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(actuales), List.of());
    }

    private void productos(ProductoVendidoEnElPeriodo... filas) {
        lenient().when(ventaLibreDetalleRepository.resumirProductosVendidos(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(filas), List.of());
        lenient().when(productoRepository.findByCompanyIdAndActivoTrue(anyInt())).thenReturn(List.of());
    }

    private VentaAgrupada venta(MetodoPago metodo, String dia, long ventas, String monto) {
        VentaAgrupada fila = mock(VentaAgrupada.class);
        lenient().when(fila.getMetodoPago()).thenReturn(metodo);
        lenient().when(fila.getDia()).thenReturn(LocalDate.parse(dia));
        lenient().when(fila.getVentas()).thenReturn(ventas);
        lenient().when(fila.getMonto()).thenReturn(new BigDecimal(monto));
        return fila;
    }

    private ProductoVendidoEnElPeriodo producto(Long id, String sku, long cantidad, String monto) {
        ProductoVendidoEnElPeriodo fila = mock(ProductoVendidoEnElPeriodo.class);
        lenient().when(fila.getProductoId()).thenReturn(id);
        lenient().when(fila.getSku()).thenReturn(sku);
        lenient().when(fila.getNombre()).thenReturn("Producto " + sku);
        lenient().when(fila.getCantidad()).thenReturn(cantidad);
        lenient().when(fila.getMonto()).thenReturn(new BigDecimal(monto));
        return fila;
    }
}
