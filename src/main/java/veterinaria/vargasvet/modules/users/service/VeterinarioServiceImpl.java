package veterinaria.vargasvet.modules.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.modules.users.domain.entity.*;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.modules.users.dto.request.VeterinarioRegistrationDTO;
import veterinaria.vargasvet.modules.users.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.modules.users.mapper.UserMapper;
import veterinaria.vargasvet.modules.users.repository.*;
import veterinaria.vargasvet.modules.shared.service.EmailService;

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
    private final EspecialidadRepository especialidadRepository;
    private final TipoEmpleadoRepository tipoEmpleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.url}")
    private String appUrl;

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
        String tempPassword = dto.getNumeroDocumento();
        usuario.setPassword(passwordEncoder.encode(tempPassword));
        usuario.setActivo(false);
        usuario.setEmailVerified(false);
        usuario.setVerificationToken(UUID.randomUUID().toString());

        roleRepository.findByName("ROLE_VETERINARIO")
                .ifPresent(role -> usuario.getRoles().add(role));

        Usuario savedUser = usuarioRepository.save(usuario);

        Empleado empleado = new Empleado();
        empleado.setNumeroColegiatura(dto.getNumeroColegiatura());
        empleado.setFotoUrl(dto.getFotoUrl());
        empleado.setEstado(true);
        empleado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        empleado.setNumeroDocumentoIdentidad(dto.getNumeroDocumento());
        empleado.setGenero(dto.getGenero());
        empleado.setUser(savedUser);
        empleado.setCreatedAt(LocalDateTime.now());

        if (dto.getEspecialidades() != null) {
            empleado.setEspecialidades(dto.getEspecialidades().stream()
                    .map(nombre -> especialidadRepository.findByNombre(nombre)
                            .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + nombre)))
                    .collect(Collectors.toSet()));
        }

        tipoEmpleadoRepository.findByNombre("VETERINARIO")
                .ifPresent(tipo -> empleado.getTiposEmpleado().add(tipo));

        empleadoRepository.save(empleado);

        sendWelcomeEmail(savedUser, dto.getNombre(), tempPassword);

        return userMapper.toProfileDTO(savedUser);
    }

    private void sendWelcomeEmail(Usuario usuario, String nombre, String tempPassword) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nombre", nombre);
            model.put("tempPassword", tempPassword);
            model.put("companyName", "VargasVet");
            model.put("verificationLink", appUrl + "/auth/verify/" + usuario.getVerificationToken());

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Bienvenido al equipo de VargasVet",
                    model
            );

            emailService.sendEmail(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de bienvenida a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }
}
