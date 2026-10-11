package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.dto.request.VistaRequestDTO;
import veterinaria.vargasvet.dto.response.VistaDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import veterinaria.vargasvet.dto.request.VistaReorderDTO;

@Service
@RequiredArgsConstructor
public class VistaService {

    private static final int ORDEN_MAXIMO = 10000;
    private static final int MAXIMO_MOVIMIENTOS_AUDITADOS = 15;
    private static final String MODULO_AUDITORIA = "Catálogo de vistas";
    private static final Pattern GRUPO_VALIDO = Pattern.compile("^[A-Za-z0-9_\\s-]{1,40}$");

    private final VistaRepository vistaRepository;
    private final VentanaRepository ventanaRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<VistaDTO> listarTodas() {
        return vistaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VistaDTO> listarPorGrupo(String grupo) {
        return vistaRepository.findByGrupo(grupo).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public VistaDTO crear(VistaRequestDTO request) {
        String codigo = normalizarCodigo(request.getCodigo());
        if (vistaRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe una vista con el código: " + codigo);
        }

        Vista vista = new Vista();
        vista.setCodigo(codigo);
        vista.setNombre(request.getNombre());
        vista.setRuta(resolveRuta(request));
        vista.setGrupo(request.getGrupo());
        vista.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        vista.setOrdenGrupo(request.getOrdenGrupo());
        vista.setActivo(request.isActivo());
        vista.setIcono(request.getIcono());
        vista.setVentana(resolveVentana(request.getVentanaId()));

        Vista guardada = vistaRepository.save(vista);
        auditLogService.log("REGISTRAR_VISTA", MODULO_AUDITORIA,
                "Se registró la vista " + guardada.getCodigo() + " \"" + guardada.getNombre() + "\" en "
                        + describirModulo(guardada.getVentana()) + ", posición " + guardada.getOrden());
        return toDTO(guardada);
    }

    @Transactional
    public VistaDTO actualizar(Integer id, VistaRequestDTO request) {
        Vista vista = vistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada"));

        String nombreAnterior = vista.getNombre();
        Ventana moduloAnterior = vista.getVentana();
        Integer ordenAnterior = vista.getOrden();
        boolean activaAnterior = vista.isActivo();
        String iconoAnterior = vista.getIcono();

        vista.setNombre(request.getNombre());
        if (request.getRuta() != null && !request.getRuta().isBlank()) {
            vista.setRuta(request.getRuta());
        }
        vista.setGrupo(request.getGrupo());
        vista.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        vista.setOrdenGrupo(request.getOrdenGrupo());
        vista.setActivo(request.isActivo());
        vista.setIcono(request.getIcono());
        vista.setVentana(resolveVentana(request.getVentanaId()));

        Vista guardada = vistaRepository.save(vista);
        List<String> cambios = new ArrayList<>();
        if (!Objects.equals(nombreAnterior, guardada.getNombre())) {
            cambios.add("nombre \"" + nombreAnterior + "\" → \"" + guardada.getNombre() + "\"");
        }
        if (!Objects.equals(idDe(moduloAnterior), idDe(guardada.getVentana()))) {
            cambios.add("módulo " + describirModulo(moduloAnterior) + " → " + describirModulo(guardada.getVentana()));
        }
        if (!Objects.equals(ordenAnterior, guardada.getOrden())) {
            cambios.add("posición " + ordenAnterior + " → " + guardada.getOrden());
        }
        if (activaAnterior != guardada.isActivo()) {
            cambios.add(guardada.isActivo() ? "reactivada" : "retirada");
        }
        if (!Objects.equals(iconoAnterior, guardada.getIcono())) {
            cambios.add("ícono " + (iconoAnterior == null ? "ninguno" : iconoAnterior) + " → "
                    + (guardada.getIcono() == null ? "ninguno" : guardada.getIcono()));
        }
        if (!cambios.isEmpty()) {
            auditLogService.log("MODIFICAR_VISTA", MODULO_AUDITORIA,
                    "Se modificó la vista " + guardada.getCodigo() + ": " + String.join("; ", cambios));
        }
        return toDTO(guardada);
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    private Ventana resolveVentana(Integer ventanaId) {
        if (ventanaId == null) {
            return null;
        }
        return ventanaRepository.findById(ventanaId)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));
    }

    @Transactional
    public void eliminar(Integer id) {
        Vista vista = vistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada"));
        boolean estabaActiva = vista.isActivo();
        vista.setActivo(false);
        vistaRepository.save(vista);
        if (estabaActiva) {
            auditLogService.log("RETIRAR_VISTA", MODULO_AUDITORIA,
                    "Se retiró la vista " + vista.getCodigo() + " \"" + vista.getNombre() + "\"");
        }
    }

    @Transactional
    public void reordenar(List<VistaReorderDTO> reorders) {
        if (reorders == null || reorders.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una vista para reordenar");
        }

        Set<Integer> idsRecibidos = new HashSet<>();
        Map<Integer, Vista> vistas = new HashMap<>();
        Map<Integer, Ventana> modulos = new HashMap<>();
        for (VistaReorderDTO cambio : reorders) {
            validar(cambio, idsRecibidos);
            vistas.put(cambio.getId(), vistaRepository.findById(cambio.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada: " + cambio.getId())));
            if (cambio.getVentanaId() != null && !modulos.containsKey(cambio.getVentanaId())) {
                modulos.put(cambio.getVentanaId(), resolveVentana(cambio.getVentanaId()));
            }
        }

        boolean huboCambios = false;
        List<String> movimientos = new ArrayList<>();
        for (VistaReorderDTO cambio : reorders) {
            Vista vista = vistas.get(cambio.getId());
            Ventana moduloAnterior = vista.getVentana();
            Integer ordenAnterior = vista.getOrden();
            vista.setOrden(cambio.getOrden());
            if (cambio.isSinModulo()) {
                vista.setVentana(null);
            } else if (cambio.getVentanaId() != null) {
                vista.setVentana(modulos.get(cambio.getVentanaId()));
            }
            if (cambio.getGrupo() != null) {
                vista.setGrupo(cambio.getGrupo());
            }
            if (cambio.getOrdenGrupo() != null) {
                vista.setOrdenGrupo(cambio.getOrdenGrupo());
            }
            if (!Objects.equals(idDe(moduloAnterior), idDe(vista.getVentana()))) {
                huboCambios = true;
                movimientos.add(vista.getCodigo() + ": " + describirModulo(moduloAnterior) + " → "
                        + describirModulo(vista.getVentana()));
            } else if (!Objects.equals(ordenAnterior, vista.getOrden())) {
                huboCambios = true;
            }
        }
        vistaRepository.flush();

        if (huboCambios) {
            auditLogService.log("REORDENAR_VISTAS", MODULO_AUDITORIA, detalleReordenamiento(reorders.size(), movimientos));
        }
    }

    private String detalleReordenamiento(int total, List<String> movimientos) {
        StringBuilder detalle = new StringBuilder("Se reorganizaron ").append(total).append(" vista(s) del menú");
        if (!movimientos.isEmpty()) {
            List<String> visibles = movimientos.subList(0, Math.min(movimientos.size(), MAXIMO_MOVIMIENTOS_AUDITADOS));
            detalle.append(". Cambiaron de módulo: ").append(String.join("; ", visibles));
            if (movimientos.size() > visibles.size()) {
                detalle.append("; y ").append(movimientos.size() - visibles.size()).append(" más");
            }
        }
        return detalle.toString();
    }

    private Integer idDe(Ventana ventana) {
        return ventana != null ? ventana.getId() : null;
    }

    private String describirModulo(Ventana ventana) {
        return ventana != null ? ventana.getNombre() : "sin módulo";
    }

    private void validar(VistaReorderDTO cambio, Set<Integer> idsRecibidos) {
        if (cambio == null || cambio.getId() == null) {
            throw new IllegalArgumentException("Cada cambio debe indicar la vista");
        }
        if (!idsRecibidos.add(cambio.getId())) {
            throw new IllegalArgumentException("La vista " + cambio.getId() + " está repetida en la solicitud");
        }
        if (cambio.getOrden() == null || cambio.getOrden() < 0 || cambio.getOrden() > ORDEN_MAXIMO) {
            throw new IllegalArgumentException("El orden debe estar entre 0 y " + ORDEN_MAXIMO);
        }
        if (cambio.isSinModulo() && cambio.getVentanaId() != null) {
            throw new IllegalArgumentException("No se puede indicar un módulo y a la vez dejar la vista sin módulo");
        }
        if (cambio.getGrupo() != null && !GRUPO_VALIDO.matcher(cambio.getGrupo()).matches()) {
            throw new IllegalArgumentException("El grupo solo puede contener letras, números, guiones y espacios (máximo 40)");
        }
    }

    public VistaDTO toDTO(Vista v) {
        return VistaDTO.builder()
                .id(v.getId())
                .codigo(v.getCodigo())
                .nombre(v.getNombre())
                .ruta(v.getRuta())
                .grupo(v.getGrupo())
                .orden(v.getOrden())
                .ordenGrupo(v.getOrdenGrupo())
                .activo(v.isActivo())
                .icono(v.getIcono())
                .ventanaId(v.getVentana() != null ? v.getVentana().getId() : null)
                .ventanaNombre(v.getVentana() != null ? v.getVentana().getNombre() : null)
                .build();
    }

    private String resolveRuta(VistaRequestDTO request) {
        if (request.getRuta() != null && !request.getRuta().isBlank()) {
            return request.getRuta();
        }
        return "/" + request.getCodigo().toLowerCase().trim().replace("vista_", "").replace("_", "-");
    }
}
