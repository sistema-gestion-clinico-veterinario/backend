package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Raza;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.request.RazaRequest;
import veterinaria.vargasvet.dto.response.RazaResponse;
import veterinaria.vargasvet.repository.RazaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.RazaService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RazaServiceImpl implements RazaService {

    private final RazaRepository razaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RazaResponse> listarPorEspecie(String especie, Long companyId) {
        EspecieMascota enumEspecie = (especie != null && !especie.isBlank())
                ? EspecieMascota.valueOf(especie.toUpperCase()) : null;
        if (companyId != null) {
            return razaRepository.findByCompanyAndEspecie(companyId, enumEspecie)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }
        if (enumEspecie != null) {
            return razaRepository.findByEspecieAndActivoTrueOrderByNombreAsc(enumEspecie)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }
        return razaRepository.findByActivoTrueOrderByNombreAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RazaResponse crear(RazaRequest request, Long companyId) {
        if (razaRepository.existsByNombreIgnoreCaseAndEspecieAndCompanyId(
                request.getNombre(), request.getEspecie(), companyId)) {
            throw new IllegalArgumentException("Ya existe una raza con ese nombre para la especie seleccionada");
        }

        Raza raza = new Raza();
        raza.setNombre(request.getNombre());
        raza.setDescripcion(request.getDescripcion());
        raza.setEspecie(request.getEspecie());
        raza.setCompanyId(companyId);
        raza.setActivo(true);
        raza.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        raza.setUpdatedBy(SecurityUtils.getCurrentUserEmail());

        return toResponse(razaRepository.save(raza));
    }

    private RazaResponse toResponse(Raza raza) {
        RazaResponse r = new RazaResponse();
        r.setId(raza.getId());
        r.setNombre(raza.getNombre());
        r.setDescripcion(raza.getDescripcion());
        r.setEspecie(raza.getEspecie().name());
        r.setActivo(raza.getActivo());
        r.setCreatedBy(raza.getCreatedBy());
        r.setCreatedAt(raza.getCreatedAt());
        r.setUpdatedBy(raza.getUpdatedBy());
        r.setUpdatedAt(raza.getUpdatedAt());
        return r;
    }
}
