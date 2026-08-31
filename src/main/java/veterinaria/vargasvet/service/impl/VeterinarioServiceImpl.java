package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.VeterinarioRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.VeterinarioService;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.domain.enums.RoleScope;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VeterinarioServiceImpl implements VeterinarioService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final veterinaria.vargasvet.repository.UsuarioPorRolRepository usuarioPorRolRepository;
    private final EspecialidadRepository especialidadRepository;
    private final TipoEmpleadoRepository tipoEmpleadoRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.frontend.verify-url}")
    private String frontendVerifyUrl;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.company.logo}")
    private String companyLogo;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.phone}")
    private String companyPhone;

    @Value("${app.company.address}")
    private String companyAddress;

    @Override
    @Transactional
    public UserProfileDTO registerVeterinario(VeterinarioRegistrationDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso");
        }
        if (empleadoRepository.existsByNumeroColegiatura(dto.getNumeroColegiatura())) {
            throw new IllegalArgumentException("El número de colegiatura ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setDni(dto.getNumeroDocumento());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        usuario.setPassword(passwordEncoder.encode(tempPassword));
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        String verificationToken = SecurityTokenUtils.generate();
        usuario.setVerificationToken(SecurityTokenUtils.hash(verificationToken));
        usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(24));
        Integer companyId = SecurityUtils.isSuperAdmin() ? dto.getCompanyId() : SecurityUtils.getCurrentCompanyId();
        if (companyId == null) throw new IllegalArgumentException("Debe seleccionar una empresa");
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        usuario.setCompany(company);

        Usuario savedUser = usuarioRepository.save(usuario);

        for (Integer roleId : new java.util.LinkedHashSet<>(dto.getRoleIds())) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleId));
            Integer roleCompanyId = role.getCompany() != null ? role.getCompany().getId() : null;
            if (!role.isActivo() || role.getScope() != RoleScope.STAFF
                    || !java.util.Objects.equals(roleCompanyId, companyId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "El rol no pertenece al personal de esta empresa");
            }
            UsuarioPorRol upr = new UsuarioPorRol();
            upr.setUsuario(savedUser);
            upr.setRol(role);
            usuarioPorRolRepository.save(upr);
        }

        Empleado empleado = new Empleado();
        empleado.setNumeroColegiatura(dto.getNumeroColegiatura());
        empleado.setObservaciones(dto.getObservaciones());
        empleado.setFotoUrl(dto.getFotoUrl());
        empleado.setEstado(true);
        empleado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        empleado.setNumeroDocumentoIdentidad(dto.getNumeroDocumento());
        empleado.setGenero(dto.getGenero());
        empleado.setUser(savedUser);
        empleado.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());

        if (dto.getEspecialidades() != null) {
            empleado.setEspecialidades(dto.getEspecialidades().stream()
                    .map(nombre -> especialidadRepository.findByNombreAndCompanyId(nombre, companyId)
                            .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + nombre)))
                    .collect(Collectors.toSet()));
        }

        tipoEmpleadoRepository.findByNombreAndCompanyId("VETERINARIO", companyId)
                .ifPresent(tipo -> empleado.getTiposEmpleado().add(tipo));

        empleadoRepository.save(empleado);

        sendWelcomeEmail(savedUser, dto.getNombre(), verificationToken);

        return userMapper.toProfileDTO(savedUser);
    }

    private void sendWelcomeEmail(Usuario usuario, String nombre, String verificationToken) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nombre", nombre);
            model.put("companyName", companyName);
            model.put("companyLogo", companyLogo);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyAddress", companyAddress);
            model.put("verificationLink", frontendVerifyUrl + verificationToken);

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Bienvenido al equipo de " + companyName,
                    model
            );

            emailService.sendEmail(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de bienvenida a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }
}
