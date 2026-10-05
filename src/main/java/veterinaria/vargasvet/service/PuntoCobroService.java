package veterinaria.vargasvet.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Caja;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;
import veterinaria.vargasvet.dto.response.EstadoEquipoResponse;
import veterinaria.vargasvet.dto.response.PuntoCobroResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CajaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.SesionCajaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.CajaDispositivoCookie;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.DispositivoInfo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Puntos de cobro de una sede (una sede es una empresa). Cada punto tiene su propia caja y puede vincularse a un
 * equipo; así dos equipos de un mostrador trabajan cada uno con su caja y un mismo equipo no abre dos a la vez.
 */
@Service
@RequiredArgsConstructor
public class PuntoCobroService {

    public static final String MODO_DISPOSITIVO = "DISPOSITIVO";
    public static final String MODO_SENCILLO = "SENCILLO";
    public static final String MODO_NO_REGISTRADO = "NO_REGISTRADO";

    private static final Duration USO_MINIMO_ENTRE_REGISTROS = Duration.ofMinutes(5);
    private static final String PRINCIPAL = "Caja principal";

    private final CajaRepository cajaRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaDispositivoCookie dispositivoCookie;
    private final AuditLogService auditLogService;
    private final CompanyRepository companyRepository;

    public record Resolucion(String modo, Caja caja) {}

    /** Qué caja le corresponde a quien está operando ahora desde este equipo. */
    @Transactional
    public Resolucion resolver(Integer companyId) {
        validarEmpresa(companyId);
        asegurarPrincipal(companyId);
        Optional<Caja> delEquipo = dispositivoCookie.leer(slugDe(companyId))
                .flatMap(token -> cajaRepository.findByDispositivoTokenHashAndCompanyId(SecurityTokenUtils.hash(token), companyId))
                .filter(Caja::isActiva);
        if (delEquipo.isPresent()) {
            registrarUso(delEquipo.get());
            return new Resolucion(MODO_DISPOSITIVO, delEquipo.get());
        }
        List<Caja> activas = cajaRepository.findByCompanyIdAndActivaTrueOrderByNombreAsc(companyId);
        if (activas.size() == 1 && activas.get(0).getDispositivoTokenHash() == null) {
            return new Resolucion(MODO_SENCILLO, activas.get(0));
        }
        return new Resolucion(MODO_NO_REGISTRADO, null);
    }

    @Transactional
    public Caja resolverParaOperar(Integer companyId) {
        Resolucion resolucion = resolver(companyId);
        if (resolucion.caja() == null) {
            throw new IllegalArgumentException("Este equipo no está registrado como punto de cobro. "
                    + "Pide a un administrador que lo registre en Caja, en «Puntos de cobro».");
        }
        return resolucion.caja();
    }

    @Transactional
    public EstadoEquipoResponse estadoDeEsteEquipo(Integer companyId) {
        Resolucion resolucion = resolver(companyId);
        return switch (resolucion.modo()) {
            case MODO_DISPOSITIVO -> new EstadoEquipoResponse(MODO_DISPOSITIVO, resolucion.caja().getNombre(),
                    "Este equipo es el punto de cobro «" + resolucion.caja().getNombre() + "».");
            case MODO_SENCILLO -> new EstadoEquipoResponse(MODO_SENCILLO, resolucion.caja().getNombre(),
                    "Esta sede tiene un solo punto de cobro y cualquier equipo puede usarlo.");
            default -> new EstadoEquipoResponse(MODO_NO_REGISTRADO, null,
                    "Este equipo no está registrado como punto de cobro. Pide a un administrador que lo registre.");
        };
    }

    @Transactional
    public List<PuntoCobroResponse> listar(Integer companyId) {
        validarEmpresa(companyId);
        asegurarPrincipal(companyId);
        Optional<String> hashDeEsteEquipo = dispositivoCookie.leer(slugDe(companyId)).map(SecurityTokenUtils::hash);
        Map<Long, SesionCaja> abiertas = sesionCajaRepository.findAllByCompanyIdAndEstado(companyId, EstadoSesionCaja.ABIERTA)
                .stream().filter(s -> s.getCajaId() != null)
                .collect(Collectors.toMap(SesionCaja::getCajaId, Function.identity(), (a, b) -> a));
        Map<Integer, String> nombres = new HashMap<>();
        return cajaRepository.findByCompanyIdOrderByNombreAsc(companyId).stream().map(caja -> {
            SesionCaja sesion = abiertas.get(caja.getId());
            String quien = sesion == null ? null : nombres.computeIfAbsent(
                    sesion.getAbiertaPorUsuarioId() == null ? -1 : sesion.getAbiertaPorUsuarioId(),
                    id -> nombreDe(sesion));
            return new PuntoCobroResponse(caja.getId(), caja.getNombre(), caja.isActiva(),
                    caja.getDispositivoTokenHash() != null, caja.getDispositivoInfo(),
                    caja.getDispositivoVinculadoAt(), caja.getDispositivoUltimoUsoAt(),
                    caja.getDispositivoTokenHash() != null && hashDeEsteEquipo.filter(caja.getDispositivoTokenHash()::equals).isPresent(),
                    sesion != null, quien == null || quien.isEmpty() ? (sesion == null ? null : sesion.getAbiertaPor()) : quien);
        }).toList();
    }

    @Transactional
    public PuntoCobroResponse crear(Integer companyId, String nombre) {
        validarEmpresa(companyId);
        asegurarAdministrador();
        String limpio = normalizarNombre(nombre);
        if (cajaRepository.existsByCompanyIdAndNombreIgnoreCase(companyId, limpio)) {
            throw new IllegalArgumentException("Ya existe un punto de cobro con ese nombre");
        }
        Caja caja = new Caja();
        caja.setCompanyId(companyId);
        caja.setNombre(limpio);
        caja.setCreadaAt(AppClock.now());
        Caja guardada = cajaRepository.save(caja);
        auditLogService.log(companyId, "CREAR_PUNTO_COBRO", "Caja", "Creó el punto de cobro «" + limpio + "»");
        return respuestaSimple(guardada);
    }

    @Transactional
    public PuntoCobroResponse actualizar(Integer companyId, Long id, String nombre, Boolean activa) {
        validarEmpresa(companyId);
        asegurarAdministrador();
        Caja caja = buscar(companyId, id);
        String limpio = normalizarNombre(nombre);
        if (!caja.getNombre().equalsIgnoreCase(limpio)
                && cajaRepository.existsByCompanyIdAndNombreIgnoreCase(companyId, limpio)) {
            throw new IllegalArgumentException("Ya existe un punto de cobro con ese nombre");
        }
        if (activa != null && caja.isActiva() && !activa) {
            if (tieneSesionAbierta(caja)) {
                throw new IllegalArgumentException("Cierra la caja de este punto de cobro antes de desactivarlo");
            }
            long otrasActivas = cajaRepository.findByCompanyIdAndActivaTrueOrderByNombreAsc(companyId).stream()
                    .filter(otra -> !otra.getId().equals(caja.getId())).count();
            if (otrasActivas == 0) {
                throw new IllegalArgumentException("La sede debe tener al menos un punto de cobro activo");
            }
            caja.setActiva(false);
            caja.setDispositivoTokenHash(null);
            caja.setDispositivoInfo(null);
            caja.setDispositivoVinculadoAt(null);
        } else if (activa != null && !caja.isActiva() && activa) {
            caja.setActiva(true);
        }
        String anterior = caja.getNombre();
        caja.setNombre(limpio);
        Caja guardada = cajaRepository.save(caja);
        auditLogService.log(companyId, "ACTUALIZAR_PUNTO_COBRO", "Caja",
                "Actualizó el punto de cobro «" + anterior + "»" + (anterior.equals(limpio) ? "" : " (ahora «" + limpio + "»)"));
        return respuestaSimple(guardada);
    }

    /** Registra este equipo como el punto de cobro indicado; la persona que lo pida debe ser administradora. */
    @Transactional
    public PuntoCobroResponse vincular(Integer companyId, Long id, HttpServletRequest request, HttpServletResponse response) {
        validarEmpresa(companyId);
        asegurarAdministrador();
        Caja caja = buscar(companyId, id);
        if (!caja.isActiva()) {
            throw new IllegalArgumentException("Activa el punto de cobro antes de registrar un equipo");
        }
        if (tieneSesionAbierta(caja)) {
            throw new IllegalArgumentException("Cierra la caja de este punto de cobro antes de cambiar su equipo");
        }
        String slug = slugDe(companyId);
        dispositivoCookie.leer(request, slug)
                .flatMap(token -> cajaRepository.findByDispositivoTokenHashAndCompanyId(SecurityTokenUtils.hash(token), companyId))
                .filter(otra -> !otra.getId().equals(caja.getId()))
                .ifPresent(otra -> {
                    if (tieneSesionAbierta(otra)) {
                        throw new IllegalArgumentException("Este equipo ya es el punto de cobro «" + otra.getNombre()
                                + "» y tiene la caja abierta. Ciérrala antes de cambiarlo.");
                    }
                    otra.setDispositivoTokenHash(null);
                    otra.setDispositivoInfo(null);
                    otra.setDispositivoVinculadoAt(null);
                    cajaRepository.save(otra);
                });
        String token = SecurityTokenUtils.generate();
        LocalDateTime ahora = AppClock.now();
        caja.setDispositivoTokenHash(SecurityTokenUtils.hash(token));
        caja.setDispositivoInfo(DispositivoInfo.describir(request.getHeader("User-Agent")));
        caja.setDispositivoVinculadoAt(ahora);
        caja.setDispositivoUltimoUsoAt(ahora);
        Caja guardada = cajaRepository.save(caja);
        dispositivoCookie.fijar(response, token, slug);
        auditLogService.log(companyId, "VINCULAR_PUNTO_COBRO", "Caja",
                "Registró un equipo (" + guardada.getDispositivoInfo() + ") como el punto de cobro «" + guardada.getNombre() + "»");
        return new PuntoCobroResponse(guardada.getId(), guardada.getNombre(), true, true, guardada.getDispositivoInfo(),
                ahora, ahora, true, false, null);
    }

    @Transactional
    public PuntoCobroResponse desvincular(Integer companyId, Long id, HttpServletRequest request, HttpServletResponse response) {
        validarEmpresa(companyId);
        asegurarAdministrador();
        Caja caja = buscar(companyId, id);
        if (tieneSesionAbierta(caja)) {
            throw new IllegalArgumentException("Cierra la caja de este punto de cobro antes de quitarle su equipo");
        }
        String slug = slugDe(companyId);
        boolean eraEsteEquipo = caja.getDispositivoTokenHash() != null && dispositivoCookie.leer(request, slug)
                .map(SecurityTokenUtils::hash).filter(caja.getDispositivoTokenHash()::equals).isPresent();
        caja.setDispositivoTokenHash(null);
        caja.setDispositivoInfo(null);
        caja.setDispositivoVinculadoAt(null);
        Caja guardada = cajaRepository.save(caja);
        if (eraEsteEquipo) {
            dispositivoCookie.borrar(response, slug);
        }
        auditLogService.log(companyId, "DESVINCULAR_PUNTO_COBRO", "Caja",
                "Quitó el equipo registrado del punto de cobro «" + guardada.getNombre() + "»");
        return respuestaSimple(guardada);
    }

    private String slugDe(Integer companyId) {
        return companyRepository.findById(companyId).map(veterinaria.vargasvet.domain.entity.Company::getSlug).orElse(null);
    }

    private void asegurarPrincipal(Integer companyId) {
        if (cajaRepository.findByCompanyIdOrderByNombreAsc(companyId).isEmpty()) {
            Caja caja = new Caja();
            caja.setCompanyId(companyId);
            caja.setNombre(PRINCIPAL);
            caja.setCreadaAt(AppClock.now());
            cajaRepository.save(caja);
        }
    }

    private void registrarUso(Caja caja) {
        LocalDateTime ahora = AppClock.now();
        if (caja.getDispositivoUltimoUsoAt() == null
                || Duration.between(caja.getDispositivoUltimoUsoAt(), ahora).compareTo(USO_MINIMO_ENTRE_REGISTROS) > 0) {
            caja.setDispositivoUltimoUsoAt(ahora);
            cajaRepository.save(caja);
        }
    }

    private boolean tieneSesionAbierta(Caja caja) {
        return sesionCajaRepository.findFirstByCajaIdAndEstado(caja.getId(), EstadoSesionCaja.ABIERTA).isPresent();
    }

    private Caja buscar(Integer companyId, Long id) {
        return cajaRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Punto de cobro no encontrado"));
    }

    private String normalizarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 2 || limpio.length() > 80) {
            throw new IllegalArgumentException("El nombre debe tener entre 2 y 80 caracteres");
        }
        return limpio;
    }

    private PuntoCobroResponse respuestaSimple(Caja caja) {
        return new PuntoCobroResponse(caja.getId(), caja.getNombre(), caja.isActiva(), caja.getDispositivoTokenHash() != null,
                caja.getDispositivoInfo(), caja.getDispositivoVinculadoAt(), caja.getDispositivoUltimoUsoAt(),
                false, tieneSesionAbierta(caja), null);
    }

    private String nombreDe(SesionCaja sesion) {
        if (sesion.getAbiertaPorUsuarioId() == null) {
            return "";
        }
        Usuario usuario = usuarioRepository.findById(sesion.getAbiertaPorUsuarioId()).orElse(null);
        if (usuario == null) {
            return "";
        }
        return ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }

    private void validarEmpresa(Integer companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Debe seleccionar una sede");
        }
        if (!SecurityUtils.isSuperAdmin()) {
            Integer actual = SecurityUtils.getCurrentCompanyId();
            if (actual == null || !actual.equals(companyId)) {
                throw new IllegalArgumentException("No puede operar la caja de otra sede");
            }
        }
    }

    private void asegurarAdministrador() {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Solo un administrador puede gestionar los puntos de cobro");
        }
    }
}
