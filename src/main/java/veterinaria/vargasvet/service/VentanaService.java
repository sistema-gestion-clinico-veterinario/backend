package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.VentanaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VentanaService {

    private static final String MODULO_AUDITORIA = "Catálogo de vistas";

    private final VentanaRepository ventanaRepository;
    private final AuditLogService auditLogService;

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
        Ventana guardada = ventanaRepository.save(ventana);
        auditLogService.log("REGISTRAR_MODULO", MODULO_AUDITORIA,
                "Se registró el módulo " + guardada.getCodigo() + " \"" + guardada.getNombre() + "\"");
        return guardada;
    }

    @Transactional
    public Ventana actualizar(Integer id, Ventana datos) {
        Ventana ventana = ventanaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));

        String nombreAnterior = ventana.getNombre();
        Integer ordenAnterior = ventana.getOrden();
        boolean activaAnterior = ventana.isActivo();

        ventana.setNombre(datos.getNombre());
        ventana.setGrupo(normalizarTexto(datos.getGrupo()));
        ventana.setOrden(datos.getOrden());
        ventana.setActivo(datos.isActivo());

        Ventana guardada = ventanaRepository.save(ventana);
        List<String> cambios = new ArrayList<>();
        if (!Objects.equals(nombreAnterior, guardada.getNombre())) {
            cambios.add("nombre \"" + nombreAnterior + "\" → \"" + guardada.getNombre() + "\"");
        }
        if (!Objects.equals(ordenAnterior, guardada.getOrden())) {
            cambios.add("posición " + ordenAnterior + " → " + guardada.getOrden());
        }
        if (activaAnterior != guardada.isActivo()) {
            cambios.add(guardada.isActivo() ? "reactivado" : "retirado");
        }
        if (!cambios.isEmpty()) {
            auditLogService.log("MODIFICAR_MODULO", MODULO_AUDITORIA,
                    "Se modificó el módulo " + guardada.getCodigo() + ": " + String.join("; ", cambios));
        }
        return guardada;
    }

    @Transactional
    public void eliminar(Integer id) {
        Ventana ventana = ventanaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));
        boolean estabaActivo = ventana.isActivo();
        ventana.setActivo(false);
        ventanaRepository.save(ventana);
        if (estabaActivo) {
            auditLogService.log("RETIRAR_MODULO", MODULO_AUDITORIA,
                    "Se retiró el módulo " + ventana.getCodigo() + " \"" + ventana.getNombre() + "\"");
        }
    }
    private String normalizarCodigo(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    private String normalizarTexto(String value) {
        return value == null ? null : value.trim();
    }
}
