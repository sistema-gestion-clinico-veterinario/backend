package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.VentaLibreDetalle;
import veterinaria.vargasvet.domain.enums.EstadoStockReporte;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.dto.request.ReporteVentasProductosFiltro;
import veterinaria.vargasvet.dto.response.ReporteVentasProductosDTO;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.VentaLibreDetalleRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private record PeriodoVentas(List<Purchase> ventas, List<VentaLibreDetalle> detalles) {}

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

        RangoDeReporte anterior = rango.anterior();
        PeriodoVentas actual = cargarPeriodo(empresa, rango, registradoPorUsuarioId, puedePagos, puedeProductos, filtros);
        PeriodoVentas previo = cargarPeriodo(empresa, anterior, registradoPorUsuarioId, puedePagos, puedeProductos, filtros);
        List<Purchase> ventas = actual.ventas();
        List<VentaLibreDetalle> detalles = actual.detalles();
        boolean filtraProducto = tieneFiltroProducto(filtros);
        // El inventario es un recurso común de la clínica y no tiene "propietario" individual.
        // Solo se muestran sus métricas cuando el rol tiene alcance COMPANY.
        boolean puedeInventarioEmpresa = puedeProductos && alcanceEmpresa;
        List<Producto> productos = puedeInventarioEmpresa
                ? productoRepository.findByCompanyIdAndActivoTrue(empresa).stream()
                        .filter(p -> coincideProducto(p, filtros))
                        .filter(p -> coincideEstadoStock(p, filtros.getEstadoStock()))
                        .toList()
                : List.of();

        Map<Long, BigDecimal> montoPorVenta = montoPorVenta(ventas, detalles, filtraProducto);
        BigDecimal ingresos = sumarMontos(montoPorVenta);
        long sinStock = productos.stream().filter(p -> p.getStock() != null && p.getStock() <= 0).count();
        long stockBajo = productos.stream().filter(p -> p.getStock() != null && p.getStockMinimo() != null
                && p.getStock() > 0 && p.getStock() <= p.getStockMinimo()).count();

        return ReporteVentasProductosDTO.builder().fechaDesde(rango.desde()).fechaHasta(rango.hasta())
                .resumen(ReporteVentasProductosDTO.Resumen.builder()
                        .ventas(puedePagos ? (long) ventas.size() : null)
                        .ingresos(puedePagos ? ingresos : null)
                        .unidadesVendidas(puedeProductos && puedePagos ? sumarUnidades(detalles) : null)
                        .ticketPromedio(puedePagos ? calcularTicket(ingresos, ventas.size()) : null)
                        .productosStockBajo(puedeInventarioEmpresa ? stockBajo : null)
                        .productosSinStock(puedeInventarioEmpresa ? sinStock : null).build())
                .resumenAnterior(resumen(previo, filtraProducto, puedePagos, puedeProductos))
                .ventasPorMetodo(puedePagos ? agruparVentas(ventas,
                        v -> v.getMetodoPago() == null ? VENTA_SIN_METODO : v.getMetodoPago().name(), montoPorVenta) : null)
                .ventasPorDia(puedePagos ? agruparVentas(ventas, v -> v.getCreatedAt().toLocalDate(), montoPorVenta) : null)
                .productosMasVendidos(puedeProductos && puedePagos ? productosMasVendidos(detalles) : null)
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
        if (!puedePagos) return new PeriodoVentas(List.of(), List.of());
        List<Purchase> ventas = purchaseRepository.findForSalesReport(companyId, TipoPurchase.TIENDA,
                        PaymentStatus.PAID, rango.inicio(), rango.finExclusivo(), registradoPorUsuarioId)
                .stream()
                .filter(v -> filtros.getMetodoPago() == null || filtros.getMetodoPago() == v.getMetodoPago())
                .toList();
        List<VentaLibreDetalle> detalles = puedeProductos && !ventas.isEmpty()
                ? ventaLibreDetalleRepository.findForReportByPurchaseIds(ventas.stream().map(Purchase::getId).toList())
                        .stream().filter(d -> coincideProducto(d.getProducto(), filtros)).toList()
                : List.of();
        if (tieneFiltroProducto(filtros)) {
            Set<Long> ventasCoincidentes = detalles.stream().map(d -> d.getPurchase().getId()).collect(Collectors.toSet());
            ventas = ventas.stream().filter(v -> ventasCoincidentes.contains(v.getId())).toList();
        }
        return new PeriodoVentas(ventas, detalles);
    }

    private ReporteVentasProductosDTO.Resumen resumen(PeriodoVentas periodo, boolean filtraProducto,
                                                      boolean puedePagos, boolean puedeProductos) {
        Map<Long, BigDecimal> montos = montoPorVenta(periodo.ventas(), periodo.detalles(), filtraProducto);
        BigDecimal ingresos = sumarMontos(montos);
        return ReporteVentasProductosDTO.Resumen.builder()
                .ventas(puedePagos ? (long) periodo.ventas().size() : null)
                .ingresos(puedePagos ? ingresos : null)
                .unidadesVendidas(puedePagos && puedeProductos ? sumarUnidades(periodo.detalles()) : null)
                .ticketPromedio(puedePagos ? calcularTicket(ingresos, periodo.ventas().size()) : null)
                .productosStockBajo(null).productosSinStock(null).build();
    }

    private <K extends Comparable<? super K>> List<ReporteVentasProductosDTO.ItemMonto> agruparVentas(
            List<Purchase> ventas, Function<Purchase, K> clave, Map<Long, BigDecimal> montoPorVenta) {
        Map<K, List<Purchase>> agrupadas = ventas.stream()
                .collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.toList()));
        return agrupadas.entrySet().stream().map(e ->
                ReporteVentasProductosDTO.ItemMonto.builder().nombre(String.valueOf(e.getKey()))
                        .cantidad((long) e.getValue().size())
                        .monto(e.getValue().stream().map(v -> montoPorVenta.getOrDefault(v.getId(), BigDecimal.ZERO))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)).build()).toList();
    }

    private List<ReporteVentasProductosDTO.ProductoVendido> productosMasVendidos(List<VentaLibreDetalle> detalles) {
        Map<Long, List<VentaLibreDetalle>> porProducto = detalles.stream().collect(Collectors.groupingBy(
                d -> d.getProducto().getId()));
        return porProducto.values().stream().map(lista -> {
            Producto p = lista.get(0).getProducto();
            return ReporteVentasProductosDTO.ProductoVendido.builder().sku(p.getSku()).nombre(p.getNombre())
                    .cantidad(lista.stream().mapToLong(d -> d.getCantidad() == null ? 0 : d.getCantidad()).sum())
                    .monto(lista.stream().map(VentaLibreDetalle::getSubtotal).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)).build();
        }).sorted(Comparator.comparing(ReporteVentasProductosDTO.ProductoVendido::getCantidad).reversed())
                .limit(MAXIMO_PRODUCTOS_MAS_VENDIDOS).toList();
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

    private Map<Long, BigDecimal> montoPorVenta(List<Purchase> ventas, List<VentaLibreDetalle> detalles,
                                                boolean filtraProducto) {
        if (!filtraProducto) {
            return ventas.stream().collect(Collectors.toMap(Purchase::getId,
                    v -> v.getTotal() == null ? BigDecimal.ZERO : v.getTotal()));
        }
        return detalles.stream().collect(Collectors.groupingBy(d -> d.getPurchase().getId(), Collectors.reducing(
                BigDecimal.ZERO, d -> d.getSubtotal() == null ? BigDecimal.ZERO : d.getSubtotal(), BigDecimal::add)));
    }

    private BigDecimal sumarMontos(Map<Long, BigDecimal> montos) {
        return montos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private long sumarUnidades(List<VentaLibreDetalle> detalles) {
        return detalles.stream().mapToLong(d -> d.getCantidad() == null ? 0 : d.getCantidad()).sum();
    }

    private BigDecimal calcularTicket(BigDecimal ingresos, int ventas) {
        return ventas == 0 ? BigDecimal.ZERO : ingresos.divide(BigDecimal.valueOf(ventas), 2, RoundingMode.HALF_UP);
    }
}
