package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.MascotaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.MascotaService;
import veterinaria.vargasvet.service.ApoderadoService;
import veterinaria.vargasvet.service.MascotaRelacionService;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mascotaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final CitaRepository citaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final MascotaMapper mascotaMapper;
    private final BusinessValidator businessValidator;
    private final veterinaria.vargasvet.service.AuditLogService auditLogService;
    private final veterinaria.vargasvet.repository.RazaRepository razaRepository;
    private final ApoderadoService apoderadoService;
    private final MascotaRelacionService mascotaRelacionService;

    @Override
    @Transactional
    public MascotaResponse registerMascota(MascotaRequest request) {
        
    
        Apoderado apoderado = apoderadoRepository.findById(request.getApoderadoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el apoderado con ID: " + request.getApoderadoId()));


        if (!Boolean.TRUE.equals(apoderado.getEstado())) {
            throw new IllegalArgumentException("No se puede registrar una mascota a un dueño inactivo. Active al dueño primero.");
        }

        boolean esPrimeraMascotaActiva = !mascotaRepository
                .existsByApoderadoIdAndActivoTrue(apoderado.getId());

        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (apoderado.getCompany() == null || !apoderado.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para registrar mascotas a clientes de otra clínica");
            }
        }
        businessValidator.checkCompanyActiva(
            apoderado.getUser() != null && apoderado.getCompany() != null
                ? apoderado.getCompany().getId() : null);


        if (request.getEspecie() == EspecieMascota.OTRO) {
            if (request.getOtraEspecie() == null || request.getOtraEspecie().trim().isEmpty()) {
                throw new IllegalArgumentException("Debe especificar cuál es la especie si selecciona 'OTRO'");
            }
        }


        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(request.getNombreCompleto());
        mascota.setEspecie(request.getEspecie());
        mascota.setOtraEspecie(request.getEspecie() == EspecieMascota.OTRO ? request.getOtraEspecie() : null);
        if (request.getRazaId() != null) {
            veterinaria.vargasvet.domain.entity.Raza raza = razaRepository.findById(request.getRazaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Raza no encontrada con ID: " + request.getRazaId()));
            mascota.setRaza(raza);
        }
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

        HistoriaClinica hc = new HistoriaClinica();
        hc.setMascota(savedMascota);
        hc.setNumeroHc(String.format("HC-%06d", savedMascota.getId()));
        hc.setActiva(true);
        historiaClinicaRepository.save(hc);

        mascotaRelacionService.asegurarPropietarioPrincipal(savedMascota.getId());

        auditLogService.log(
            "REGISTRAR_MASCOTA",
            "Mascotas",
            "Se registró a la mascota: " + savedMascota.getNombreCompleto() + " (" + savedMascota.getEspecie() + ")"
        );

        if (esPrimeraMascotaActiva) {
            apoderadoService.enviarInvitacionAccesoSiTieneMascota(apoderado.getId());
        }

        return mascotaMapper.toResponse(savedMascota);
    }

    @Override
    @Transactional(readOnly = true)
    public MascotaResponse obtenerPorId(Long id) {
        Mascota mascota = findAccessibleById(id);
        return mascotaMapper.toResponse(mascota);
    }

    @Override
    @Transactional(readOnly = true)
    public MascotaResponse obtenerPorUuid(String uuid) {
        Mascota mascota = SecurityUtils.isSuperAdmin()
                ? mascotaRepository.findByUuid(uuid).orElseThrow(() -> mascotaNotFound(null))
                : mascotaRepository.findByUuidAndCompanyId(uuid, requireCompanyId())
                    .orElseThrow(() -> mascotaNotFound(null));
        return mascotaMapper.toResponse(mascota);
    }

    @Override
    @Transactional
    public MascotaResponse updateMascota(Long id, MascotaRequest request) {
        Mascota mascota = findAccessibleById(id);
        Apoderado apoderadoAnterior = mascota.getApoderado();
        boolean titularidadTransferida = false;
        boolean nuevoTitularSinMascotasActivas = false;

        if (!Boolean.TRUE.equals(mascota.getActivo())) {
            throw new IllegalStateException("No se puede editar una mascota inactiva. Active a la mascota primero.");
        }
        businessValidator.checkCompanyActiva(
            mascota.getApoderado() != null && mascota.getApoderado().getUser() != null
                && mascota.getApoderado().getCompany() != null
                ? mascota.getApoderado().getCompany().getId() : null);

        if (request.getApoderadoId() != null && !request.getApoderadoId().equals(mascota.getApoderado().getId())) {
            Integer companyIdMascota = mascota.getApoderado().getCompany().getId();
            Apoderado nuevoApoderado = apoderadoRepository
                    .findByIdAndCompanyId(request.getApoderadoId(), companyIdMascota)
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró el nuevo propietario en esta clínica"));
            
            if (!Boolean.TRUE.equals(nuevoApoderado.getEstado())) {
                throw new IllegalArgumentException("No se puede transferir una mascota a un propietario inactivo. Active al propietario primero.");
            }

            if (!SecurityUtils.isSuperAdmin()) {
                Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
                if (nuevoApoderado.getCompany() == null || !nuevoApoderado.getCompany().getId().equals(currentCompanyId)) {
                    throw new IllegalArgumentException("No tienes permiso para transferir la mascota a un cliente de otra clínica");
                }
            }

            nuevoTitularSinMascotasActivas = !mascotaRepository
                    .existsByApoderadoIdAndActivoTrue(nuevoApoderado.getId());
            mascota.setApoderado(nuevoApoderado);
            titularidadTransferida = true;
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
        if (request.getRazaId() != null) {
            veterinaria.vargasvet.domain.entity.Raza raza = razaRepository.findById(request.getRazaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Raza no encontrada con ID: " + request.getRazaId()));
            mascota.setRaza(raza);
        }
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

        mascotaRelacionService.asegurarPropietarioPrincipal(savedMascota.getId());

        if (titularidadTransferida) {
            String anterior = nombreApoderado(apoderadoAnterior);
            String nuevo = nombreApoderado(savedMascota.getApoderado());
            auditLogService.log(
                    savedMascota.getApoderado().getCompany().getId(),
                    "TRANSFERIR_TITULARIDAD_MASCOTA",
                    "Mascotas",
                    "Se transfirió la titularidad de " + savedMascota.getNombreCompleto()
                            + " de " + anterior + " a " + nuevo
            );
            if (nuevoTitularSinMascotasActivas) {
                apoderadoService.enviarInvitacionAccesoSiTieneMascota(savedMascota.getApoderado().getId());
            }
        }

        auditLogService.log(
            "ACTUALIZAR_MASCOTA",
            "Mascotas",
            "Se actualizaron los datos de la mascota: " + savedMascota.getNombreCompleto()
        );

        return mascotaMapper.toResponse(savedMascota);
    }

    private String nombreApoderado(Apoderado apoderado) {
        if (apoderado == null || apoderado.getUser() == null) return "propietario anterior";
        return (apoderado.getUser().getNombre() + " " + apoderado.getUser().getApellido()).trim();
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, veterinaria.vargasvet.dto.request.EstadoMascotaRequest request) {
        Mascota mascota = findAccessibleById(id);

        if (!request.getActive()) {
            if (citaRepository.existsCitaVigenteByMascotaId(id, veterinaria.vargasvet.util.AppClock.now())) {
                throw new IllegalArgumentException("No se puede desactivar una mascota con citas programadas vigentes");
            }
            if (request.getMotivoBaja() == null) {
                throw new IllegalArgumentException("Debe proporcionar un motivo de baja para desactivar la mascota");
            }
            if (request.getMotivoBaja() == veterinaria.vargasvet.domain.enums.MotivoBajaMascota.OTRO) {
                if (request.getOtroMotivoBaja() == null || request.getOtroMotivoBaja().trim().isEmpty()) {
                    throw new IllegalArgumentException("Debe especificar el motivo si selecciona 'OTRO'");
                }
            }
            mascota.setMotivoBaja(request.getMotivoBaja());
            mascota.setOtroMotivoBaja(request.getMotivoBaja() == veterinaria.vargasvet.domain.enums.MotivoBajaMascota.OTRO ? request.getOtroMotivoBaja() : null);
        } else {
            mascota.setMotivoBaja(null);
            mascota.setOtroMotivoBaja(null);
        }

        mascota.setActivo(request.getActive());
        mascota.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        mascota.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());

        mascotaRepository.save(mascota);

        auditLogService.log(
            Boolean.TRUE.equals(request.getActive()) ? "ACTIVAR_MASCOTA" : "DESACTIVAR_MASCOTA",
            "Mascotas",
            (Boolean.TRUE.equals(request.getActive()) ? "Se activó" : "Se desactivó") + " a la mascota: " + mascota.getNombreCompleto()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MascotaResponse> listar(Integer companyId, String nombre, EspecieMascota especie, String nombrePropietario, Boolean activo, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim() : null;
        String propietarioFiltro = (nombrePropietario != null && !nombrePropietario.isBlank()) ? nombrePropietario.trim() : null;
        return mascotaRepository.buscar(resolvedCompanyId, nombreFiltro, especie, propietarioFiltro, activo,
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

    private Mascota findAccessibleById(Long id) {
        return (SecurityUtils.isSuperAdmin()
                ? mascotaRepository.findById(id)
                : mascotaRepository.findByIdAndCompanyId(id, requireCompanyId()))
                .orElseThrow(() -> mascotaNotFound(id));
    }

    private Integer requireCompanyId() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("El usuario no tiene una empresa asignada");
        }
        return companyId;
    }

    private ResourceNotFoundException mascotaNotFound(Long id) {
        return new ResourceNotFoundException(id == null
                ? "Mascota no encontrada"
                : "Mascota no encontrada con ID: " + id);
    }
}
