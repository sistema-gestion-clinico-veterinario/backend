package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.dto.response.VistaDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolPermisoRepository;
import veterinaria.vargasvet.repository.VentanaRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentanaService {

    private final VentanaRepository ventanaRepository;
    private final RolVentanaPermisoRepository rolVentanaPermisoRepository;
    private final UsuarioPorRolPermisoRepository usuarioPorRolPermisoRepository;

    @Transactional(readOnly = true)
    public List<MenuItemDTO> listarTodas() {
        return ventanaRepository.findByActivoTrueOrderByOrdenAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuItemDTO> listarArbol() {
        return ventanaRepository.findRaices().stream()
                .map(v -> toDTOConHijos(v, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuItemDTO> listarArbolCompleto() {
        return ventanaRepository.findRaicesAll().stream()
                .map(v -> toDTOConHijos(v, true))
                .collect(Collectors.toList());
    }

    private MenuItemDTO toDTO(Ventana v) {
        return MenuItemDTO.builder()
                .id(v.getId())
                .codigo(v.getCodigo())
                .nombre(v.getNombre())
                .icono(v.getIcono())
                .ruta(v.getRuta())
                .activo(v.isActivo())
                .vistas(v.getVistas() != null ? v.getVistas().stream()
                        .filter(Vista::isActivo)
                        .map(vis -> VistaDTO.builder()
                                .id(vis.getId())
                                .codigo(vis.getCodigo())
                                .nombre(vis.getNombre())
                                .ruta(vis.getRuta())
                                .activo(vis.isActivo())
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private MenuItemDTO toDTOConHijos(Ventana v, boolean incluirInactivos) {
        List<MenuItemDTO> hijos = null;
        if (v.getHijos() != null && !v.getHijos().isEmpty()) {
            hijos = v.getHijos().stream()
                    .filter(h -> incluirInactivos || h.isActivo())
                    .map(h -> toDTOConHijos(h, incluirInactivos))
                    .collect(Collectors.toList());
            if (hijos.isEmpty()) hijos = null;
        }
        return MenuItemDTO.builder()
                .id(v.getId())
                .codigo(v.getCodigo())
                .nombre(v.getNombre())
                .icono(v.getIcono())
                .ruta(v.getRuta())
                .activo(v.isActivo())
                .hijos(hijos)
                .vistas(v.getVistas() != null ? v.getVistas().stream()
                        .filter(vis -> incluirInactivos || vis.isActivo())
                        .map(vis -> VistaDTO.builder()
                                .id(vis.getId())
                                .codigo(vis.getCodigo())
                                .nombre(vis.getNombre())
                                .ruta(vis.getRuta())
                                .activo(vis.isActivo())
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    @Transactional
    public Ventana crear(Ventana ventana) {
        if (ventanaRepository.existsByCodigo(ventana.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una ventana con el código: " + ventana.getCodigo());
        }
        return ventanaRepository.save(ventana);
    }

    @Transactional
    public Ventana actualizar(Integer id, Ventana datos) {
        Ventana ventana = ventanaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));

        ventana.setNombre(datos.getNombre());
        ventana.setIcono(datos.getIcono());
        ventana.setRuta(datos.getRuta());
        ventana.setOrden(datos.getOrden());
        ventana.setDescripcion(datos.getDescripcion());
        ventana.setActivo(datos.isActivo());

        if (datos.getParent() != null && datos.getParent().getId() != null) {
            Ventana padre = ventanaRepository.findById(datos.getParent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ventana padre no encontrada"));
            ventana.setParent(padre);
        } else {
            ventana.setParent(null);
        }

        return ventanaRepository.save(ventana);
    }

    @Transactional
    public void eliminar(Integer id) {
        Ventana ventana = ventanaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada"));

        if (ventana.getHijos() != null && !ventana.getHijos().isEmpty()) {
            throw new IllegalArgumentException(
                    "No se puede eliminar una ventana que tiene sub-ventanas. Elimina primero sus hijos.");
        }

        rolVentanaPermisoRepository.deleteByVentanaId(id);
        usuarioPorRolPermisoRepository.deleteByVentanaId(id);
        ventanaRepository.delete(ventana);
    }
}
