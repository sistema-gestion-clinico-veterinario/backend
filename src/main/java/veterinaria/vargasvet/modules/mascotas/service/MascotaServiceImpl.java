package veterinaria.vargasvet.modules.mascotas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.modules.users.domain.entity.Apoderado;
import veterinaria.vargasvet.modules.company.domain.entity.Company;
import veterinaria.vargasvet.modules.mascotas.domain.entity.Mascota;
import veterinaria.vargasvet.modules.mascotas.domain.enums.EspecieMascota;
import veterinaria.vargasvet.modules.mascotas.domain.enums.MotivoBajaMascota;
import veterinaria.vargasvet.modules.mascotas.dto.EstadoMascotaRequest;
import veterinaria.vargasvet.modules.mascotas.dto.MascotaRequest;
import veterinaria.vargasvet.modules.mascotas.dto.MascotaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.modules.mascotas.mapper.MascotaMapper;
import veterinaria.vargasvet.modules.users.repository.ApoderadoRepository;
import veterinaria.vargasvet.modules.mascotas.repository.MascotaRepository;
import veterinaria.vargasvet.modules.users.security.SecurityUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mascotaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaMapper mascotaMapper;

    @Override
    @Transactional
    public MascotaResponse registerMascota(MascotaRequest request) {
        Apoderado apoderado = apoderadoRepository.findById(request.getApoderadoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el apoderado con ID: " + request.getApoderadoId()));

        if (apoderado.getUser() == null || !apoderado.getUser().isActivo()) {
            throw new IllegalArgumentException("No se puede registrar una mascota a un dueño inactivo. Active al dueño primero.");
        }

        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (apoderado.getUser().getCompany() == null || !apoderado.getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para registrar mascotas a clientes de otra clínica");
            }
        }

        if (request.getEspecie() == EspecieMascota.OTRO) {
            if (request.getOtraEspecie() == null || request.getOtraEspecie().trim().isEmpty()) {
                throw new IllegalArgumentException("Debe especificar cuál es la especie si selecciona 'OTRO'");
            }
        }

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(request.getNombreCompleto());
        mascota.setEspecie(request.getEspecie());
        mascota.setOtraEspecie(request.getEspecie() == EspecieMascota.OTRO ? request.getOtraEspecie() : null);
        mascota.setRaza(request.getRaza());
        mascota.setSexo(request.getSexo());
        mascota.setFechaNacimiento(request.getFechaNacimiento());
        mascota.setPeso(request.getPeso());
        mascota.setColor(request.getColor());
        mascota.setSenasParticulares(request.getSenasParticulares());
        mascota.setEsterilizado(request.getEsterilizado() != null ? request.getEsterilizado() : false);
        mascota.setActivo(true);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota.setFotoUrl(request.getFotoUrl());
        mascota.setNumeroMicrochip(request.getNumeroMicrochip());
        mascota.setObservaciones(request.getObservaciones());
        mascota.setApoderado(apoderado);

        Mascota savedMascota = mascotaRepository.save(mascota);
        return mascotaMapper.toResponse(savedMascota);
    }

    @Override
    @Transactional
    public MascotaResponse updateMascota(Long id, MascotaRequest request) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + id));

        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (mascota.getApoderado().getUser().getCompany() == null || !mascota.getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar una mascota de otra clínica");
            }
        }

        if (request.getApoderadoId() != null && !request.getApoderadoId().equals(mascota.getApoderado().getId())) {
            Apoderado nuevoApoderado = apoderadoRepository.findById(request.getApoderadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró el nuevo apoderado con ID: " + request.getApoderadoId()));
            
            if (nuevoApoderado.getUser() == null || !nuevoApoderado.getUser().isActivo()) {
                throw new IllegalArgumentException("No se puede transferir una mascota a un dueño inactivo. Active al dueño primero.");
            }

            if (!SecurityUtils.isSuperAdmin()) {
                Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
                if (nuevoApoderado.getUser().getCompany() == null || !nuevoApoderado.getUser().getCompany().getId().equals(currentCompanyId)) {
                    throw new IllegalArgumentException("No tienes permiso para transferir la mascota a un cliente de otra clínica");
                }
            }
            mascota.setApoderado(nuevoApoderado);
        }

        if (request.getEspecie() != null) {
            if (request.getEspecie() == EspecieMascota.OTRO) {
                if (request.getOtraEspecie() == null || request.getOtraEspecie().trim().isEmpty()) {
                    throw new IllegalArgumentException("Debe especificar cuál es la especie si selecciona 'OTRO'");
                }
                mascota.setOtraEspecie(request.getOtraEspecie());
            } else {
                mascota.setOtraEspecie(null);
            }
            mascota.setEspecie(request.getEspecie());
        }

        if (request.getNombreCompleto() != null) mascota.setNombreCompleto(request.getNombreCompleto());
        if (request.getRaza() != null) mascota.setRaza(request.getRaza());
        if (request.getSexo() != null) mascota.setSexo(request.getSexo());
        if (request.getFechaNacimiento() != null) mascota.setFechaNacimiento(request.getFechaNacimiento());
        if (request.getPeso() != null) mascota.setPeso(request.getPeso());
        if (request.getColor() != null) mascota.setColor(request.getColor());
        if (request.getSenasParticulares() != null) mascota.setSenasParticulares(request.getSenasParticulares());
        if (request.getEsterilizado() != null) mascota.setEsterilizado(request.getEsterilizado());
        if (request.getFotoUrl() != null) mascota.setFotoUrl(request.getFotoUrl());
        if (request.getNumeroMicrochip() != null) mascota.setNumeroMicrochip(request.getNumeroMicrochip());
        if (request.getObservaciones() != null) mascota.setObservaciones(request.getObservaciones());

        Mascota savedMascota = mascotaRepository.save(mascota);
        return mascotaMapper.toResponse(savedMascota);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, EstadoMascotaRequest request) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + id));

        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (mascota.getApoderado().getUser().getCompany() == null || !mascota.getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar el estado de una mascota de otra clínica");
            }
        }

        if (!request.getActive()) {
            if (request.getMotivoBaja() == null) {
                throw new IllegalArgumentException("Debe proporcionar un motivo de baja para desactivar la mascota");
            }
            if (request.getMotivoBaja() == MotivoBajaMascota.OTRO) {
                if (request.getOtroMotivoBaja() == null || request.getOtroMotivoBaja().trim().isEmpty()) {
                    throw new IllegalArgumentException("Debe especificar el motivo si selecciona 'OTRO'");
                }
            }
            mascota.setMotivoBaja(request.getMotivoBaja());
            mascota.setOtroMotivoBaja(request.getMotivoBaja() == MotivoBajaMascota.OTRO ? request.getOtroMotivoBaja() : null);
        } else {
            mascota.setMotivoBaja(null);
            mascota.setOtroMotivoBaja(null);
        }

        mascota.setActivo(request.getActive());
        mascota.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        mascota.setFechaModificacionEstado(java.time.LocalDateTime.now());

        mascotaRepository.save(mascota);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MascotaResponse> listar(Integer companyId, String nombre, EspecieMascota especie, String nombrePropietario, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim() : null;
        String propietarioFiltro = (nombrePropietario != null && !nombrePropietario.isBlank()) ? nombrePropietario.trim() : null;
        return mascotaRepository.buscar(resolvedCompanyId, nombreFiltro, especie, propietarioFiltro,
                PageRequest.of(page, size, Sort.unsorted()))
                .map(mascotaMapper::toResponse);
    }

    private Integer resolverCompanyId(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null) {
                throw new IllegalArgumentException("El parámetro companyId es requerido para SUPER_ADMIN");
            }
            return companyIdParam;
        }
        return SecurityUtils.getCurrentCompanyId();
    }
}
