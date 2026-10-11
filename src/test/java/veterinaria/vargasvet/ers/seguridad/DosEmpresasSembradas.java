package veterinaria.vargasvet.ers.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Diagnostico;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Lote;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MarcaProducto;
import veterinaria.vargasvet.domain.entity.Prescripcion;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Tratamiento;
import veterinaria.vargasvet.domain.entity.UnidadMedida;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaContacto;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.UsuarioMembresia;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.EstadoDiagnostico;
import veterinaria.vargasvet.domain.enums.EstadoTratamiento;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.domain.enums.TipoControlStock;
import veterinaria.vargasvet.domain.enums.TipoDiagnostico;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.DiagnosticoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.MarcaProductoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TratamientoRepository;
import veterinaria.vargasvet.repository.UnidadMedidaRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Crea dos clínicas con datos reales de cada tipo. Todo dato de una clínica lleva su marca (TKA, TKB, TKA2…) para
 * que cualquier fuga se detecte buscando esa marca en una respuesta que no le corresponde.
 */
class DosEmpresasSembradas {

    static final String CLAVE = "Clave!2026x";

    record Fila(String tabla, Object id, String grupo) {}

    record Empresa(String marca, String slug, Integer companyId, Long apoderado1Id, Long apoderado2Id, Long mascota1Id,
                   Long mascota2Id, String uuidMascota1, String sku, String numeroHc, Long consultaId, Long citaId,
                   Long empleadoId, Long productoId, Long servicioId, Integer rolPersonalizadoId, Long citaFuturaId,
                   Long categoriaId, java.util.List<Fila> filas) {}

    private final CompanyRepository companias;
    private final UsuarioRepository usuarios;
    private final ApoderadoRepository apoderados;
    private final MascotaRepository mascotas;
    private final EmpleadoRepository empleados;
    private final ServiciosVeterinariosRepository servicios;
    private final CitaRepository citas;
    private final HistoriaClinicaRepository historias;
    private final ConsultaRepository consultas;
    private final DiagnosticoRepository diagnosticos;
    private final TratamientoRepository tratamientos;
    private final PrescripcionRepository prescripciones;
    private final CategoriaProductoRepository categorias;
    private final MarcaProductoRepository marcas;
    private final UnidadMedidaRepository unidades;
    private final ProductoRepository productos;
    private final LoteRepository lotes;
    private final RoleRepository roles;
    private final UsuarioPorRolRepository asignaciones;
    private final UsuarioEmpresaContactoRepository contactos;
    private final UsuarioEmpresaCredencialRepository credenciales;
    private final UsuarioMembresiaRepository membresias;
    private final VistaRepository vistas;
    private final RolVistaPermisoRepository permisos;
    private final CompanyRoleProvisioningService aprovisionamiento;
    private final PasswordEncoder codificador;
    private final TransactionTemplate transaccion;
    private final AtomicInteger secuencia = new AtomicInteger(10_000_000);
    private java.util.List<Fila> filas;
    private String grupo;

    private <T> T g(T entidad) {
        try {
            String tabla = entidad.getClass().getAnnotation(jakarta.persistence.Table.class).name();
            Object id = entidad.getClass().getMethod("getId").invoke(entidad);
            filas.add(new Fila(tabla, id, grupo));
        } catch (ReflectiveOperationException | NullPointerException ex) {
            throw new IllegalStateException("No se pudo registrar la fila de " + entidad.getClass().getSimpleName(), ex);
        }
        return entidad;
    }

    DosEmpresasSembradas(CompanyRepository companias, UsuarioRepository usuarios, ApoderadoRepository apoderados,
                         MascotaRepository mascotas, EmpleadoRepository empleados, ServiciosVeterinariosRepository servicios,
                         CitaRepository citas, HistoriaClinicaRepository historias, ConsultaRepository consultas,
                         DiagnosticoRepository diagnosticos, TratamientoRepository tratamientos,
                         PrescripcionRepository prescripciones, CategoriaProductoRepository categorias,
                         MarcaProductoRepository marcas, UnidadMedidaRepository unidades, ProductoRepository productos,
                         LoteRepository lotes, RoleRepository roles, UsuarioPorRolRepository asignaciones,
                         UsuarioEmpresaContactoRepository contactos, UsuarioEmpresaCredencialRepository credenciales,
                         UsuarioMembresiaRepository membresias, VistaRepository vistas, RolVistaPermisoRepository permisos,
                         CompanyRoleProvisioningService aprovisionamiento, PasswordEncoder codificador,
                         TransactionTemplate transaccion) {
        this.companias = companias; this.usuarios = usuarios; this.apoderados = apoderados; this.mascotas = mascotas;
        this.empleados = empleados; this.servicios = servicios; this.citas = citas; this.historias = historias;
        this.consultas = consultas; this.diagnosticos = diagnosticos; this.tratamientos = tratamientos;
        this.prescripciones = prescripciones; this.categorias = categorias; this.marcas = marcas; this.unidades = unidades;
        this.productos = productos; this.lotes = lotes; this.roles = roles; this.asignaciones = asignaciones;
        this.contactos = contactos; this.credenciales = credenciales; this.membresias = membresias; this.vistas = vistas; this.permisos = permisos; this.aprovisionamiento = aprovisionamiento;
        this.codificador = codificador; this.transaccion = transaccion;
    }

    Empresa sembrar(String marca) {
        filas = new java.util.ArrayList<>();
        grupo = "comun";
        return transaccion.execute(estado -> crear(marca));
    }

    private Empresa crear(String marca) {
        String minuscula = marca.toLowerCase();
        Company company = new Company();
        company.setName("Clinica " + marca);
        company.setSlug("clinica-" + minuscula);
        company.setRuc(String.valueOf(secuencia.incrementAndGet()).concat("000").substring(0, 11));
        company.setActivo(true);
        company = g(companias.save(company));
        var rolesBase = aprovisionamiento.ensureRequiredRoles(company);
        concederVistas(rolesBase.companyAdmin(), false);
        concederVistas(rolesBase.clientPortal(), true);

        Usuario admin = usuario("admin", marca, "Administrador", company);
        asignar(admin, rolesBase.companyAdmin(), company);

        grupo = "cliente1";
        Usuario cliente1 = usuario("cliente1", marca, "Ana", company);
        Apoderado apoderado1 = apoderado(cliente1, company);
        asignar(cliente1, rolesBase.clientPortal(), company);
        Mascota mascota1 = mascota("Luna" + marca, apoderado1);

        grupo = "cliente2";
        Usuario cliente2 = usuario("cliente2", marca, "Beatriz", company);
        Apoderado apoderado2 = apoderado(cliente2, company);
        asignar(cliente2, rolesBase.clientPortal(), company);
        Mascota mascota2 = mascota("Nube" + marca, apoderado2);

        grupo = "comun";
        Role rolVacio = rol("Sin permisos " + marca, company);
        grupo = "sinpermisos";
        Usuario personalSinPermisos = usuario("sinpermisos", marca, "Personal", company);
        asignar(personalSinPermisos, rolVacio, company);

        grupo = "comun";
        Usuario vet = usuario("vet", marca, "Veterinario", company);
        Empleado empleado = empleado(vet, company);
        ServiciosVeterinarios servicio = servicio(company, marca);

        grupo = "cliente1";
        HistoriaClinica historia = new HistoriaClinica();
        historia.setNumeroHc("HC-" + marca + "-1");
        historia.setMascota(mascota1);
        historia.setActiva(true);
        historia = g(historias.save(historia));

        Cita pasada = cita(mascota1, empleado, servicio, EstadoCita.COMPLETADA, -20);
        Cita futura = cita(mascota1, empleado, servicio, EstadoCita.PROGRAMADA, 5);
        Consulta consulta = consulta(historia, pasada, empleado, marca);
        diagnostico(consulta, marca);
        tratamiento(consulta, marca);
        prescripcion(consulta, marca);

        grupo = "comun";
        Producto producto = producto(company, marca);
        Role personalizado = rol("Rol personalizado " + marca, company);

        return new Empresa(marca, company.getSlug(), company.getId(), apoderado1.getId(), apoderado2.getId(),
                mascota1.getId(), mascota2.getId(), mascota1.getUuid(), producto.getSku(), historia.getNumeroHc(),
                consulta.getId(), pasada.getId(), empleado.getId(), producto.getId(), servicio.getId(), personalizado.getId(), futura.getId(),
                producto.getCategoria().getId(), filas);
    }

    /** Un administrador de clínica real puede operar todas las pantallas de personal de su empresa. */
    private void concederVistas(Role rolAdministrador, boolean deCliente) {
        java.util.Set<Integer> existentes = new java.util.HashSet<>();
        permisos.findByRolId(rolAdministrador.getId()).forEach(p -> existentes.add(p.getVista().getId()));
        for (var vista : vistas.findByActivoTrue()) {
            boolean esDeCliente = vista.getAudience() == veterinaria.vargasvet.domain.enums.ViewAudience.CLIENT;
            if (esDeCliente != deCliente || existentes.contains(vista.getId())) {
                continue;
            }
            RolVistaPermiso permiso = new RolVistaPermiso();
            permiso.setRol(rolAdministrador);
            permiso.setVista(vista);
            permiso.setLeer(true);
            permiso.setEscribir(true);
            permiso.setModificar(true);
            permiso.setEliminar(true);
            permiso.setDataScope(deCliente ? veterinaria.vargasvet.domain.enums.DataScope.OWN
                    : veterinaria.vargasvet.domain.enums.DataScope.COMPANY);
            permisos.save(permiso);
        }
    }

    private String digitos() {
        return String.valueOf(secuencia.incrementAndGet()).substring(0, 8);
    }

    private Usuario usuario(String rol, String marca, String nombre, Company company) {
        Usuario usuario = new Usuario();
        String base = rol + "." + marca.toLowerCase();
        usuario.setEmail(base + "@" + marca.toLowerCase() + ".test");
        usuario.setUsername(base);
        usuario.setNombre(nombre + marca);
        usuario.setApellido("Apellido" + marca);
        usuario.setDni(digitos());
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        usuario = g(usuarios.save(usuario));

        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setUsuario(usuario);
        credencial.setCompany(company);
        credencial.setPassword(codificador.encode(CLAVE));
        credencial.setPasswordChanged(true);
        credencial.setCreatedAt(LocalDateTime.now());
        credencial.setUpdatedAt(LocalDateTime.now());
        g(credenciales.save(credencial));

        UsuarioEmpresaContacto contacto = new UsuarioEmpresaContacto();
        contacto.setUsuario(usuario);
        contacto.setCompany(company);
        contacto.setTelefono("9" + digitos().substring(0, 8));
        contacto.setDireccion("Calle " + marca + " 123");
        contacto.setCreatedAt(LocalDateTime.now());
        contacto.setUpdatedAt(LocalDateTime.now());
        g(contactos.save(contacto));

        UsuarioMembresia membresia = new UsuarioMembresia();
        membresia.setUsuario(usuario);
        membresia.setCompany(company);
        membresia.setFechaIngreso(LocalDate.now().minusMonths(2));
        membresia.setEstado(true);
        membresia.setCreatedAt(LocalDateTime.now());
        membresia.setUpdatedAt(LocalDateTime.now());
        g(membresias.save(membresia));
        return usuario;
    }

    private void asignar(Usuario usuario, Role rol, Company company) {
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        asignacion.setCompany(company);
        g(asignaciones.save(asignacion));
    }

    private Role rol(String nombre, Company company) {
        Role rol = new Role();
        rol.setName(nombre);
        rol.setScope(RoleScope.STAFF);
        rol.setPurpose(RolePurpose.CUSTOM);
        rol.setCompany(company);
        return g(roles.save(rol));
    }

    private Apoderado apoderado(Usuario usuario, Company company) {
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(company);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(usuario.getDni());
        apoderado.setGenero(Genero.FEMENINO);
        return g(apoderados.save(apoderado));
    }

    private Mascota mascota(String nombre, Apoderado dueno) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(dueno);
        mascota.setUuid(UUID.randomUUID().toString());
        return g(mascotas.save(mascota));
    }

    private Empleado empleado(Usuario usuario, Company company) {
        Empleado empleado = new Empleado();
        empleado.setUser(usuario);
        empleado.setCompany(company);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(usuario.getDni());
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        return g(empleados.save(empleado));
    }

    private ServiciosVeterinarios servicio(Company company, String marca) {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta " + marca);
        servicio.setDescripcion("Consulta veterinaria " + marca);
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        return g(servicios.save(servicio));
    }

    private Cita cita(Mascota mascota, Empleado empleado, ServiciosVeterinarios servicio, EstadoCita estado, int dias) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(dias);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(new BigDecimal("100.00"));
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return g(citas.save(cita));
    }

    private Consulta consulta(HistoriaClinica historia, Cita cita, Empleado veterinario, String marca) {
        Consulta consulta = new Consulta();
        consulta.setEstado(EstadoConsulta.ABIERTA);
        consulta.setHistoriaClinica(historia);
        consulta.setCita(cita);
        consulta.setVeterinario(veterinario);
        consulta.setFechaConsulta(LocalDateTime.now().minusDays(20));
        consulta.setMotivoConsulta("Motivo confidencial " + marca);
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        return g(consultas.save(consulta));
    }

    private void diagnostico(Consulta consulta, String marca) {
        Diagnostico diagnostico = new Diagnostico();
        diagnostico.setConsulta(consulta);
        diagnostico.setNombre("Diagnostico " + marca);
        diagnostico.setTipo(TipoDiagnostico.DEFINITIVO);
        diagnostico.setEstado(EstadoDiagnostico.ACTIVO);
        g(diagnosticos.save(diagnostico));
    }

    private void tratamiento(Consulta consulta, String marca) {
        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setConsulta(consulta);
        tratamiento.setNombre("Tratamiento " + marca);
        tratamiento.setEstado(EstadoTratamiento.ACTIVO);
        g(tratamientos.save(tratamiento));
    }

    private void prescripcion(Consulta consulta, String marca) {
        Prescripcion prescripcion = new Prescripcion();
        prescripcion.setConsulta(consulta);
        prescripcion.setMedicamento("Medicamento " + marca);
        prescripcion.setDosis("1 tableta");
        prescripcion.setFrecuencia("Cada 8 horas");
        prescripcion.setFechaInicio(LocalDate.now().minusDays(20));
        g(prescripciones.save(prescripcion));
    }

    private Producto producto(Company company, String marca) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setCompany(company);
        categoria.setNombre("Categoria " + marca);
        categoria.setActivo(true);
        categoria = g(categorias.save(categoria));
        MarcaProducto marcaProducto = new MarcaProducto();
        marcaProducto.setCompany(company);
        marcaProducto.setNombre("Marca " + marca);
        marcaProducto.setActivo(true);
        g(marcas.save(marcaProducto));
        UnidadMedida unidad = new UnidadMedida();
        unidad.setCompany(company);
        unidad.setNombre("Unidad " + marca);
        unidad.setActivo(true);
        g(unidades.save(unidad));

        Producto producto = new Producto();
        producto.setCompany(company);
        producto.setNombre("Producto " + marca);
        producto.setCategoria(categoria);
        producto.setPrecio(new BigDecimal("25.00"));
        producto.setStock(10);
        producto.setControlStock(TipoControlStock.LOTES);
        producto.setStockMinimo(2);
        producto.setSku("SKU-" + marca + "-1");
        producto.setRequiereReceta(false);
        producto.setAplicacionEspecie(TipoAplicacionProducto.USO_GENERAL);
        producto.setEspecies(Set.of(EspecieMascota.PERRO));
        producto.setActivo(true);
        producto = g(productos.save(producto));

        Lote lote = new Lote();
        lote.setCompany(company);
        lote.setProducto(producto);
        lote.setNumeroLote("LOTE-" + marca);
        lote.setFechaVencimiento(LocalDate.now().plusMonths(8));
        lote.setCantidad(10);
        lote.setCantidadInicial(10);
        lote.setActivo(true);
        g(lotes.save(lote));
        return producto;
    }
}
