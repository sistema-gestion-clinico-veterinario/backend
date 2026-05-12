package veterinaria.vargasvet.perfil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.admin.Empleado;
import veterinaria.vargasvet.admin.HorarioEmpleado;
import veterinaria.vargasvet.admin.Usuario;
import veterinaria.vargasvet.perfil.ProfileUpdateRequest;
import veterinaria.vargasvet.admin.HorarioEmpleadoResponse;
import veterinaria.vargasvet.perfil.ProfileResponse;
import veterinaria.vargasvet.shared.ResourceNotFoundException;
import veterinaria.vargasvet.admin.EmpleadoRepository;
import veterinaria.vargasvet.admin.HorarioEmpleadoRepository;
import veterinaria.vargasvet.admin.UsuarioRepository;
import veterinaria.vargasvet.shared.SecurityUtils;
import veterinaria.vargasvet.perfil.ProfileService;

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

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        Usuario usuario = getCurrentUser();
        Optional<Empleado> empleadoOpt = empleadoRepository.findByUserId(usuario.getId());
        return buildResponse(usuario, empleadoOpt.orElse(null));
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
        empleadoOpt.ifPresent(empleado -> {
            if (dto.getObservaciones() != null) empleado.setObservaciones(dto.getObservaciones());
            if (dto.getFotoUrl() != null) empleado.setFotoUrl(dto.getFotoUrl());
            empleado.setUpdatedAt(LocalDateTime.now());
            empleadoRepository.save(empleado);
        });

        return buildResponse(usuario, empleadoOpt.orElse(null));
    }

    private Usuario getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private ProfileResponse buildResponse(Usuario usuario, Empleado empleado) {
        ProfileResponse res = new ProfileResponse();
        res.setId(usuario.getId());
        res.setEmail(usuario.getEmail());
        res.setNombre(usuario.getNombre());
        res.setApellido(usuario.getApellido());
        res.setDni(usuario.getDni());
        res.setTelefono(usuario.getTelefono());
        res.setDireccion(usuario.getDireccion());
        res.setActivo(usuario.isActivo());
        res.setRoles(usuario.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()));
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
                    .sorted(java.util.Comparator.comparingInt(h -> h.getDiaSemana().ordinal()))
                    .map(h -> {
                        HorarioEmpleadoResponse hr = new HorarioEmpleadoResponse();
                        hr.setId(h.getId());
                        hr.setDiaSemana(h.getDiaSemana().name());
                        hr.setHoraInicio(h.getHoraInicio());
                        hr.setHoraFin(h.getHoraFin());
                        hr.setActivo(h.getActivo());
                        return hr;
                    })
                    .collect(Collectors.toList());
            res.setHorarios(horarios);
        }

        return res;
    }
}
