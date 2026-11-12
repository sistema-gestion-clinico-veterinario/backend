package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.enums.EstadoStockReporte;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.dto.request.ReporteVentasProductosFiltro;
import veterinaria.vargasvet.dto.response.ReporteVentasProductosDTO;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.PurchaseRepository.VentaAgrupada;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository.ProductoVendidoEnElPeriodo;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReporteVentasProductos {

    private static final int MAXIMO_PRODUCTOS_MAS_VENDIDOS = 10;
    private static final String VENTA_SIN_METODO = "SIN ESPECIFICAR";

    private final PurchaseRepository purchaseRepository;
    private final VentaLibreDetalleRepository ventaLibreDetalleRepository;
    private final ProductoRepository productoRepository;
    private final AlcanceReportes alcance;

    private record PeriodoVentas(List<VentaAgrupada> ventas, List<ProductoVendidoEnElPeriodo> productos) {

        static final PeriodoVentas SIN_VENTAS = new PeriodoVentas(List.of(), List.of());

        long cantidadDeVentas() {
            return ventas.stream().mapToLong(VentaAgrupada::getVentas).sum();
        }

        BigDecimal ingresos() {
            return ventas.stream().map(VentaAgrupada::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        long unidades() {
            return productos.stream().mapToLong(ProductoVendidoEnElPeriodo::getCantidad).sum();
        }
    }

    public ReporteVentasProductosDTO obtener(ReporteVentasProductosFiltro filtros) {
        Integer empresa = alcance.resolverEmpresa(filtros.getCompanyId());
        RangoDeReporte rango = RangoDeReporte.validado(filtros.getFechaDesde(), filtros.getFechaHasta(), AppClock.today());

        boolean puedePagos = alcance.tienePermiso("VISTA_PAGOS");
        boolean puedeProductos = alcance.tienePermiso("VISTA_PRODUCTOS");
        boolean alcanceEmpresa = alcance.tieneAlcanceEmpresa();
        Integer registradoPorUsuarioId = alcanceEmpresa ? null : SecurityUtils.getCurrentUserId();
        if (empresa == null) {
            return reporteVacio(rango);
        }

        PeriodoVentas actual = cargarPeriodo(empresa, rango, registradoPorUsuarioId, puedePagos, puedeProductos, filtros);
        PeriodoVentas previo = cargarPeriodo(empresa, rango.anterior(), registradoPorUsuarioId, puedePagos, puedeProductos, filtros);
        // El inventario es un recurso común de la clínica y no tiene "propietario" individual.
        // Solo se muestran sus métricas cuando el rol tiene alcance COMPANY.
        boolean puedeInventarioEmpresa = puedeProductos && alcanceEmpresa;
        List<Producto> productos = puedeInventarioEmpresa
                ? productoRepository.findByCompanyIdAndActivoTrue(empresa).stream()
                        .filter(p -> coincideProducto(p, filtros))
                        .filter(p -> coincideEstadoStock(p, filtros.getEstadoStock()))
                        .toList()
                : List.of();

        long sinStock = productos.stream().filter(p -> p.getStock() != null && p.getStock() <= 0).count();
        long stockBajo = productos.stream().filter(p -> p.getStock() != null && p.getStockMinimo() != null
                && p.getStock() > 0 && p.getStock() <= p.getStockMinimo()).count();

        return ReporteVentasProductosDTO.builder().fechaDesde(rango.desde()).fechaHasta(rango.hasta())
                .resumen(ReporteVentasProductosDTO.Resumen.builder()
                        .ventas(puedePagos ? actual.cantidadDeVentas() : null)
                        .ingresos(puedePagos ? actual.ingresos() : null)
                        .unidadesVendidas(puedeProductos && puedePagos ? actual.unidades() : null)
                        .ticketPromedio(puedePagos ? calcularTicket(actual.ingresos(), actual.cantidadDeVentas()) : null)
                        .productosStockBajo(puedeInventarioEmpresa ? stockBajo : null)
                        .productosSinStock(puedeInventarioEmpresa ? sinStock : null).build())
                .resumenAnterior(resumen(previo, puedePagos, puedeProductos))
                .ventasPorMetodo(puedePagos ? agruparVentas(actual.ventas(),
                        v -> v.getMetodoPago() == null ? VENTA_SIN_METODO : v.getMetodoPago().name()) : null)
                .ventasPorDia(puedePagos ? agruparVentas(actual.ventas(), VentaAgrupada::getDia) : null)
                .productosMasVendidos(puedeProductos && puedePagos ? productosMasVendidos(actual.productos()) : null)
                .alertasStock(puedeInventarioEmpresa ? alertasDeStock(productos, filtros.getEstadoStock()) : null).build();
    }

    private ReporteVentasProductosDTO reporteVacio(RangoDeReporte rango) {
        ReporteVentasProductosDTO.Resumen resumenVacio = ReporteVentasProductosDTO.Resumen.builder()
                .ventas(0L).ingresos(BigDecimal.ZERO).unidadesVendidas(0L).ticketPromedio(BigDecimal.ZERO)
                .productosStockBajo(0L).productosSinStock(0L).build();
        return ReporteVentasProductosDTO.builder().fechaDesde(rango.desde()).fechaHasta(rango.hasta())
                .resumen(resumenVacio).resumenAnterior(resumenVacio)
                .ventasPorMetodo(List.of()).ventasPorDia(List.of())
                .productosMasVendidos(List.of()).alertasStock(List.of()).build();
    }

    private PeriodoVentas cargarPeriodo(Integer companyId, RangoDeReporte rango, Integer registradoPorUsuarioId,
                                        boolean puedePagos, boolean puedeProductos,
                                        ReporteVentasProductosFiltro filtros) {
        if (!puedePagos) return PeriodoVentas.SIN_VENTAS;
        boolean filtraProducto = tieneFiltroProducto(filtros);
        if (filtraProducto && !puedeProductos) return PeriodoVentas.SIN_VENTAS;

        List<VentaAgrupada> ventas = filtraProducto
                ? ventaLibreDetalleRepository.resumirVentasConProductos(companyId, TipoPurchase.TIENDA, PaymentStatus.PAID,
                        rango.inicio(), rango.finExclusivo(), registradoPorUsuarioId, filtros.getMetodoPago(),
                        filtros.getProductoId(), filtros.getCategoriaId(), filtros.getMarcaId())
                : purchaseRepository.resumirVentas(companyId, TipoPurchase.TIENDA, PaymentStatus.PAID,
                        rango.inicio(), rango.finExclusivo(), registradoPorUsuarioId, filtros.getMetodoPago());
        if (ventas.isEmpty() || !puedeProductos) {
            return new PeriodoVentas(ventas, List.of());
        }
        List<ProductoVendidoEnElPeriodo> productos = ventaLibreDetalleRepository.resumirProductosVendidos(
                companyId, TipoPurchase.TIENDA, PaymentStatus.PAID, rango.inicio(), rango.finExclusivo(),
                registradoPorUsuarioId, filtros.getMetodoPago(),
                filtros.getProductoId(), filtros.getCategoriaId(), filtros.getMarcaId());
        return new PeriodoVentas(ventas, productos);
    }

    private ReporteVentasProductosDTO.Resumen resumen(PeriodoVentas periodo, boolean puedePagos, boolean puedeProductos) {
        BigDecimal ingresos = periodo.ingresos();
        return ReporteVentasProductosDTO.Resumen.builder()
                .ventas(puedePagos ? periodo.cantidadDeVentas() : null)
                .ingresos(puedePagos ? ingresos : null)
                .unidadesVendidas(puedePagos && puedeProductos ? periodo.unidades() : null)
                .ticketPromedio(puedePagos ? calcularTicket(ingresos, periodo.cantidadDeVentas()) : null)
                .productosStockBajo(null).productosSinStock(null).build();
    }

    private <K extends Comparable<? super K>> List<ReporteVentasProductosDTO.ItemMonto> agruparVentas(
            List<VentaAgrupada> filas, Function<VentaAgrupada, K> clave) {
        Map<K, List<VentaAgrupada>> agrupadas = filas.stream()
                .collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.toList()));
        return agrupadas.entrySet().stream().map(e ->
                ReporteVentasProductosDTO.ItemMonto.builder().nombre(String.valueOf(e.getKey()))
                        .cantidad(e.getValue().stream().mapToLong(VentaAgrupada::getVentas).sum())
                        .monto(e.getValue().stream().map(VentaAgrupada::getMonto)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)).build()).toList();
    }

    private List<ReporteVentasProductosDTO.ProductoVendido> productosMasVendidos(List<ProductoVendidoEnElPeriodo> filas) {
        return filas.stream()
                .sorted(Comparator.comparing(ProductoVendidoEnElPeriodo::getCantidad).reversed()
                        .thenComparing(ProductoVendidoEnElPeriodo::getProductoId))
                .limit(MAXIMO_PRODUCTOS_MAS_VENDIDOS)
                .map(fila -> ReporteVentasProductosDTO.ProductoVendido.builder()
                        .sku(fila.getSku()).nombre(fila.getNombre())
                        .cantidad(fila.getCantidad()).monto(fila.getMonto()).build())
                .toList();
    }

    private List<ReporteVentasProductosDTO.AlertaStock> alertasDeStock(List<Producto> productos,
                                                                       EstadoStockReporte estadoFiltrado) {
        return productos.stream()
                .filter(p -> estadoFiltrado == EstadoStockReporte.NORMAL
                        || (p.getStock() != null && p.getStockMinimo() != null && p.getStock() <= p.getStockMinimo()))
                .sorted(Comparator.comparing(Producto::getStock))
                .map(p -> ReporteVentasProductosDTO.AlertaStock.builder().sku(p.getSku()).nombre(p.getNombre())
                        .categoria(p.getCategoria() == null ? null : p.getCategoria().getNombre())
                        .stock(p.getStock()).stockMinimo(p.getStockMinimo()).build()).toList();
    }

    private boolean tieneFiltroProducto(ReporteVentasProductosFiltro filtros) {
        return filtros.getCategoriaId() != null || filtros.getProductoId() != null || filtros.getMarcaId() != null;
    }

    private boolean coincideProducto(Producto producto, ReporteVentasProductosFiltro filtros) {
        if (producto == null) return false;
        if (filtros.getProductoId() != null && !filtros.getProductoId().equals(producto.getId())) return false;
        if (filtros.getCategoriaId() != null && (producto.getCategoria() == null
                || !filtros.getCategoriaId().equals(producto.getCategoria().getId()))) return false;
        return filtros.getMarcaId() == null || (producto.getMarcaProducto() != null
                && filtros.getMarcaId().equals(producto.getMarcaProducto().getId()));
    }

    private boolean coincideEstadoStock(Producto producto, EstadoStockReporte estado) {
        if (estado == null) return true;
        int stock = producto.getStock() == null ? 0 : producto.getStock();
        int minimo = producto.getStockMinimo() == null ? 0 : producto.getStockMinimo();
        return switch (estado) {
            case SIN_STOCK -> stock <= 0;
            case BAJO -> stock > 0 && stock <= minimo;
            case NORMAL -> stock > minimo;
        };
    }

    private BigDecimal calcularTicket(BigDecimal ingresos, long ventas) {
        return ventas == 0 ? BigDecimal.ZERO : ingresos.divide(BigDecimal.valueOf(ventas), 2, RoundingMode.HALF_UP);
    }
}
