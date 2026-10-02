package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Lote;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.SalidaLote;
import veterinaria.vargasvet.domain.enums.TipoControlStock;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.SalidaLoteRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioStockServiceImplTest {

    @Mock ProductoRepository productoRepository;
    @Mock LoteRepository loteRepository;
    @Mock SalidaLoteRepository salidaLoteRepository;
    @InjectMocks InventarioStockServiceImpl service;

    @Test
    void descuentaPorFefoYRegistraCadaLoteUtilizado() {
        Producto producto = productoPorLotes();
        Lote primero = lote(11L, "L-PRIMERO", 3, LocalDate.now().plusMonths(1), producto);
        Lote segundo = lote(12L, "L-SEGUNDO", 8, LocalDate.now().plusMonths(3), producto);
        when(loteRepository.findDisponiblesFefoForUpdate(eq(10L), any(LocalDate.class)))
                .thenReturn(List.of(primero, segundo));

        service.descontar(producto, 5, "VENTA_LIBRE_DETALLE", 99L);

        assertThat(primero.getCantidad()).isZero();
        assertThat(segundo.getCantidad()).isEqualTo(6);
        ArgumentCaptor<SalidaLote> captor = ArgumentCaptor.forClass(SalidaLote.class);
        verify(salidaLoteRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(SalidaLote::getCantidad).containsExactly(3, 2);
        assertThat(captor.getAllValues()).allSatisfy(salida -> {
            assertThat(salida.getReferenciaTipo()).isEqualTo("VENTA_LIBRE_DETALLE");
            assertThat(salida.getReferenciaId()).isEqualTo(99L);
        });
        verify(productoRepository, never()).descontarStock(any(), any());
    }

    @Test
    void noModificaNingunLoteCuandoElTotalDisponibleEsInsuficiente() {
        Producto producto = productoPorLotes();
        Lote lote = lote(11L, "L-UNICO", 2, LocalDate.now().plusMonths(1), producto);
        when(loteRepository.findDisponiblesFefoForUpdate(eq(10L), any(LocalDate.class)))
                .thenReturn(List.of(lote));

        assertThatThrownBy(() -> service.descontar(producto, 3, "VENTA_LIBRE_DETALLE", 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock por lotes insuficiente");

        assertThat(lote.getCantidad()).isEqualTo(2);
        verify(loteRepository, never()).save(any());
        verify(salidaLoteRepository, never()).save(any());
    }

    private Producto productoPorLotes() {
        Company company = new Company();
        company.setId(2);
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setCompany(company);
        producto.setNombre("Vacuna");
        producto.setControlStock(TipoControlStock.LOTES);
        return producto;
    }

    private Lote lote(Long id, String numero, int cantidad, LocalDate vencimiento, Producto producto) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setCompany(producto.getCompany());
        lote.setNumeroLote(numero);
        lote.setCantidad(cantidad);
        lote.setCantidadInicial(cantidad);
        lote.setFechaVencimiento(vencimiento);
        lote.setActivo(true);
        return lote;
    }
}
