package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.EmpleadoService;
import veterinaria.vargasvet.security.SecurityUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.dto.response.EmpleadoListResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpleadoServiceImpl implements EmpleadoService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final TipoEmpleadoRepository tipoEmpleadoRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.url}")
    private String appUrl;

    @Override
    @Transactional
    public UserProfileDTO registerEmpleado(EmpleadoRequest dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setDni(dto.getNumeroDocumento());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        
        String tempPassword = dto.getNumeroDocumento();
        usuario.setPassword(passwordEncoder.encode(tempPassword));
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        usuario.setVerificationToken(UUID.randomUUID().toString());

        Integer companyIdToUse;
        if (SecurityUtils.isSuperAdmin()) {
            if (dto.getCompanyId() == null) {
                throw new IllegalArgumentException("El Super Admin debe proporcionar un companyId");
            }
            companyIdToUse = dto.getCompanyId();
        } else {
            companyIdToUse = SecurityUtils.getCurrentCompanyId();
            if (companyIdToUse == null) {
                throw new IllegalArgumentException("No se pudo determinar la empresa del administrador");
            }
        }

        usuario.setCompany(companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));

     
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            usuario.getRoles().clear();
            for (String roleName : dto.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName));
                usuario.getRoles().add(role);
            }
        }

        Usuario savedUser = usuarioRepository.save(usuario);


        Empleado empleado = new Empleado();
        empleado.setUser(savedUser);
        empleado.setEstado(true);
        empleado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        empleado.setNumeroDocumentoIdentidad(dto.getNumeroDocumento());
        empleado.setGenero(dto.getGenero());
        empleado.setObservaciones(dto.getObservaciones());
        empleado.setFotoUrl(dto.getFotoUrl());
        empleado.setCreatedAt(LocalDateTime.now());

    
        boolean isVeterinario = dto.getRoles().contains("ROLE_VETERINARIO");
        if (isVeterinario) {
            if (dto.getNumeroColegiatura() == null || dto.getNumeroColegiatura().isBlank()) {
                throw new IllegalArgumentException("El número de colegiatura es obligatorio para veterinarios");
            }
            empleado.setNumeroColegiatura(dto.getNumeroColegiatura());

            if (dto.getEspecialidades() != null) {
                empleado.setEspecialidades(dto.getEspecialidades().stream()
                        .map(nombre -> especialidadRepository.findByNombreAndCompanyId(nombre, companyIdToUse)
                                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + nombre + " para la empresa")))
                        .collect(Collectors.toSet()));
            }
        }

  
        if (dto.getTiposEmpleado() != null) {
            empleado.setTiposEmpleado(dto.getTiposEmpleado().stream()
                    .map(nombre -> tipoEmpleadoRepository.findByNombreAndCompanyId(nombre, companyIdToUse)
                            .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado: " + nombre + " para la empresa")))
                    .collect(Collectors.toSet()));
        }

        empleadoRepository.save(empleado);
        
        sendWelcomeEmail(savedUser, dto.getNombre(), tempPassword);

        return userMapper.toProfileDTO(savedUser);
    }

    @Transactional
    @Override
    public UserProfileDTO updateEmpleado(Long empleadoId, EmpleadoRequest dto) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + empleadoId));

        Usuario usuario = empleado.getUser();

        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para editar a un empleado de otra empresa");
            }
        }

        Integer companyIdToUse = usuario.getCompany().getId();


        if (dto.getNumeroDocumento() != null && !dto.getNumeroDocumento().equals(usuario.getDni())) {
            if (usuarioRepository.existsByDni(dto.getNumeroDocumento())) {
                throw new IllegalArgumentException("El DNI/Documento ya está registrado por otro usuario");
            }
            usuario.setDni(dto.getNumeroDocumento());
        }


        if (dto.getNombre() != null) usuario.setNombre(dto.getNombre());
        if (dto.getApellido() != null) usuario.setApellido(dto.getApellido());
        if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
        if (dto.getDireccion() != null) usuario.setDireccion(dto.getDireccion());


        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            boolean isTargetSuperAdmin = usuario.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"));
            if (isTargetSuperAdmin && !SecurityUtils.isSuperAdmin()) {
                throw new IllegalArgumentException("Solo un Super Admin puede modificar los roles de otro Super Admin");
            }

            java.util.Set<Role> newRoles = new java.util.HashSet<>();
            for (String roleName : dto.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName));
                newRoles.add(role);
            }

            // Sincronizar roles para evitar errores de llave duplicada en Hibernate
            usuario.getRoles().retainAll(newRoles);
            usuario.getRoles().addAll(newRoles);
        }

        usuarioRepository.saveAndFlush(usuario);

        // Los datos del usuario ya están cargados en la variable 'usuario'

        if (dto.getGenero() != null) empleado.setGenero(dto.getGenero());
        if (dto.getTipoDocumento() != null) empleado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        if (dto.getNumeroDocumento() != null) empleado.setNumeroDocumentoIdentidad(dto.getNumeroDocumento());
        if (dto.getFotoUrl() != null) empleado.setFotoUrl(dto.getFotoUrl());
        if (dto.getObservaciones() != null) empleado.setObservaciones(dto.getObservaciones());
        if (dto.getEstado() != null) empleado.setEstado(dto.getEstado());


        if (dto.getTiposEmpleado() != null) {
            java.util.Set<TipoEmpleado> newTipos = new java.util.HashSet<>();
            for (String nombre : dto.getTiposEmpleado()) {
                TipoEmpleado tipo = tipoEmpleadoRepository.findByNombreAndCompanyId(nombre, companyIdToUse)
                        .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado: " + nombre + " para la empresa"));
                newTipos.add(tipo);
            }
            empleado.getTiposEmpleado().retainAll(newTipos);
            empleado.getTiposEmpleado().addAll(newTipos);
        }
        boolean isVeterinario = usuario.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_VETERINARIO"));
        if (isVeterinario && dto.getEspecialidades() != null) {
            java.util.Set<Especialidad> newEspecialidades = new java.util.HashSet<>();
            for (String nombre : dto.getEspecialidades()) {
                Especialidad esp = especialidadRepository.findByNombreAndCompanyId(nombre, companyIdToUse)
                        .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + nombre + " para la empresa"));
                newEspecialidades.add(esp);
            }
            empleado.getEspecialidades().retainAll(newEspecialidades);
            empleado.getEspecialidades().addAll(newEspecialidades);
        }

        empleado.setUpdatedAt(LocalDateTime.now());
        empleadoRepository.save(empleado);

        return userMapper.toProfileDTO(usuario);
    }

    @Transactional
    @Override
    public void cambiarEstado(Long empleadoId, Boolean nuevoEstado) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + empleadoId));

        Usuario usuario = empleado.getUser();

        String adminEmail = SecurityUtils.getCurrentUserEmail();
        
        if (usuario.getEmail().equals(adminEmail)) {
            throw new IllegalArgumentException("No puedes cambiar tu propio estado de actividad");
        }
        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar el estado de un empleado de otra empresa");
            }
        }
        empleado.setEstado(nuevoEstado);
        empleado.setEstadoModificadoPor(adminEmail);
        empleado.setFechaModificacionEstado(LocalDateTime.now());
        empleado.setUpdatedAt(LocalDateTime.now());
        usuario.setActivo(nuevoEstado);

        empleadoRepository.save(empleado);
        usuarioRepository.save(usuario);
    }

    private void sendWelcomeEmail(Usuario usuario, String nombre, String tempPassword) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nombre", nombre);
            model.put("tempPassword", tempPassword);
            model.put("companyName", usuario.getCompany() != null ? usuario.getCompany().getName() : "VargasVet");
            model.put("verificationLink", appUrl + "/auth/verify/" + usuario.getVerificationToken());

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Bienvenido al equipo de " + (usuario.getCompany() != null ? usuario.getCompany().getName() : "VargasVet"),
                    model
            );

            emailService.sendEmail(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de bienvenida a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmpleadoListResponse> listar(Integer companyId, String nombre, Long tipoEmpleadoId, Long especialidadId, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim() : null;
        return empleadoRepository.buscar(resolvedCompanyId, nombreFiltro, tipoEmpleadoId, especialidadId,
                PageRequest.of(page, size, Sort.unsorted()))
                .map(this::toListResponse);
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

    @Override
    @Transactional(readOnly = true)
    public EmpleadoRequest findById(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + id));

        Usuario usuario = empleado.getUser();
        EmpleadoRequest dto = new EmpleadoRequest();
        dto.setId(empleado.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setNumeroDocumento(usuario.getDni());
        dto.setTelefono(usuario.getTelefono());
        dto.setDireccion(usuario.getDireccion());
        dto.setRoles(usuario.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()));
        dto.setCompanyId(usuario.getCompany() != null ? usuario.getCompany().getId() : null);

        dto.setGenero(empleado.getGenero());
        dto.setTipoDocumento(empleado.getTipoDocumentoIdentidad());
        dto.setObservaciones(empleado.getObservaciones());
        dto.setFotoUrl(empleado.getFotoUrl());
        dto.setNumeroColegiatura(empleado.getNumeroColegiatura());
        dto.setEspecialidades(empleado.getEspecialidades().stream().map(e -> e.getNombre()).collect(Collectors.toSet()));
        dto.setTiposEmpleado(empleado.getTiposEmpleado().stream().map(t -> t.getNombre()).collect(Collectors.toSet()));

        return dto;
    }

    private EmpleadoListResponse toListResponse(Empleado empleado) {
        EmpleadoListResponse response = new EmpleadoListResponse();
        response.setId(empleado.getId());
        response.setNumeroColegiatura(empleado.getNumeroColegiatura());
        response.setFotoUrl(empleado.getFotoUrl());
        response.setActivo(empleado.getEstado());
        if (empleado.getUser() != null) {
            response.setNombre(empleado.getUser().getNombre());
            response.setApellido(empleado.getUser().getApellido());
            response.setEmail(empleado.getUser().getEmail());
            response.setTelefono(empleado.getUser().getTelefono());
        }
        response.setTiposEmpleado(empleado.getTiposEmpleado().stream()
                .map(t -> t.getNombre())
                .toList());
        response.setEspecialidades(empleado.getEspecialidades().stream()
                .map(e -> e.getNombre())
                .toList());
        return response;
    }
}
