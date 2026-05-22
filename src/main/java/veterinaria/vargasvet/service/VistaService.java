package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.dto.request.VistaRequestDTO;
import veterinaria.vargasvet.dto.response.VistaDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.VistaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VistaService {

    private final VistaRepository vistaRepository;

    @Transactional(readOnly = true)
    public List<VistaDTO> listarTodas() {
        return vistaRepository.findByActivoTrue().stream()
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
        if (vistaRepository.existsByCodigo(request.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una vista con el código: " + request.getCodigo());
        }

        Vista vista = new Vista();
        vista.setCodigo(request.getCodigo().toUpperCase().trim().replace(" ", "_"));
        vista.setNombre(request.getNombre());
        vista.setRuta(resolveRuta(request));
        vista.setGrupo(request.getGrupo());
        vista.setActivo(request.isActivo());

        return toDTO(vistaRepository.save(vista));
    }

    @Transactional
    public VistaDTO actualizar(Integer id, VistaRequestDTO request) {
        Vista vista = vistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada"));

        vista.setNombre(request.getNombre());
        if (request.getRuta() != null && !request.getRuta().isBlank()) {
            vista.setRuta(request.getRuta());
        }
        vista.setGrupo(request.getGrupo());
        vista.setActivo(request.isActivo());

        return toDTO(vistaRepository.save(vista));
    }

    @Transactional
    public void eliminar(Integer id) {
        Vista vista = vistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada"));
        vistaRepository.delete(vista);
    }

    public VistaDTO toDTO(Vista v) {
        return VistaDTO.builder()
                .id(v.getId())
                .codigo(v.getCodigo())
                .nombre(v.getNombre())
                .ruta(v.getRuta())
                .grupo(v.getGrupo())
                .orden(v.getOrden())
                .activo(v.isActivo())
                .build();
    }

    private String resolveRuta(VistaRequestDTO request) {
        if (request.getRuta() != null && !request.getRuta().isBlank()) {
            return request.getRuta();
        }
        return "/" + request.getCodigo().toLowerCase().trim().replace("vista_", "").replace("_", "-");
    }
}
