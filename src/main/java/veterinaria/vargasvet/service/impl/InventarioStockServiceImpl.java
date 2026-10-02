package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Lote;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.SalidaLote;
import veterinaria.vargasvet.domain.enums.TipoControlStock;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.SalidaLoteRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.InventarioStockService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioStockServiceImpl implements InventarioStockService {
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final SalidaLoteRepository salidaLoteRepository;

    @Override
    @Transactional
    public void descontar(Producto producto, int cantidad, String referenciaTipo, Long referenciaId) {
        if (producto.getControlStock() != TipoControlStock.LOTES) {
            if (productoRepository.descontarStock(producto.getId(), cantidad) == 0) {
                throw new IllegalArgumentException("Stock insuficiente para «" + producto.getNombre() + "»");
            }
            return;
        }

        List<Lote> lotes = loteRepository.findDisponiblesFefoForUpdate(producto.getId(), LocalDate.now());
        int disponible = lotes.stream().mapToInt(Lote::getCantidad).sum();
        if (disponible < cantidad) {
            throw new IllegalArgumentException("Stock por lotes insuficiente para «" + producto.getNombre() + "»");
        }

        int pendiente = cantidad;
        for (Lote lote : lotes) {
            if (pendiente == 0) break;
            int tomado = Math.min(lote.getCantidad(), pendiente);
            lote.setCantidad(lote.getCantidad() - tomado);
            lote.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
            loteRepository.save(lote);

            SalidaLote salida = new SalidaLote();
            salida.setCompany(producto.getCompany());
            salida.setProducto(producto);
            salida.setLote(lote);
            salida.setReferenciaTipo(referenciaTipo);
            salida.setReferenciaId(referenciaId);
            salida.setCantidad(tomado);
            salida.setCantidadRepuesta(0);
            salida.setCreatedBy(SecurityUtils.getCurrentUserEmail());
            salida.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
            salidaLoteRepository.save(salida);
            pendiente -= tomado;
        }
    }

    @Override
    @Transactional
    public void restaurar(Producto producto, int cantidad, String referenciaTipo, Long referenciaId) {
        if (producto.getControlStock() != TipoControlStock.LOTES) {
            productoRepository.restaurarStock(producto.getId(), cantidad);
            return;
        }

        List<SalidaLote> salidas = salidaLoteRepository
                .findByReferenciaTipoAndReferenciaIdOrderByIdDesc(referenciaTipo, referenciaId);
        int pendiente = cantidad;
        for (SalidaLote salida : salidas) {
            if (pendiente == 0) break;
            int sinReponer = salida.getCantidad() - salida.getCantidadRepuesta();
            if (sinReponer <= 0) continue;
            int repuesto = Math.min(sinReponer, pendiente);
            Lote lote = salida.getLote();
            lote.setCantidad(lote.getCantidad() + repuesto);
            lote.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
            salida.setCantidadRepuesta(salida.getCantidadRepuesta() + repuesto);
            salida.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
            loteRepository.save(lote);
            salidaLoteRepository.save(salida);
            pendiente -= repuesto;
        }
        if (pendiente != 0) {
            throw new IllegalStateException("No se pudo identificar el lote original de todo el stock a reponer");
        }
    }
}
