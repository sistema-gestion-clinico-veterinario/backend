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

        Company companyToUse = companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        businessValidator.checkCompanyActiva(companyIdToUse);
        usuario.setCompany(companyToUse);

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

        Empleado savedEmpleado = empleadoRepository.save(empleado);

        if (dto.getHorarios() != null && !dto.getHorarios().isEmpty()) {
            guardarHorarios(savedEmpleado, dto.getHorarios());
        }

        sendWelcomeEmail(savedUser, dto.getNombre(), tempPassword);

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

        if (dto.getHorarios() != null) {
            horarioEmpleadoRepository.deleteByEmpleadoId(empleado.getId());
            guardarHorarios(empleado, dto.getHorarios());
        }

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

    @Override
    @Transactional
    public void eliminar(Long empleadoId) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + empleadoId));
        horarioEmpleadoRepository.deleteByEmpleadoId(empleadoId);
        empleado.getEspecialidades().clear();
        empleado.getTiposEmpleado().clear();
        empleadoRepository.delete(empleado);
        usuarioRepository.delete(empleado.getUser());
    }

    private void guardarHorarios(Empleado empleado, List<HorarioEmpleadoRequest> horariosRequest) {
        String adminEmail = SecurityUtils.getCurrentUserEmail();
        for (HorarioEmpleadoRequest h : horariosRequest) {
            if (h.getHoraInicio() != null && h.getHoraFin() != null) {
                LocalDate fecha = h.getFecha() != null ? h.getFecha() : LocalDate.now();
                DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());

                // Validar contra horario de empresa
                validarHorarioContraEmpresa(empleado.getUser().getCompany().getId(), fecha, h.getHoraInicio(), h.getHoraFin());

                HorarioEmpleado horario = new HorarioEmpleado();
                horario.setEmpleado(empleado);
                horario.setFecha(fecha);
                horario.setDiaSemana(dia);
                horario.setHoraInicio(h.getHoraInicio());
                horario.setHoraFin(h.getHoraFin());
                horario.setActivo(h.getActivo() != null ? h.getActivo() : true);
                horario.setCreatedAt(java.time.LocalDateTime.now());
                horario.setCreatedBy(adminEmail);
                horarioEmpleadoRepository.save(horario);
            }
        }
    }

    private void validarHorarioContraEmpresa(Integer companyId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());
        
        // 1. Validar excepciones de empresa (feriados/cierres)
        companyExceptionRepository.findByCompanyIdAndDate(companyId, fecha)
                .ifPresent(ex -> {
                    if (Boolean.FALSE.equals(ex.getIsOpen())) {
                        throw new IllegalArgumentException("La clínica está cerrada el día " + fecha + " (" + ex.getDescription() + ")");
                    }
                });

        // 2. Validar horario de atención del día (solo si está configurado)
        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
        if (opHourOpt.isEmpty()) {
            // Sin configuración de horario, se permite cualquier hora
            return;
        }

        CompanyOperatingHour opHour = opHourOpt.get();
        if (Boolean.FALSE.equals(opHour.getIsOpen())) {
            throw new IllegalArgumentException("La clínica no abre los días " + dia);
        }

        LocalTime opening = opHour.getOpeningTime();
        LocalTime closing = opHour.getClosingTime();

        // Validar si el turno está contenido en el horario de atención
        // Si el turno cruza la medianoche (inicio >= fin), es inválido si la clínica no abre 24h
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la de fin (no se permiten turnos de duración cero o que crucen la medianoche)");
        }

        if (inicio.isBefore(opening) || fin.isAfter(closing)) {
            throw new IllegalArgumentException(String.format("El horario (%s - %s) está fuera del horario de atención de la clínica (%s - %s)",
                    inicio, fin, opening, closing));
        }
    }

    @Override
    @Transactional
    public void assignBulkSchedule(Long empleadoId, veterinaria.vargasvet.dto.request.BulkScheduleRequest request) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));
        
        String adminEmail = SecurityUtils.getCurrentUserEmail();
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (start.isAfter(end)) throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");

        // 1. Validar citas existentes en el rango si no es sobreescritura total o si se eliminan turnos
        // Para simplificar, si hay citas en el rango, mostramos cuáles son.
        List<Cita> citas = citaRepository.findByEmpleadoIdAndDateRange(empleadoId, start, end);
        if (!citas.isEmpty()) {
            StringBuilder sb = new StringBuilder("No se puede modificar el horario porque existen citas programadas: ");
            for (Cita c : citas) {
                sb.append(String.format("[%s %s - %s], ", c.getFechaHoraInicio().toLocalDate(), 
                        c.getFechaHoraInicio().toLocalTime(), c.getMascota().getNombreCompleto()));
            }
            throw new IllegalStateException(sb.toString());
        }

        // 3. Generar turnos día por día
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            final LocalDate currentDay = date;
            DiaSemana dia = toDiaSemana(currentDay.getDayOfWeek());

            // Verificar si la empresa abre ese día
            if (!isEmpresaAbiertaEnDia(empleado.getUser().getCompany().getId(), currentDay, dia)) {
                continue;
            }

            // Filtrar solo los turnos que aplican a este día de la semana
            List<HorarioEmpleadoRequest> shiftsParaHoy = request.getShifts().stream()
                    .filter(s -> s.getDiaSemana() == null || s.getDiaSemana().equals(dia))
                    .toList();
            
            if (shiftsParaHoy.isEmpty()) continue;

            // Si hay sobreescritura, borrar SOLO el turno original que se está reemplazando
            if (Boolean.TRUE.equals(request.getOverwrite()) && request.getOriginalStartTime() != null) {
                horarioEmpleadoRepository.deleteByEmpleadoIdAndFechaAndHoraInicio(empleadoId, currentDay, request.getOriginalStartTime());
                horarioEmpleadoRepository.flush();
            }

            // Validar traslapes con OTROS turnos que ya existan (y que no son el que estamos reemplazando)
            for (HorarioEmpleadoRequest shiftReq : shiftsParaHoy) {
                if (horarioEmpleadoRepository.existsOverlap(empleadoId, currentDay, shiftReq.getHoraInicio(), shiftReq.getHoraFin())) {
                    throw new IllegalStateException("Conflicto de horario el día " + currentDay + ": el nuevo rango (" + 
                        shiftReq.getHoraInicio() + "-" + shiftReq.getHoraFin() + ") se traslapa con otro turno existente.");
                }
            }

            // Validar refrigerio solo si hay más de un turno para el MISMO día
            if (shiftsParaHoy.size() > 1) {
                validarRefrigerio(shiftsParaHoy);
            }

            for (HorarioEmpleadoRequest shiftReq : shiftsParaHoy) {
                validarHorarioContraEmpresa(empleado.getUser().getCompany().getId(), currentDay, shiftReq.getHoraInicio(), shiftReq.getHoraFin());

                // Solo verificar traslape si NO estamos sobrescribiendo (porque ya borramos arriba)
                if (!Boolean.TRUE.equals(request.getOverwrite())) {
                    if (horarioEmpleadoRepository.existsOverlap(empleadoId, currentDay, shiftReq.getHoraInicio(), shiftReq.getHoraFin())) {
                        throw new IllegalStateException("Conflicto de horario el día " + currentDay + " en la franja " + shiftReq.getHoraInicio());
                    }
                }

                HorarioEmpleado h = new HorarioEmpleado();
                h.setEmpleado(empleado);
                h.setFecha(currentDay);
                h.setDiaSemana(dia);
                h.setHoraInicio(shiftReq.getHoraInicio());
                h.setHoraFin(shiftReq.getHoraFin());
                h.setActivo(true);
                h.setCreatedAt(java.time.LocalDateTime.now());
                h.setCreatedBy(adminEmail);
                horarioEmpleadoRepository.save(h);
            }
        }
    }

    /**
     * Verifica si la empresa está abierta en un día determinado.
     * Si no hay horario operativo configurado, se permite la asignación (retorna true).
     */
    private boolean isEmpresaAbiertaEnDia(Integer companyId, LocalDate fecha, DiaSemana dia) {
        // Verificar excepciones (feriados/cierres especiales) - solo bloquear si existe y está cerrado
        var exception = companyExceptionRepository.findByCompanyIdAndDate(companyId, fecha);
        if (exception.isPresent() && Boolean.FALSE.equals(exception.get().getIsOpen())) {
            return false;
        }

        // Verificar horario operativo del día
        var opHour = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
        // Si no hay configuración, se permite (no bloquear). Solo bloquear si explícitamente cerrado.
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
        if (!horarioEmpleadoRepository.existsById(horarioId)) {
            throw new ResourceNotFoundException("Horario no encontrado");
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

    @Override
    @Transactional
    public void cloneWeekSchedule(Long empleadoId, LocalDate sourceStart, LocalDate targetStart) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        Integer companyId = empleado.getUser().getCompany().getId();
        LocalDate sourceEnd = sourceStart.plusDays(6);
        LocalDate targetEnd = targetStart.plusDays(6);
        String adminEmail  = SecurityUtils.getCurrentUserEmail();

        // ── 1. La semana destino no puede ser igual o anterior a la semana origen ──────
        if (!targetStart.isAfter(sourceStart)) {
            throw new IllegalArgumentException(
                "La semana destino (" + targetStart + ") debe ser posterior a la semana origen (" + sourceStart + ").");
        }

        // ── 2. Obtener turnos de la semana origen ────────────────────────────────────
        List<HorarioEmpleado> sourceShifts =
                horarioEmpleadoRepository.findByEmpleadoIdAndFechaBetween(empleadoId, sourceStart, sourceEnd);

        if (sourceShifts.isEmpty()) {
            throw new IllegalArgumentException(
                "No hay turnos registrados en la semana origen (" + sourceStart + " al " + sourceEnd + ") para clonar.");
        }

        // ── 3. Validar citas existentes en la semana destino ────────────────────────
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

        // ── 4. Pre-validar cada día destino contra horario de clínica y feriados ────
        //      (antes de borrar nada, para fallar rápido si algo es inválido)
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(sourceStart, targetStart);
        List<String> warnings = new java.util.ArrayList<>();

        for (HorarioEmpleado source : sourceShifts) {
            LocalDate targetDay = source.getFecha().plusDays(daysDiff);
            DiaSemana dia       = toDiaSemana(targetDay.getDayOfWeek());

            // 4a. Verificar cierre especial / feriado en la fecha destino exacta
            var exception = companyExceptionRepository.findByCompanyIdAndDate(companyId, targetDay);
            if (exception.isPresent() && Boolean.FALSE.equals(exception.get().getIsOpen())) {
                throw new IllegalArgumentException(
                    "No se puede clonar el turno al " + targetDay + ": la clínica está cerrada ese día (" +
                    exception.get().getDescription() + ").");
            }

            // 4b. Verificar horario operativo del día destino
            var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, dia);
            if (opHourOpt.isPresent()) {
                CompanyOperatingHour opHour = opHourOpt.get();

                if (Boolean.FALSE.equals(opHour.getIsOpen())) {
                    // Día cerrado → omitir con advertencia (no lanzar error, solo skip)
                    warnings.add("El día " + targetDay + " (" + dia + ") fue omitido: la clínica no abre ese día.");
                    continue;
                }

                // 4c. Validar que las horas del turno estén dentro del horario de la clínica
                LocalTime inicio   = source.getHoraInicio();
                LocalTime fin      = source.getHoraFin();
                LocalTime opening  = opHour.getOpeningTime();
                LocalTime closing  = opHour.getClosingTime();

                if (inicio.isBefore(opening) || fin.isAfter(closing)) {
                    throw new IllegalArgumentException(String.format(
                        "El turno del %s (%s - %s) no puede clonarse al %s: " +
                        "está fuera del horario de atención de la clínica (%s - %s).",
                        source.getFecha(), inicio, fin, targetDay, opening, closing));
                }
            }
        }

        // ── 5. Limpiar SOLO si todas las validaciones pasaron ────────────────────────
        horarioEmpleadoRepository.deleteByEmpleadoIdAndFechaBetween(empleadoId, targetStart, targetEnd);
        horarioEmpleadoRepository.flush();

        // ── 6. Clonar día por día, respetando días cerrados ──────────────────────────
        for (HorarioEmpleado source : sourceShifts) {
            LocalDate targetDay = source.getFecha().plusDays(daysDiff);
            DiaSemana dia       = toDiaSemana(targetDay.getDayOfWeek());

            // Omitir días cerrados (ya detectados en el pre-check de arriba)
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
            target.setCreatedAt(LocalDateTime.now());
            target.setCreatedBy(adminEmail);
            horarioEmpleadoRepository.save(target);
        }
    }

    @Override
    @Transactional
    public void cloneDaySchedule(Long empleadoId, LocalDate sourceDate, LocalDate targetDate) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

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
                "No hay turnos registrados en el día de origen (" + sourceDate + ") para clonar.");
        }

        // 3. Validar citas existentes en el día destino
        List<Cita> citasEnDestino = citaRepository.findByEmpleadoIdAndDateRange(empleadoId, targetDate, targetDate);
        if (!citasEnDestino.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                "No se puede clonar el horario porque el empleado tiene citas programadas en el día destino: ");
            for (Cita c : citasEnDestino) {
                sb.append(String.format("[%s %s - %s], ",
                    c.getFechaHoraInicio().toLocalDate(),
                    c.getFechaHoraInicio().toLocalTime(),
                    c.getMascota().getNombreCompleto()));
            }
            throw new IllegalStateException(sb.toString().replaceAll(", $", ""));
        }

        // 4. Pre-validar el día destino contra horario de clínica y feriados
        DiaSemana diaDestino = toDiaSemana(targetDate.getDayOfWeek());

        // 4a. Verificar cierre especial / feriado en la fecha destino exacta
        var exception = companyExceptionRepository.findByCompanyIdAndDate(companyId, targetDate);
        if (exception.isPresent() && Boolean.FALSE.equals(exception.get().getIsOpen())) {
            throw new IllegalArgumentException(
                "No se puede clonar el turno al " + targetDate + ": la clínica está cerrada ese día (" +
                exception.get().getDescription() + ").");
        }

        // 4b. Verificar horario operativo del día destino
        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, diaDestino);
        if (opHourOpt.isPresent()) {
            CompanyOperatingHour opHour = opHourOpt.get();

            if (Boolean.FALSE.equals(opHour.getIsOpen())) {
                throw new IllegalArgumentException(
                    "No se puede clonar el turno al " + targetDate + ": la clínica no abre los días " + diaDestino + ".");
            }

            // 4c. Validar que las horas del turno estén dentro del horario de la clínica
            for (HorarioEmpleado source : sourceShifts) {
                LocalTime inicio = source.getHoraInicio();
                LocalTime fin    = source.getHoraFin();
                LocalTime opening = opHour.getOpeningTime();
                LocalTime closing = opHour.getClosingTime();

                if (inicio.isBefore(opening) || fin.isAfter(closing)) {
                    throw new IllegalArgumentException(String.format(
                        "El turno del %s (%s - %s) no puede clonarse al %s: " +
                        "está fuera del horario de atención de la clínica (%s - %s).",
                        source.getFecha(), inicio, fin, targetDate, opening, closing));
                }
            }
        }

        // 5. Limpiar horarios previos del día destino
        horarioEmpleadoRepository.deleteByEmpleadoIdAndFecha(empleadoId, targetDate);
        horarioEmpleadoRepository.flush();

        // 6. Clonar día por día
        for (HorarioEmpleado source : sourceShifts) {
            HorarioEmpleado target = new HorarioEmpleado();
            target.setEmpleado(empleado);
            target.setFecha(targetDate);
            target.setDiaSemana(diaDestino);
            target.setHoraInicio(source.getHoraInicio());
            target.setHoraFin(source.getHoraFin());
            target.setActivo(true);
            target.setCreatedAt(LocalDateTime.now());
            target.setCreatedBy(adminEmail);
            horarioEmpleadoRepository.save(target);
        }
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
