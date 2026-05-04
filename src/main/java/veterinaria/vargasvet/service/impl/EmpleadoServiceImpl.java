package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.EmpleadoRegistrationDTO;
import veterinaria.vargasvet.dto.request.EmpleadoUpdateDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.EmpleadoService;
import veterinaria.vargasvet.security.SecurityUtils;

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
    public UserProfileDTO registerEmpleado(EmpleadoRegistrationDTO dto) {
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

        if (dto.getCompanyId() != null) {
            usuario.setCompany(companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
        }

     
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
                        .map(nombre -> especialidadRepository.findByNombre(nombre)
                                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + nombre)))
                        .collect(Collectors.toSet()));
            }
        }

  
        if (dto.getTiposEmpleado() != null) {
            empleado.setTiposEmpleado(dto.getTiposEmpleado().stream()
                    .map(nombre -> tipoEmpleadoRepository.findByNombre(nombre)
                            .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado: " + nombre)))
                    .collect(Collectors.toSet()));
        }

        empleadoRepository.save(empleado);
        
        sendWelcomeEmail(savedUser, dto.getNombre(), tempPassword);

        return userMapper.toProfileDTO(savedUser);
    }

    @Override
    @Transactional
    public UserProfileDTO updateEmpleado(Integer usuarioId, EmpleadoUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para editar a un empleado de otra empresa");
            }
        }


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

            usuario.getRoles().clear();
            for (String roleName : dto.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName));
                usuario.getRoles().add(role);
            }
        }

        usuarioRepository.save(usuario);

        Empleado empleado = empleadoRepository.findByUserId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de empleado no encontrada para este usuario"));

        if (dto.getGenero() != null) empleado.setGenero(dto.getGenero());
        if (dto.getTipoDocumento() != null) empleado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        if (dto.getNumeroDocumento() != null) empleado.setNumeroDocumentoIdentidad(dto.getNumeroDocumento());
        if (dto.getFotoUrl() != null) empleado.setFotoUrl(dto.getFotoUrl());
        if (dto.getObservaciones() != null) empleado.setObservaciones(dto.getObservaciones());
        if (dto.getEstado() != null) empleado.setEstado(dto.getEstado());


        if (dto.getTiposEmpleado() != null) {
            empleado.getTiposEmpleado().clear();
            for (String nombre : dto.getTiposEmpleado()) {
                TipoEmpleado tipo = tipoEmpleadoRepository.findByNombre(nombre)
                        .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado: " + nombre));
                empleado.getTiposEmpleado().add(tipo);
            }
        }
        boolean isVeterinario = usuario.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_VETERINARIO"));
        if (isVeterinario && dto.getEspecialidades() != null) {
            empleado.getEspecialidades().clear();
            for (String nombre : dto.getEspecialidades()) {
                Especialidad esp = especialidadRepository.findByNombre(nombre)
                        .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + nombre));
                empleado.getEspecialidades().add(esp);
            }
        }

        empleado.setUpdatedAt(LocalDateTime.now());
        empleadoRepository.save(empleado);

        return userMapper.toProfileDTO(usuario);
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
}
