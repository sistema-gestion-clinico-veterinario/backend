package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ApoderadoService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.util.BusinessValidator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.dto.response.ApoderadoListResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApoderadoServiceImpl implements ApoderadoService {

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaRepository mascotaRepository;
    private final RoleRepository roleRepository;
    private final veterinaria.vargasvet.repository.UsuarioPorRolRepository usuarioPorRolRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final BusinessValidator businessValidator;
    private final EmailService emailService;
    private final veterinaria.vargasvet.service.AuditLogService auditLogService;

    @Value("${app.frontend.login-url}")
    private String loginUrl;

    @Value("${app.company.name}")
    private String defaultCompanyName;

    @Value("${app.company.logo}")
    private String defaultCompanyLogo;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.phone}")
    private String companyPhone;

    @Value("${app.company.address}")
    private String companyAddress;

    @Override
    @Transactional
    public UserProfileDTO registerApoderado(ApoderadoRequest dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (usuarioRepository.existsByDni(dto.getNumeroDocumento())) {
            throw new IllegalArgumentException("El DNI ya está registrado en el sistema");
        }

        Integer companyIdToUse;
        if (SecurityUtils.isSuperAdmin()) {
            if (dto.getCompanyId() == null) {
                throw new IllegalArgumentException("El Super Admin debe proporcionar un companyId");
            }
            companyIdToUse = dto.getCompanyId();
        } else {
            companyIdToUse = SecurityUtils.getCurrentCompanyId();
            if (companyIdToUse == null) {
                throw new IllegalArgumentException("No se pudo determinar la empresa del registrador");
            }
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setDni(dto.getNumeroDocumento());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        usuario.setPassword(passwordEncoder.encode(dto.getNumeroDocumento()));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        businessValidator.checkCompanyActiva(companyIdToUse);
        usuario.setCompany(companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));

        Usuario savedUser = usuarioRepository.save(usuario);

        Role apoderadoRole = roleRepository.findByName("ROLE_APODERADO")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Error de configuración: El rol 'ROLE_APODERADO' no existe."));
        UsuarioPorRol upr = new UsuarioPorRol();
        upr.setUsuario(savedUser);
        upr.setRol(apoderadoRole);
        usuarioPorRolRepository.save(upr);

        Apoderado apoderado = new Apoderado();
        apoderado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        apoderado.setNumeroDocumento(dto.getNumeroDocumento());
        apoderado.setGenero(dto.getGenero());
        apoderado.setReferencias(dto.getReferencias());
        apoderado.setObservaciones(dto.getObservaciones());
        apoderado.setUser(savedUser);

        Apoderado savedApoderado = apoderadoRepository.save(apoderado);

        sendWelcomeEmail(savedUser, dto.getNombre() + " " + dto.getApellido(), dto.getNumeroDocumento());

        auditLogService.log(
            "CREAR_APODERADO",
            "Clientes",
            "Se registró al cliente/apoderado " + dto.getNombre() + " " + dto.getApellido() + " con email " + dto.getEmail()
        );

        UserProfileDTO profileDTO = userMapper.toProfileDTO(savedUser);
        profileDTO.setApoderadoId(savedApoderado.getId().intValue());
        return profileDTO;
    }

    private void sendWelcomeEmail(Usuario usuario, String nombre, String DNI) {
        try {
            String resolvedCompanyName = usuario.getCompany() != null ? usuario.getCompany().getName() : defaultCompanyName;
            String resolvedLogo = (usuario.getCompany() != null && usuario.getCompany().getLogoUrl() != null) ? usuario.getCompany().getLogoUrl() : defaultCompanyLogo;
            java.util.Map<String, Object> model = new java.util.HashMap<>();
            model.put("nombre", nombre);
            model.put("email", usuario.getEmail());
            model.put("tempPassword", DNI);
            model.put("companyName", resolvedCompanyName);
            model.put("companyLogo", resolvedLogo);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyAddress", companyAddress);
            model.put("verificationLink", loginUrl);

            veterinaria.vargasvet.dto.Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "¡Bienvenido a la familia de " + resolvedCompanyName + "!",
                    model
            );

            emailService.sendEmail(mail, "email/welcome-apoderado-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de bienvenida al apoderado " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserProfileDTO updateApoderado(Long id, ApoderadoRequest dto) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));

        Usuario usuario = apoderado.getUser();

        if (!usuario.isActivo()) {
            throw new IllegalStateException("No se puede editar un cliente inactivo. Active al cliente primero.");
        }
        businessValidator.checkCompanyActiva(usuario.getCompany() != null ? usuario.getCompany().getId() : null);

        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para editar un apoderado de otra empresa");
            }
        }

        if (dto.getNombre() != null) usuario.setNombre(dto.getNombre());
        if (dto.getApellido() != null) usuario.setApellido(dto.getApellido());
        if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
        if (dto.getDireccion() != null) usuario.setDireccion(dto.getDireccion());

        if (dto.getEmail() != null && !dto.getEmail().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado por otro usuario");
            }
            usuario.setEmail(dto.getEmail());
        }

        usuarioRepository.save(usuario);

        if (dto.getGenero() != null) apoderado.setGenero(dto.getGenero());
        if (dto.getTipoDocumento() != null) apoderado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        if (dto.getReferencias() != null) apoderado.setReferencias(dto.getReferencias());
        if (dto.getObservaciones() != null) apoderado.setObservaciones(dto.getObservaciones());

        apoderado.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        apoderadoRepository.save(apoderado);

        auditLogService.log(
            "ACTUALIZAR_APODERADO",
            "Clientes",
            "Se actualizaron los datos del cliente/apoderado " + usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
        );

        return userMapper.toProfileDTO(usuario);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean nuevoEstado) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));

        Usuario usuario = apoderado.getUser();


        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para cambiar el estado de un apoderado de otra empresa");
            }
        }


        usuario.setActivo(nuevoEstado);
        usuarioRepository.save(usuario);

        apoderado.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        apoderado.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());
        apoderadoRepository.save(apoderado);


        List<Mascota> mascotas = mascotaRepository.findByApoderadoId(apoderado.getId());
        for (Mascota mascota : mascotas) {
            mascota.setActivo(nuevoEstado);
            mascotaRepository.save(mascota);
        }

        auditLogService.log(
            Boolean.TRUE.equals(nuevoEstado) ? "ACTIVAR_APODERADO" : "DESACTIVAR_APODERADO",
            "Clientes",
            (Boolean.TRUE.equals(nuevoEstado) ? "Se activó" : "Se desactivó") + " al cliente/apoderado " + usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
        );
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));
        if (!mascotaRepository.findByApoderadoId(apoderado.getId()).isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar un propietario que tiene mascotas registradas");
        }
        String clientNombre = apoderado.getUser().getNombre() + " " + apoderado.getUser().getApellido();
        String clientEmail = apoderado.getUser().getEmail();
        apoderadoRepository.delete(apoderado);
        usuarioRepository.delete(apoderado.getUser());

        auditLogService.log(
            "ELIMINAR_APODERADO",
            "Clientes",
            "Se eliminó permanentemente al cliente/apoderado " + clientNombre + " (" + clientEmail + ")"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApoderadoListResponse> listar(Integer companyId, String nombre, String numeroDocumento, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim() : null;
        String docFiltro = (numeroDocumento != null && !numeroDocumento.isBlank()) ? numeroDocumento.trim() : null;
        return apoderadoRepository.buscar(resolvedCompanyId, nombreFiltro, docFiltro,
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
    public ApoderadoRequest findById(Long id) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));

        Usuario usuario = apoderado.getUser();
        ApoderadoRequest dto = new ApoderadoRequest();
        dto.setId(apoderado.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setNumeroDocumento(usuario.getDni());
        dto.setTelefono(usuario.getTelefono());
        dto.setDireccion(usuario.getDireccion());
        dto.setCompanyId(usuario.getCompany() != null ? usuario.getCompany().getId() : null);

        dto.setGenero(apoderado.getGenero());
        dto.setTipoDocumento(apoderado.getTipoDocumentoIdentidad());
        dto.setReferencias(apoderado.getReferencias());
        dto.setObservaciones(apoderado.getObservaciones());

        return dto;
    }

    private ApoderadoListResponse toListResponse(Apoderado apoderado) {
        ApoderadoListResponse response = new ApoderadoListResponse();
        response.setId(apoderado.getId());
        response.setTipoDocumento(apoderado.getTipoDocumentoIdentidad());
        response.setNumeroDocumento(apoderado.getNumeroDocumento());
        if (apoderado.getUser() != null) {
            response.setNombre(apoderado.getUser().getNombre());
            response.setApellido(apoderado.getUser().getApellido());
            response.setEmail(apoderado.getUser().getEmail());
            response.setTelefono(apoderado.getUser().getTelefono());
            response.setActivo(apoderado.getUser().isActivo());
        }
        return response;
    }
}
