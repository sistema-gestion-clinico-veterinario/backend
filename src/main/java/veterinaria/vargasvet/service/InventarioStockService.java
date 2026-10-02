package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.entity.Producto;

public interface InventarioStockService {
    void descontar(Producto producto, int cantidad, String referenciaTipo, Long referenciaId);
    void restaurar(Producto producto, int cantidad, String referenciaTipo, Long referenciaId);
}
