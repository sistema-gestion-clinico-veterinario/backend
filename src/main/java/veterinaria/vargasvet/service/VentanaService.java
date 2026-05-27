package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.VentanaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VentanaService {

    private final VentanaRepository ventanaRepository;

    @Transactional(readOnly = true)
    public List<Ventana> listarTodas() {
        return ventanaRepository.findByActivoTrueOrderByOrdenAsc();
    }

    @Transactional(readOnly = true)
    public Ventana obtenerPorId(Integer id) {
        return ventanaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));
    }

    @Transactional
    public Ventana crear(Ventana ventana) {
        ventana.setCodigo(normalizarCodigo(ventana.getCodigo()));
        ventana.setGrupo(normalizarTexto(ventana.getGrupo()));
        if (ventanaRepository.findByCodigo(ventana.getCodigo()) != null) {
            throw new IllegalArgumentException("Ya existe una ventana con el código: " + ventana.getCodigo());
        }
        return ventanaRepository.save(ventana);
    }

    @Transactional
    public Ventana actualizar(Integer id, Ventana datos) {
        Ventana ventana = ventanaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));

        ventana.setNombre(datos.getNombre());
        ventana.setGrupo(normalizarTexto(datos.getGrupo()));
        ventana.setOrden(datos.getOrden());
        ventana.setActivo(datos.isActivo());

        return ventanaRepository.save(ventana);
    }

    @Transactional
    public void eliminar(Integer id) {
        Ventana ventana = ventanaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));

        ventanaRepository.delete(ventana);
    }
    private String normalizarCodigo(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    private String normalizarTexto(String value) {
        return value == null ? null : value.trim();
    }
}
