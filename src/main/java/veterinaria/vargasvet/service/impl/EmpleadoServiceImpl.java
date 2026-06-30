package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest;
import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.*;

import java.time.LocalTime;
import java.util.List;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.EmpleadoService;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.BusinessValidator;
import java.time.LocalDate;
import java.time.DayOfWeek;

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
    private final HorarioEmpleadoRepository horarioEmpleadoRepository;
    private final CompanyOperatingHourRepository companyOperatingHourRepository;
    private final CompanyExceptionRepository companyExceptionRepository;
    private final CitaRepository citaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final BusinessValidator businessValidator;
    private final veterinaria.vargasvet.service.AuditLogService auditLogService;
    private final UsuarioPorRolRepository usuarioPorRolRepository;

    @Value("${app.frontend.verify-url}")
    private String frontendVerifyUrl;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.phone}")
    private String companyPhone;

    @Value("${app.company.address}")
    private String companyAddress;

    @Value("${app.company.logo}")
    private String defaultCompanyLogo;

    @Override
    @Transactional
    public UserProfileDTO registerEmpleado(EmpleadoRequest dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El correo electrÃ³nico ya estÃ¡ en uso");
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

        Company companyToUse = companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        businessValidator.checkCompanyActiva(companyIdToUse);
        usuario.setCompany(companyToUse);

        Usuario savedUser = usuarioRepository.save(usuario);

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            usuarioPorRolRepository.deleteByUsuarioId(savedUser.getId());
            for (String roleName : new java.util.LinkedHashSet<>(dto.getRoles())) {
                Role role = roleRepository.findByNameAndCompanyId(roleName, companyIdToUse)
                        .orElseGet(() -> roleRepository.findFirstByName(roleName)
                                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName)));
                UsuarioPorRol upr = new UsuarioPorRol();
                upr.setUsuario(savedUser);
                upr.setRol(role);
                usuarioPorRolRepository.save(upr);
            }
        }


        Empleado empleado = new Empleado();
        empleado.setUser(savedUser);
        empleado.setEstado(true);
        empleado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        empleado.setNumeroDocumentoIdentidad(dto.getNumeroDocumento());
        empleado.setGenero(dto.getGenero());
        empleado.setObservaciones(dto.getObservaciones());
        empleado.setFotoUrl(dto.getFotoUrl());
        empleado.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());

    
        boolean isVeterinario = dto.getRoles() != null && dto.getRoles().contains("ROLE_VETERINARIO");
        if (isVeterinario) {
            if (dto.getNumeroColegiatura() == null || dto.getNumeroColegiatura().isBlank()) {
                throw new IllegalArgumentException("El nÃºmero de colegiatura es obligatorio para veterinarios");
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

        Empleado savedEmpleado = empleadoRepository.save(empleado);

        if (dto.getHorarios() != null && !dto.getHorarios().isEmpty()) {
            guardarHorarios(savedEmpleado, dto.getHorarios());
        }

        sendWelcomeEmail(savedUser, dto.getNombre(), tempPassword);

        auditLogService.log(
            "CREAR_EMPLEADO",
            "Empleados",
            "Se registrÃ³ al empleado " + dto.getNombre() + " " + dto.getApellido() + " con email " + dto.getEmail()
        );

        return userMapper.toProfileDTO(savedUser);
    }

    @Transactional
    @Override
    public UserProfileDTO updateEmpleado(Long empleadoId, EmpleadoRequest dto) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + empleadoId));

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new IllegalStateException("No se puede editar un empleado inactivo. Active al empleado primero.");
        }

        Usuario usuario = empleado.getUser();
        businessValidator.checkCompanyActiva(usuario.getCompany() != null ? usuario.getCompany().getId() : null);

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


        if (dto.getEmail() != null && !dto.getEmail().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El correo electrónico ya está en uso");
            }
            usuario.setEmail(dto.getEmail());
            usuario.setEmailVerified(false);
        }

        if (dto.getNombre() != null) usuario.setNombre(dto.getNombre());
        if (dto.getApellido() != null) usuario.setApellido(dto.getApellido());
        if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
        if (dto.getDireccion() != null) usuario.setDireccion(dto.getDireccion());


        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            boolean isTargetSuperAdmin = usuario.getUsuariosPorRol().stream()
                    .anyMatch(upr -> upr.getRol().getName().equals("ROLE_SUPER_ADMIN"));
            if (isTargetSuperAdmin && !SecurityUtils.isSuperAdmin()) {
                throw new IllegalArgumentException("Solo un Super Admin puede modificar los roles de otro Super Admin");
            }

            usuarioPorRolRepository.deleteByUsuarioId(usuario.getId());
            for (String roleName : new java.util.LinkedHashSet<>(dto.getRoles())) {
                Role role = roleRepository.findByNameAndCompanyId(roleName, companyIdToUse)
                        .orElseGet(() -> roleRepository.findFirstByName(roleName)
                                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName)));
                UsuarioPorRol upr = new UsuarioPorRol();
                upr.setUsuario(usuario);
                upr.setRol(role);
                usuarioPorRolRepository.save(upr);
            }
        }

        usuarioRepository.saveAndFlush(usuario);

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
        boolean isVeterinario = dto.getRoles() != null && dto.getRoles().contains("ROLE_VETERINARIO");
        if (isVeterinario) {
            if (dto.getNumeroColegiatura() != null) {
                empleado.setNumeroColegiatura(dto.getNumeroColegiatura());
            }
        }
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

        empleado.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        empleadoRepository.save(empleado);

        if (dto.getHorarios() != null) {
            empleado.getHorarios().clear();
            empleadoRepository.saveAndFlush(empleado);
            guardarHorarios(empleado, dto.getHorarios());
        }

        auditLogService.log(
            "ACTUALIZAR_EMPLEADO",
            "Empleados",
            "Se actualizaron los datos del empleado " + usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
        );

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
        if (Boolean.FALSE.equals(nuevoEstado) && citaRepository.existsCitaVigenteByEmpleadoId(empleadoId, veterinaria.vargasvet.util.AppClock.now())) {
            throw new IllegalArgumentException("No se puede desactivar un empleado con citas programadas vigentes");
        }
        empleado.setEstado(nuevoEstado);
        empleado.setEstadoModificadoPor(adminEmail);
        empleado.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());
        empleado.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        usuario.setActivo(nuevoEstado);

        empleadoRepository.save(empleado);
        usuarioRepository.save(usuario);

        auditLogService.log(
            Boolean.TRUE.equals(nuevoEstado) ? "ACTIVAR_EMPLEADO" : "DESACTIVAR_EMPLEADO",
            "Empleados",
            (Boolean.TRUE.equals(nuevoEstado) ? "Se activÃ³" : "Se desactivÃ³") + " al empleado " + usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
        );
    }

    @Override
    @Transactional
    public void eliminar(Long empleadoId) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + empleadoId));

        if (citaRepository.existsByEmpleadoId(empleadoId)) {
            Usuario usuario = empleado.getUser();
            horarioEmpleadoRepository.deleteByEmpleadoId(empleadoId);
            empleado.setEstado(false);
            empleado.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
            empleado.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());
            if (usuario != null) {
                usuario.setActivo(false);
                usuarioRepository.save(usuario);
            }
            empleadoRepository.save(empleado);

            String detalleEmpleado = usuario != null
                    ? usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
                    : "ID " + empleadoId;
            auditLogService.log(
                "DESACTIVAR_EMPLEADO_CON_HISTORIAL",
                "Empleados",
                "Se desactivo al empleado " + detalleEmpleado + " porque tiene historial asociado"
            );
            return;
        }

        Usuario usuario = empleado.getUser();
        String empEmail = usuario.getEmail();
        String empNombre = usuario.getNombre() + " " + usuario.getApellido();
        Integer usuarioId = usuario.getId();

        horarioEmpleadoRepository.deleteByEmpleadoId(empleadoId);
        empleado.getEspecialidades().clear();
        empleado.getTiposEmpleado().clear();
        usuarioPorRolRepository.deleteByUsuarioId(usuarioId);
        empleadoRepository.delete(empleado);
        usuarioRepository.deleteById(usuarioId);

        auditLogService.log(
            "ELIMINAR_EMPLEADO",
            "Empleados",
            "Se eliminÃ³ permanentemente al empleado " + empNombre + " (" + empEmail + ")"
        );
    }

    private void guardarHorarios(Empleado empleado, List<HorarioEmpleadoRequest> horariosRequest) {
        String adminEmail = SecurityUtils.getCurrentUserEmail();
        Integer companyId = empleado.getUser().getCompany().getId();

        for (HorarioEmpleadoRequest h : horariosRequest) {
            if (h.getHoraInicio() == null || h.getHoraFin() == null) continue;

            LocalDate fecha = h.getFecha();
            DiaSemana dia;

            if (fecha != null) {
                dia = toDiaSemana(fecha.getDayOfWeek());
                validarHorarioContraEmpresa(companyId, fecha, h.getHoraInicio(), h.getHoraFin());
            } else {
                dia = h.getDiaSemana();
                if (dia == null) continue;
                validarHorarioPorDia(companyId, dia, h.getHoraInicio(), h.getHoraFin());
            }

            HorarioEmpleado horario = new HorarioEmpleado();
            horario.setEmpleado(empleado);
            horario.setFecha(fecha);
            horario.setDiaSemana(dia);
            horario.setHoraInicio(h.getHoraInicio());
            horario.setHoraFin(h.getHoraFin());
            horario.setActivo(h.getActivo() != null ? h.getActivo() : true);
            horario.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
            horario.setCreatedBy(adminEmail);
            horarioEmpleadoRepository.save(horario);
        }
    }

    private void validarHorarioPorDia(Integer companyId, DiaSemana dia, LocalTime inicio, LocalTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la de fin");
        }
        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
        if (opHourOpt.isEmpty()) return;
        CompanyOperatingHour opHour = opHourOpt.get();
        if (Boolean.FALSE.equals(opHour.getIsOpen())) {
            throw new IllegalArgumentException("La clÃ­nica no abre los dÃ­as " + dia);
        }
        LocalTime opening = opHour.getOpeningTime();
        LocalTime closing = opHour.getClosingTime();
        if (inicio.isBefore(opening) || fin.isAfter(closing)) {
            throw new IllegalArgumentException(String.format(
                "El horario (%s - %s) estÃ¡ fuera del horario de atenciÃ³n de la clÃ­nica (%s - %s)",
                inicio, fin, opening, closing));
        }
    }

    private void validarHorarioContraEmpresa(Integer companyId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());
        
        // 1. Validar excepciones de empresa (feriados/cierres)
        companyExceptionRepository.findByCompanyIdAndDate(companyId, fecha)
                .ifPresent(ex -> {
                    if (Boolean.FALSE.equals(ex.getIsOpen())) {
                        throw new IllegalArgumentException("La clÃ­nica estÃ¡ cerrada el dÃ­a " + fecha + " (" + ex.getDescription() + ")");
                    }
                });

        // 2. Validar horario de atenciÃ³n del dÃ­a (solo si estÃ¡ configurado)
        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
        if (opHourOpt.isEmpty()) {
            // Sin configuraciÃ³n de horario, se permite cualquier hora
            return;
        }

        CompanyOperatingHour opHour = opHourOpt.get();
        if (Boolean.FALSE.equals(opHour.getIsOpen())) {
            throw new IllegalArgumentException("La clÃ­nica no abre los dÃ­as " + dia);
        }

        LocalTime opening = opHour.getOpeningTime();
        LocalTime closing = opHour.getClosingTime();

        // Validar si el turno estÃ¡ contenido en el horario de atenciÃ³n
        // Si el turno cruza la medianoche (inicio >= fin), es invÃ¡lido si la clÃ­nica no abre 24h
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la de fin (no se permiten turnos de duraciÃ³n cero o que crucen la medianoche)");
        }

        if (inicio.isBefore(opening) || fin.isAfter(closing)) {
            throw new IllegalArgumentException(String.format("El horario (%s - %s) estÃ¡ fuera del horario de atenciÃ³n de la clÃ­nica (%s - %s)",
                    inicio, fin, opening, closing));
        }
    }

    @Override
    @Transactional
    public void assignBulkSchedule(Long empleadoId, veterinaria.vargasvet.dto.request.BulkScheduleRequest request) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new IllegalStateException("No se puede asignar horario a un empleado inactivo");
        }

        String adminEmail = SecurityUtils.getCurrentUserEmail();
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (start.isAfter(end)) throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");

        // 1. Validar citas existentes en el rango si no es sobreescritura total o si se eliminan turnos
        // Para simplificar, si hay citas en el rango, mostramos cuÃ¡les son.
        List<Cita> citas = citaRepository.findByEmpleadoIdAndDateRange(empleadoId, start, end);
        if (!citas.isEmpty()) {
            StringBuilder sb = new StringBuilder("No se puede modificar el horario porque existen citas programadas: ");
            for (Cita c : citas) {
                sb.append(String.format("[%s %s - %s], ", c.getFechaHoraInicio().toLocalDate(), 
                        c.getFechaHoraInicio().toLocalTime(), c.getMascota().getNombreCompleto()));
            }
            throw new IllegalStateException(sb.toString());
        }

        // 3. Generar turnos dÃ­a por dÃ­a
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            final LocalDate currentDay = date;
            DiaSemana dia = toDiaSemana(currentDay.getDayOfWeek());

            // Verificar si la empresa abre ese dÃ­a
            if (!isEmpresaAbiertaEnDia(empleado.getUser().getCompany().getId(), currentDay, dia)) {
                continue;
            }

            // Filtrar solo los turnos que aplican a este dÃ­a de la semana
            List<HorarioEmpleadoRequest> shiftsParaHoy = request.getShifts().stream()
                    .filter(s -> s.getDiaSemana() == null || s.getDiaSemana().equals(dia))
                    .toList();
            
            if (shiftsParaHoy.isEmpty()) continue;

            // Si hay sobreescritura, borrar SOLO el turno original que se estÃ¡ reemplazando
            if (Boolean.TRUE.equals(request.getOverwrite()) && request.getOriginalStartTime() != null) {
                LocalTime originalStart = request.getOriginalStartTime();
                empleado.getHorarios().removeIf(h -> h.getFecha().equals(currentDay) && h.getHoraInicio().equals(originalStart));
                empleadoRepository.saveAndFlush(empleado);
            }

            // Validar traslapes con OTROS turnos que ya existan (y que no son el que estamos reemplazando)
            for (HorarioEmpleadoRequest shiftReq : shiftsParaHoy) {
                if (horarioEmpleadoRepository.existsOverlap(empleadoId, currentDay, shiftReq.getHoraInicio(), shiftReq.getHoraFin())) {
                    throw new IllegalStateException("Conflicto de horario el dÃ­a " + currentDay + ": el nuevo rango (" + 
                        shiftReq.getHoraInicio() + "-" + shiftReq.getHoraFin() + ") se traslapa con otro turno existente.");
                }
            }

            // Validar refrigerio solo si hay mÃ¡s de un turno para el MISMO dÃ­a
            if (shiftsParaHoy.size() > 1) {
                validarRefrigerio(shiftsParaHoy);
            }

            for (HorarioEmpleadoRequest shiftReq : shiftsParaHoy) {
                validarHorarioContraEmpresa(empleado.getUser().getCompany().getId(), currentDay, shiftReq.getHoraInicio(), shiftReq.getHoraFin());

                // Solo verificar traslape si NO estamos sobrescribiendo (porque ya borramos arriba)
                if (!Boolean.TRUE.equals(request.getOverwrite())) {
                    if (horarioEmpleadoRepository.existsOverlap(empleadoId, currentDay, shiftReq.getHoraInicio(), shiftReq.getHoraFin())) {
                        throw new IllegalStateException("Conflicto de horario el dÃ­a " + currentDay + " en la franja " + shiftReq.getHoraInicio());
                    }
                }

                HorarioEmpleado h = new HorarioEmpleado();
                h.setEmpleado(empleado);
                h.setFecha(currentDay);
                h.setDiaSemana(dia);
                h.setHoraInicio(shiftReq.getHoraInicio());
                h.setHoraFin(shiftReq.getHoraFin());
                h.setActivo(true);
                h.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
                h.setCreatedBy(adminEmail);
                if (empleado.getHorarios() == null) {
                    empleado.setHorarios(new java.util.ArrayList<>());
                }
                empleado.getHorarios().add(h);
                horarioEmpleadoRepository.save(h);
            }
        }

        auditLogService.log(
            "ASIGNAR_HORARIOS_MASIVO",
            "Horarios",
            "AsignaciÃ³n masiva de horarios para el empleado " + empleado.getUser().getNombre() + " " + empleado.getUser().getApellido() + " entre " + start + " y " + end
        );
    }

    /**
     * Verifica si la empresa estÃ¡ abierta en un dÃ­a determinado.
     * Si no hay horario operativo configurado, se permite la asignaciÃ³n (retorna true).
     */
    private boolean isEmpresaAbiertaEnDia(Integer companyId, LocalDate fecha, DiaSemana dia) {
        // Verificar excepciones (feriados/cierres especiales) - solo bloquear si existe y estÃ¡ cerrado
        var exception = companyExceptionRepository.findByCompanyIdAndDate(companyId, fecha);
        if (exception.isPresent() && Boolean.FALSE.equals(exception.get().getIsOpen())) {
            return false;
        }

        // Verificar horario operativo del dÃ­a
        var opHour = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
        // Si no hay configuraciÃ³n, se permite (no bloquear). Solo bloquear si explÃ­citamente cerrado.
        if (opHour.isPresent() && Boolean.FALSE.equals(opHour.get().getIsOpen())) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public void updateHorario(Long horarioId, HorarioEmpleadoRequest request) {
        HorarioEmpleado horario = horarioEmpleadoRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));
        
        Empleado empleado = horario.getEmpleado();

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new IllegalStateException("No se puede modificar el horario de un empleado inactivo");
        }
        LocalDate fecha = request.getFecha() != null ? request.getFecha() : horario.getFecha();
        
        validarHorarioContraEmpresa(empleado.getUser().getCompany().getId(), fecha, request.getHoraInicio(), request.getHoraFin());

        // Verificar traslape excluyendo el propio registro
        if (horarioEmpleadoRepository.existsOverlapExcluding(empleado.getId(), fecha, request.getHoraInicio(), request.getHoraFin(), horarioId)) {
            throw new IllegalStateException("Conflicto de horario con otro turno existente");
        }

        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());
        if (request.getFecha() != null) horario.setFecha(request.getFecha());
        if (request.getDiaSemana() != null) {
            horario.setDiaSemana(request.getDiaSemana());
        } else if (request.getFecha() != null) {
            horario.setDiaSemana(toDiaSemana(request.getFecha().getDayOfWeek()));
        }
        
        horarioEmpleadoRepository.save(horario);
    }

    @Override
    @Transactional
    public void deleteHorario(Long horarioId) {
        HorarioEmpleado horario = horarioEmpleadoRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));

        if (!Boolean.TRUE.equals(horario.getEmpleado().getEstado())) {
            throw new IllegalStateException("No se puede eliminar el horario de un empleado inactivo");
        }

        horarioEmpleadoRepository.deleteById(horarioId);
    }

    private void validarRefrigerio(List<HorarioEmpleadoRequest> shifts) {
        // Ordenar por hora de inicio
        List<HorarioEmpleadoRequest> sorted = shifts.stream()
                .sorted(java.util.Comparator.comparing(HorarioEmpleadoRequest::getHoraInicio))
                .toList();
        
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalTime finActual = sorted.get(i).getHoraFin();
            LocalTime inicioSiguiente = sorted.get(i+1).getHoraInicio();
            
            if (inicioSiguiente.isBefore(finActual)) {
                throw new IllegalArgumentException(String.format(
                    "Los turnos no pueden traslaparse o cruzarse (%s - %s con %s - %s)",
                    sorted.get(i).getHoraInicio(), finActual,
                    inicioSiguiente, sorted.get(i+1).getHoraFin()
                ));
            }
            
            long minutosGap = java.time.Duration.between(finActual, inicioSiguiente).toMinutes();
            if (minutosGap < 60) {
                throw new IllegalArgumentException("Debe haber al menos 1 hora de refrigerio entre turnos (" + finActual + " - " + inicioSiguiente + ")");
            }
        }
    }

    private DiaSemana toDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY    -> DiaSemana.LUNES;
            case TUESDAY   -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY  -> DiaSemana.JUEVES;
            case FRIDAY    -> DiaSemana.VIERNES;
            case SATURDAY  -> DiaSemana.SABADO;
            case SUNDAY    -> DiaSemana.DOMINGO;
        };
    }

    @Transactional(readOnly = true)
    public List<HorarioEmpleadoResponse> getHorario(Long empleadoId) {
        return horarioEmpleadoRepository.findByEmpleadoId(empleadoId).stream()
                .sorted(java.util.Comparator.comparing(HorarioEmpleado::getFecha, java.util.Comparator.nullsFirst(LocalDate::compareTo))
                        .thenComparing(HorarioEmpleado::getHoraInicio))
                .map(h -> {
                    HorarioEmpleadoResponse r = new HorarioEmpleadoResponse();
                    r.setId(h.getId());
                    r.setFecha(h.getFecha());
                    r.setDiaSemana(h.getDiaSemana().name());
                    r.setHoraInicio(h.getHoraInicio());
                    r.setHoraFin(h.getHoraFin());
                    r.setActivo(h.getActivo());
                    return r;
                }).toList();
    }

    private void sendWelcomeEmail(Usuario usuario, String nombre, String tempPassword) {
        try {
            Map<String, Object> model = new HashMap<>();
            String resolvedCompanyName = usuario.getCompany() != null ? usuario.getCompany().getName() : "VargasVet";
            String resolvedLogo = (usuario.getCompany() != null && usuario.getCompany().getLogoUrl() != null) ? usuario.getCompany().getLogoUrl() : defaultCompanyLogo;
            model.put("nombre", nombre);
            model.put("companyName", resolvedCompanyName);
            model.put("companyLogo", resolvedLogo);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyAddress", companyAddress);
            model.put("verificationLink", frontendVerifyUrl + usuario.getVerificationToken());

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Bienvenido al equipo de " + resolvedCompanyName,
                    model
            );

            emailService.sendEmail(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de bienvenida a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmpleadoListResponse> listar(Integer companyId, String nombre, String apellido, String email, Long tipoEmpleadoId, Long especialidadId, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim() : null;
        String apellidoFiltro = (apellido != null && !apellido.isBlank()) ? apellido.trim() : null;
        String emailFiltro = (email != null && !email.isBlank()) ? email.trim() : null;
        return empleadoRepository.buscar(resolvedCompanyId, nombreFiltro, apellidoFiltro, emailFiltro, tipoEmpleadoId, especialidadId,
                PageRequest.of(page, size, Sort.unsorted()))
                .map(this::toListResponse);
    }

    private Integer resolverCompanyId(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null) {
                throw new IllegalArgumentException("El parÃ¡metro companyId es requerido para SUPER_ADMIN");
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
        dto.setRoles(usuario.getUsuariosPorRol().stream().map(upr -> upr.getRol().getName()).collect(Collectors.toSet()));
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
            response.setUserId(empleado.getUser().getId());
        }
        response.setTiposEmpleado(empleado.getTiposEmpleado().stream()
                .map(t -> t.getNombre())
                .toList());
        response.setEspecialidades(empleado.getEspecialidades().stream()
                .map(e -> e.getNombre())
                .toList());
        return response;
    }

    @Override
    @Transactional
    public void cloneWeekSchedule(Long empleadoId, LocalDate sourceStart, LocalDate targetStart) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new IllegalStateException("No se puede clonar horario de un empleado inactivo");
        }

        Integer companyId = empleado.getUser().getCompany().getId();
        LocalDate sourceEnd = sourceStart.plusDays(6);
        LocalDate targetEnd = targetStart.plusDays(6);
        String adminEmail  = SecurityUtils.getCurrentUserEmail();

        // â”€â”€ 1. La semana destino no puede ser igual o anterior a la semana origen â”€â”€â”€â”€â”€â”€
        if (!targetStart.isAfter(sourceStart)) {
            throw new IllegalArgumentException(
                "La semana destino (" + targetStart + ") debe ser posterior a la semana origen (" + sourceStart + ").");
        }

        // â”€â”€ 2. Obtener turnos de la semana origen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<HorarioEmpleado> sourceShifts =
                horarioEmpleadoRepository.findByEmpleadoIdAndFechaBetween(empleadoId, sourceStart, sourceEnd);

        if (sourceShifts.isEmpty()) {
            throw new IllegalArgumentException(
                "No hay turnos registrados en la semana origen (" + sourceStart + " al " + sourceEnd + ") para clonar.");
        }

        // â”€â”€ 3. Validar citas existentes en la semana destino â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<Cita> citasEnDestino = citaRepository.findByEmpleadoIdAndDateRange(empleadoId, targetStart, targetEnd);
        if (!citasEnDestino.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                "No se puede clonar el horario porque el empleado tiene citas programadas en la semana destino: ");
            for (Cita c : citasEnDestino) {
                sb.append(String.format("[%s %s - %s], ",
                    c.getFechaHoraInicio().toLocalDate(),
                    c.getFechaHoraInicio().toLocalTime(),
                    c.getMascota().getNombreCompleto()));
            }
            throw new IllegalStateException(sb.toString().replaceAll(", $", ""));
        }

        // â”€â”€ 4. Pre-validar cada dÃ­a destino contra horario de clÃ­nica y feriados â”€â”€â”€â”€
        //      (antes de borrar nada, para fallar rÃ¡pido si algo es invÃ¡lido)
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(sourceStart, targetStart);
        List<String> warnings = new java.util.ArrayList<>();

        for (HorarioEmpleado source : sourceShifts) {
            LocalDate targetDay = source.getFecha().plusDays(daysDiff);
            DiaSemana dia       = toDiaSemana(targetDay.getDayOfWeek());

            // 4a. Verificar cierre especial / feriado en la fecha destino exacta
            var exception = companyExceptionRepository.findByCompanyIdAndDate(companyId, targetDay);
            if (exception.isPresent() && Boolean.FALSE.equals(exception.get().getIsOpen())) {
                throw new IllegalArgumentException(
                    "No se puede clonar el turno al " + targetDay + ": la clÃ­nica estÃ¡ cerrada ese dÃ­a (" +
                    exception.get().getDescription() + ").");
            }

            // 4b. Verificar horario operativo del dÃ­a destino
            var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
            if (opHourOpt.isPresent()) {
                CompanyOperatingHour opHour = opHourOpt.get();

                if (Boolean.FALSE.equals(opHour.getIsOpen())) {
                    // DÃ­a cerrado â†’ omitir con advertencia (no lanzar error, solo skip)
                    warnings.add("El dÃ­a " + targetDay + " (" + dia + ") fue omitido: la clÃ­nica no abre ese dÃ­a.");
                    continue;
                }

                // 4c. Validar que las horas del turno estÃ©n dentro del horario de la clÃ­nica
                LocalTime inicio   = source.getHoraInicio();
                LocalTime fin      = source.getHoraFin();
                LocalTime opening  = opHour.getOpeningTime();
                LocalTime closing  = opHour.getClosingTime();

                if (inicio.isBefore(opening) || fin.isAfter(closing)) {
                    throw new IllegalArgumentException(String.format(
                        "El turno del %s (%s - %s) no puede clonarse al %s: " +
                        "estÃ¡ fuera del horario de atenciÃ³n de la clÃ­nica (%s - %s).",
                        source.getFecha(), inicio, fin, targetDay, opening, closing));
                }
            }
        }

        // â”€â”€ 5. Limpiar SOLO si todas las validaciones pasaron â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        empleado.getHorarios().removeIf(h -> !h.getFecha().isBefore(targetStart) && !h.getFecha().isAfter(targetEnd));
        empleadoRepository.saveAndFlush(empleado);

        // â”€â”€ 6. Clonar dÃ­a por dÃ­a, respetando dÃ­as cerrados â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        for (HorarioEmpleado source : sourceShifts) {
            LocalDate targetDay = source.getFecha().plusDays(daysDiff);
            DiaSemana dia       = toDiaSemana(targetDay.getDayOfWeek());

            // Omitir dÃ­as cerrados (ya detectados en el pre-check de arriba)
            var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
            if (opHourOpt.isPresent() && Boolean.FALSE.equals(opHourOpt.get().getIsOpen())) {
                continue;
            }
            var exception = companyExceptionRepository.findByCompanyIdAndDate(companyId, targetDay);
            if (exception.isPresent() && Boolean.FALSE.equals(exception.get().getIsOpen())) {
                continue;
            }

            HorarioEmpleado target = new HorarioEmpleado();
            target.setEmpleado(empleado);
            target.setFecha(targetDay);
            target.setDiaSemana(dia);
            target.setHoraInicio(source.getHoraInicio());
            target.setHoraFin(source.getHoraFin());
            target.setActivo(true);
            target.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
            target.setCreatedBy(adminEmail);
            if (empleado.getHorarios() == null) {
                empleado.setHorarios(new java.util.ArrayList<>());
            }
            empleado.getHorarios().add(target);
            horarioEmpleadoRepository.save(target);
        }

        auditLogService.log(
            "CLONAR_HORARIOS_SEMANA",
            "Horarios",
            "Se clonÃ³ la semana de horarios del empleado " + empleado.getUser().getNombre() + " " + empleado.getUser().getApellido() + " desde " + sourceStart + " hacia " + targetStart
        );
    }

    @Override
    @Transactional
    public void cloneDaySchedule(Long empleadoId, LocalDate sourceDate, LocalDate targetDate) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new IllegalStateException("No se puede clonar horario de un empleado inactivo");
        }

        Integer companyId = empleado.getUser().getCompany().getId();
        String adminEmail  = SecurityUtils.getCurrentUserEmail();

        // 1. La fecha destino no puede ser igual a la de origen
        if (targetDate.equals(sourceDate)) {
            throw new IllegalArgumentException("La fecha destino debe ser diferente a la fecha origen.");
        }

        // 2. Obtener turnos de la fecha origen
        List<HorarioEmpleado> sourceShifts =
                horarioEmpleadoRepository.findByEmpleadoIdAndFecha(empleadoId, sourceDate);

        if (sourceShifts.isEmpty()) {
            throw new IllegalArgumentException(
                "No hay turnos registrados en el dÃ­a de origen (" + sourceDate + ") para clonar.");
        }

        // 3. Validar citas existentes en el dÃ­a destino
        List<Cita> citasEnDestino = citaRepository.findByEmpleadoIdAndDateRange(empleadoId, targetDate, targetDate);
        if (!citasEnDestino.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                "No se puede clonar el horario porque el empleado tiene citas programadas en el dÃ­a destino: ");
            for (Cita c : citasEnDestino) {
                sb.append(String.format("[%s %s - %s], ",
                    c.getFechaHoraInicio().toLocalDate(),
                    c.getFechaHoraInicio().toLocalTime(),
                    c.getMascota().getNombreCompleto()));
            }
            throw new IllegalStateException(sb.toString().replaceAll(", $", ""));
        }

        // 4. Pre-validar el dÃ­a destino contra horario de clÃ­nica y feriados
        DiaSemana diaDestino = toDiaSemana(targetDate.getDayOfWeek());

        // 4a. Verificar cierre especial / feriado en la fecha destino exacta
        var exception = companyExceptionRepository.findByCompanyIdAndDate(companyId, targetDate);
        if (exception.isPresent() && Boolean.FALSE.equals(exception.get().getIsOpen())) {
            throw new IllegalArgumentException(
                "No se puede clonar el turno al " + targetDate + ": la clÃ­nica estÃ¡ cerrada ese dÃ­a (" +
                exception.get().getDescription() + ").");
        }

        // 4b. Verificar horario operativo del dÃ­a destino
        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, diaDestino);
        if (opHourOpt.isPresent()) {
            CompanyOperatingHour opHour = opHourOpt.get();

            if (Boolean.FALSE.equals(opHour.getIsOpen())) {
                throw new IllegalArgumentException(
                    "No se puede clonar el turno al " + targetDate + ": la clÃ­nica no abre los dÃ­as " + diaDestino + ".");
            }

            // 4c. Validar que las horas del turno estÃ©n dentro del horario de la clÃ­nica
            for (HorarioEmpleado source : sourceShifts) {
                LocalTime inicio = source.getHoraInicio();
                LocalTime fin    = source.getHoraFin();
                LocalTime opening = opHour.getOpeningTime();
                LocalTime closing = opHour.getClosingTime();

                if (inicio.isBefore(opening) || fin.isAfter(closing)) {
                    throw new IllegalArgumentException(String.format(
                        "El turno del %s (%s - %s) no puede clonarse al %s: " +
                        "estÃ¡ fuera del horario de atenciÃ³n de la clÃ­nica (%s - %s).",
                        source.getFecha(), inicio, fin, targetDate, opening, closing));
                }
            }
        }

        // 5. Limpiar horarios previos del dÃ­a destino
        empleado.getHorarios().removeIf(h -> h.getFecha().equals(targetDate));
        empleadoRepository.saveAndFlush(empleado);

        // 6. Clonar dÃ­a por dÃ­a
        for (HorarioEmpleado source : sourceShifts) {
            HorarioEmpleado target = new HorarioEmpleado();
            target.setEmpleado(empleado);
            target.setFecha(targetDate);
            target.setDiaSemana(diaDestino);
            target.setHoraInicio(source.getHoraInicio());
            target.setHoraFin(source.getHoraFin());
            target.setActivo(true);
            target.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
            target.setCreatedBy(adminEmail);
            if (empleado.getHorarios() == null) {
                empleado.setHorarios(new java.util.ArrayList<>());
            }
            empleado.getHorarios().add(target);
            horarioEmpleadoRepository.save(target);
        }

        auditLogService.log(
            "CLONAR_HORARIOS_DIA",
            "Horarios",
            "Se clonÃ³ el dÃ­a de horarios del empleado " + empleado.getUser().getNombre() + " " + empleado.getUser().getApellido() + " del " + sourceDate + " hacia el " + targetDate
        );
    }

    @Override
    @Transactional
    public void deleteBulkSchedule(Long empleadoId, java.time.LocalDate startDate, java.time.LocalDate endDate, List<String> dias) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new IllegalStateException("No se puede eliminar horario de un empleado inactivo");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }

        // 1. Validar citas en el rango
        List<Cita> citas = citaRepository.findByEmpleadoIdAndDateRange(empleadoId, startDate, endDate);
        if (!citas.isEmpty()) {
            List<Cita> citasConConflicto = citas;
            if (dias != null && !dias.isEmpty() && !startDate.equals(endDate)) {
                List<DiaSemana> diasFiltro = dias.stream()
                        .map(d -> DiaSemana.valueOf(d.toUpperCase()))
                        .toList();
                citasConConflicto = citas.stream()
                        .filter(c -> {
                            DiaSemana dCita = toDiaSemana(c.getFechaHoraInicio().getDayOfWeek());
                            return diasFiltro.contains(dCita);
                        })
                        .toList();
            }

            if (!citasConConflicto.isEmpty()) {
                StringBuilder sb = new StringBuilder("No se puede eliminar el horario porque existen citas programadas: ");
                for (Cita c : citasConConflicto) {
                    sb.append(String.format("[%s %s - %s], ", c.getFechaHoraInicio().toLocalDate(), 
                            c.getFechaHoraInicio().toLocalTime(), c.getMascota().getNombreCompleto()));
                }
                throw new IllegalStateException(sb.toString());
            }
        }

        // 2. Eliminar en el rango
        if (startDate.equals(endDate)) {
            empleado.getHorarios().removeIf(h -> h.getFecha() == null || h.getFecha().equals(startDate));
        } else if (dias == null || dias.isEmpty()) {
            empleado.getHorarios().removeIf(h -> h.getFecha() == null || (!h.getFecha().isBefore(startDate) && !h.getFecha().isAfter(endDate)));
        } else {
            List<DiaSemana> diasFiltro = dias.stream()
                    .map(d -> DiaSemana.valueOf(d.toUpperCase()))
                    .toList();
            empleado.getHorarios().removeIf(h -> h.getFecha() == null || 
                    (!h.getFecha().isBefore(startDate) && 
                     !h.getFecha().isAfter(endDate) && 
                     diasFiltro.contains(h.getDiaSemana())));
        }
        empleadoRepository.save(empleado);

        auditLogService.log(
            "ELIMINAR_HORARIOS_MASIVO",
            "Horarios",
            "Se eliminaron masivamente los horarios del empleado " + empleado.getUser().getNombre() + " " + empleado.getUser().getApellido() + " del " + startDate + " al " + endDate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<veterinaria.vargasvet.dto.response.EmployeeScheduleReportResponse> getSchedulesReport(Integer companyId) {
        Integer resolvedId = resolverCompanyId(companyId);
        List<Empleado> empleados = empleadoRepository.findAllByCompanyId(resolvedId);
        
        return empleados.stream().map(emp -> {
            List<HorarioEmpleadoResponse> horarios = getHorario(emp.getId());
            return veterinaria.vargasvet.dto.response.EmployeeScheduleReportResponse.builder()
                    .empleadoId(emp.getId())
                    .nombreCompleto(emp.getUser().getNombre() + " " + emp.getUser().getApellido())
                    .cargo(emp.getTiposEmpleado().isEmpty() ? "Personal" : emp.getTiposEmpleado().iterator().next().getNombre())
                    .horarios(horarios)
                    .build();
        }).collect(Collectors.toList());
    }
}
