package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.ProfileUpdateRequest;
import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.dto.response.ProfileResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ProfileService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final HorarioEmpleadoRepository horarioEmpleadoRepository;
    private final ApoderadoRepository apoderadoRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        Usuario usuario = getCurrentUser();
        Optional<Empleado> empleadoOpt = empleadoRepository.findByUserId(usuario.getId());
        Optional<Apoderado> apoderadoOpt = Optional.empty();

        if (empleadoOpt.isEmpty() && (SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin())) {
            Integer companyId = SecurityUtils.getCurrentCompanyId();
            if (companyId != null) {
                List<Empleado> companyEmployees = empleadoRepository.findAllByCompanyId(companyId);
                if (!companyEmployees.isEmpty()) {
                    empleadoOpt = Optional.of(companyEmployees.get(0));
                }
            } else {
                List<Empleado> allEmployees = empleadoRepository.findAll();
                if (!allEmployees.isEmpty()) {
                    empleadoOpt = Optional.of(allEmployees.get(0));
                }
            }
        }

        if (empleadoOpt.isEmpty()) {
            apoderadoOpt = apoderadoRepository.findByUserId(usuario.getId());
        }

        return buildResponse(usuario, empleadoOpt.orElse(null), apoderadoOpt.orElse(null));
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(ProfileUpdateRequest dto) {
        Usuario usuario = getCurrentUser();

        if (dto.getNombre() != null && !dto.getNombre().isBlank()) usuario.setNombre(dto.getNombre());
        if (dto.getApellido() != null && !dto.getApellido().isBlank()) usuario.setApellido(dto.getApellido());
        if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
        if (dto.getDireccion() != null) usuario.setDireccion(dto.getDireccion());
        usuarioRepository.save(usuario);

        Optional<Empleado> empleadoOpt = empleadoRepository.findByUserId(usuario.getId());
        Optional<Apoderado> apoderadoOpt = Optional.empty();
        empleadoOpt.ifPresent(empleado -> {
            if (dto.getObservaciones() != null) empleado.setObservaciones(dto.getObservaciones());
            if (dto.getFotoUrl() != null) empleado.setFotoUrl(dto.getFotoUrl());
            empleado.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
            empleadoRepository.save(empleado);
        });

        if (empleadoOpt.isEmpty()) {
            apoderadoOpt = apoderadoRepository.findByUserId(usuario.getId());
        }

        return buildResponse(usuario, empleadoOpt.orElse(null), apoderadoOpt.orElse(null));
    }

    private Usuario getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private ProfileResponse buildResponse(Usuario usuario, Empleado empleado, Apoderado apoderado) {
        ProfileResponse res = new ProfileResponse();
        res.setId(usuario.getId());
        res.setEmail(usuario.getEmail());
        res.setNombre(usuario.getNombre());
        res.setApellido(usuario.getApellido());
        res.setDni(usuario.getDni());
        res.setTelefono(usuario.getTelefono());
        res.setDireccion(usuario.getDireccion());
        res.setActivo(usuario.isActivo());
        res.setRoles(usuario.getUsuariosPorRol().stream().map(upr -> upr.getRol().getName()).collect(Collectors.toSet()));
        res.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);

        if (empleado != null) {
            res.setEmpleado(true);
            res.setEmpleadoId(empleado.getId());
            res.setGenero(empleado.getGenero() != null ? empleado.getGenero().name() : null);
            res.setTipoDocumento(empleado.getTipoDocumentoIdentidad() != null ? empleado.getTipoDocumentoIdentidad().name() : null);
            res.setNumeroColegiatura(empleado.getNumeroColegiatura());
            res.setObservaciones(empleado.getObservaciones());
            res.setFotoUrl(empleado.getFotoUrl());
            res.setEspecialidades(empleado.getEspecialidades().stream().map(e -> e.getNombre()).collect(Collectors.toSet()));
            res.setTiposEmpleado(empleado.getTiposEmpleado().stream().map(t -> t.getNombre()).collect(Collectors.toSet()));

            List<HorarioEmpleadoResponse> horarios = horarioEmpleadoRepository.findByEmpleadoId(empleado.getId())
                    .stream()
                    .filter(h -> Boolean.TRUE.equals(h.getActivo()))
                    .sorted(java.util.Comparator.comparing(HorarioEmpleado::getFecha,
                            java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()))
                            .thenComparing(HorarioEmpleado::getHoraInicio))
                    .map(h -> {
                        HorarioEmpleadoResponse hr = new HorarioEmpleadoResponse();
                        hr.setId(h.getId());
                        hr.setFecha(h.getFecha());
                        hr.setDiaSemana(h.getDiaSemana().name());
                        hr.setHoraInicio(h.getHoraInicio());
                        hr.setHoraFin(h.getHoraFin());
                        hr.setActivo(h.getActivo());
                        return hr;
                    })
                    .collect(Collectors.toList());
            res.setHorarios(horarios);
        } else if (apoderado != null) {
            res.setEmpleado(false);
            res.setTipoDocumento(apoderado.getTipoDocumentoIdentidad() != null ? apoderado.getTipoDocumentoIdentidad().name() : null);
            res.setGenero(apoderado.getGenero() != null ? apoderado.getGenero().name() : null);
        }

        return res;
    }
}
