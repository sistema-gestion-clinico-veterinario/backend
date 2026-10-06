package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.AvisoPrivacidad;
import veterinaria.vargasvet.domain.entity.ConsentimientoDatos;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.domain.enums.EstadoConsentimiento;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;
import veterinaria.vargasvet.dto.response.ConsentimientoEstadoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.AvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.ConsentimientoDatosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.util.AppClock;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsentimientoDatosService {

    public static final String MENSAJE_SIN_AVISO = "La clínica aún no publicó su aviso de privacidad. "
            + "Publícalo en Configuración > Aviso de privacidad antes de registrar personas.";

    private static final List<CanalConsentimiento> CANALES_DE_LA_PERSONA =
            List.of(CanalConsentimiento.PORTAL, CanalConsentimiento.ACTIVACION);
    private static final int MAX_USER_AGENT = 255;
    private static final int MAX_IP = 64;

    private final ConsentimientoDatosRepository consentimientoRepository;
    private final AvisoPrivacidadRepository avisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AuditLogService auditLogService;
    private final CompanyMembershipService companyMembershipService;

    @Transactional(readOnly = true)
    public boolean hayAvisoPublicado(Integer companyId) {
        return companyId != null && avisoRepository.findByCompanyIdAndActivoTrue(companyId).isPresent();
    }

    @Transactional(readOnly = true)
    public void exigirAltaValida(Integer companyId, Boolean informada) {
        if (avisoRepository.findByCompanyIdAndActivoTrue(companyId).isEmpty()) {
            throw new IllegalStateException(MENSAJE_SIN_AVISO);
        }
        if (!Boolean.TRUE.equals(informada)) {
            throw new IllegalArgumentException(
                    "Confirma que la persona fue informada del aviso de privacidad antes de registrarla");
        }
    }

    @Transactional
    public void registrarAlta(Usuario usuario, Integer companyId, Boolean recordatorios, Integer registradoPorId) {
        AvisoPrivacidad aviso = avisoVigente(companyId);
        registrarEnterado(usuario, aviso, CanalConsentimiento.PRESENCIAL, registradoPorId, null, null);
        if (recordatorios != null) {
            decidir(usuario, aviso, FinalidadDatos.RECORDATORIOS_PREVENTIVOS,
                    recordatorios ? EstadoConsentimiento.OTORGADO : EstadoConsentimiento.RETIRADO,
                    CanalConsentimiento.PRESENCIAL, registradoPorId, null, null, null);
        }
    }

    @Transactional
    public boolean registrarEnterado(Integer usuarioId, Integer companyId, CanalConsentimiento canal,
                                     Integer registradoPorId, String ip, String userAgent) {
        Optional<AvisoPrivacidad> aviso = companyId == null ? Optional.empty()
                : avisoRepository.findByCompanyIdAndActivoTrue(companyId);
        if (aviso.isEmpty()) {
            return false;
        }
        registrarEnterado(usuarioDeLaEmpresa(usuarioId, companyId), aviso.get(), canal, registradoPorId, ip, userAgent);
        return true;
    }

    @Transactional
    public void cambiarFinalidad(Integer usuarioId, Integer companyId, FinalidadDatos finalidad, boolean otorgar,
                                 CanalConsentimiento canal, Integer registradoPorId, String motivo, String ip,
                                 String userAgent) {
        if (!finalidad.isOpcional()) {
            throw new IllegalArgumentException("Esa finalidad no admite consentimiento");
        }
        AvisoPrivacidad aviso = avisoVigente(companyId);
        Usuario usuario = usuarioDeLaEmpresa(usuarioId, companyId);
        EstadoConsentimiento estado = otorgar ? EstadoConsentimiento.OTORGADO : EstadoConsentimiento.RETIRADO;
        boolean cambio = decidir(usuario, aviso, finalidad, estado, canal, registradoPorId, motivo, ip, userAgent);
        if (cambio && canal == CanalConsentimiento.PRESENCIAL) {
            auditLogService.log(otorgar ? "OTORGAR_CONSENTIMIENTO_DATOS" : "RETIRAR_CONSENTIMIENTO_DATOS", "Seguridad",
                    "Se registró en la clínica que " + usuario.getNombre() + " " + usuario.getApellido()
                            + (otorgar ? " pidió recibir" : " pidió no recibir") + ": " + finalidad.getDescripcion());
        }
    }

    @Transactional(readOnly = true)
    public ConsentimientoEstadoResponse estado(Integer usuarioId, Integer companyId) {
        usuarioDeLaEmpresa(usuarioId, companyId);
        Optional<AvisoPrivacidad> aviso = avisoRepository.findByCompanyIdAndActivoTrue(companyId);
        Map<FinalidadDatos, ConsentimientoDatos> ultimas = consentimientoRepository
                .ultimasPorFinalidad(usuarioId, companyId).stream()
                .collect(Collectors.toMap(ConsentimientoDatos::getFinalidad, Function.identity()));
        ConsentimientoDatos enterado = ultimas.get(FinalidadDatos.ENTERADO);
        List<ConsentimientoEstadoResponse.Finalidad> finalidades = Arrays.stream(FinalidadDatos.values())
                .filter(FinalidadDatos::isOpcional)
                .map(finalidad -> {
                    ConsentimientoDatos ultima = ultimas.get(finalidad);
                    return new ConsentimientoEstadoResponse.Finalidad(finalidad.name(), finalidad.getDescripcion(),
                            ultima == null ? "SIN_REGISTRO" : ultima.getEstado().name(),
                            ultima == null ? null : ultima.getFecha(), ultima == null ? null : ultima.getCanal());
                })
                .toList();
        boolean vistaPorLaPersona = aviso.isPresent() && consentimientoRepository
                .existsByUsuarioIdAndCompanyIdAndFinalidadAndAvisoIdAndCanalIn(usuarioId, companyId,
                        FinalidadDatos.ENTERADO, aviso.get().getId(), CANALES_DE_LA_PERSONA);
        return new ConsentimientoEstadoResponse(aviso.isPresent(), aviso.map(AvisoPrivacidad::getVersion).orElse(null),
                enterado != null, enterado == null ? null : enterado.getAvisoVersion(),
                enterado == null ? null : enterado.getFecha(), enterado == null ? null : enterado.getCanal(),
                vistaPorLaPersona, finalidades);
    }

    @Transactional(readOnly = true)
    public Integer usuarioIdDelCliente(Long apoderadoId, Integer companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Esta acción corresponde a una clínica; ingresa con la cuenta de la clínica");
        }
        return apoderadoRepository.findByIdAndCompanyId(apoderadoId, companyId)
                .map(apoderado -> apoderado.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    @Transactional(readOnly = true)
    public Set<Integer> usuariosQueRetiraron(Collection<Integer> usuarioIds, Integer companyId,
                                             FinalidadDatos finalidad) {
        if (usuarioIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(consentimientoRepository.usuariosConUltimoEstado(
                usuarioIds, companyId, finalidad, EstadoConsentimiento.RETIRADO));
    }

    @Transactional(readOnly = true)
    public Set<Integer> usuariosInformados(Collection<Integer> usuarioIds, Integer companyId) {
        if (usuarioIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(consentimientoRepository.usuariosConRegistro(
                usuarioIds, companyId, FinalidadDatos.ENTERADO));
    }

    private void registrarEnterado(Usuario usuario, AvisoPrivacidad aviso, CanalConsentimiento canal,
                                   Integer registradoPorId, String ip, String userAgent) {
        if (consentimientoRepository.existsByUsuarioIdAndCompanyIdAndFinalidadAndAvisoIdAndCanalIn(
                usuario.getId(), aviso.getCompany().getId(), FinalidadDatos.ENTERADO, aviso.getId(), List.of(canal))) {
            return;
        }
        guardar(usuario, aviso, FinalidadDatos.ENTERADO, EstadoConsentimiento.OTORGADO, canal, registradoPorId, null, ip, userAgent);
    }

    private boolean decidir(Usuario usuario, AvisoPrivacidad aviso, FinalidadDatos finalidad, EstadoConsentimiento estado,
                            CanalConsentimiento canal, Integer registradoPorId, String motivo, String ip, String userAgent) {
        Optional<ConsentimientoDatos> ultimo = consentimientoRepository
                .findFirstByUsuarioIdAndCompanyIdAndFinalidadOrderByIdDesc(
                        usuario.getId(), aviso.getCompany().getId(), finalidad);
        if (ultimo.isPresent() && ultimo.get().getEstado() == estado) {
            return false;
        }
        guardar(usuario, aviso, finalidad, estado, canal, registradoPorId, motivo, ip, userAgent);
        return true;
    }

    private void guardar(Usuario usuario, AvisoPrivacidad aviso, FinalidadDatos finalidad, EstadoConsentimiento estado,
                         CanalConsentimiento canal, Integer registradoPorId, String motivo, String ip, String userAgent) {
        ConsentimientoDatos constancia = new ConsentimientoDatos();
        constancia.setCompany(aviso.getCompany());
        constancia.setUsuario(usuario);
        constancia.setAviso(aviso);
        constancia.setAvisoVersion(aviso.getVersion());
        constancia.setContenidoHash(aviso.getContenidoHash());
        constancia.setFinalidad(finalidad);
        constancia.setEstado(estado);
        constancia.setCanal(canal);
        if (registradoPorId != null && !registradoPorId.equals(usuario.getId())) {
            constancia.setRegistradoPor(usuarioRepository.getReferenceById(registradoPorId));
        }
        constancia.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.strip());
        constancia.setIpAddress(truncar(ip, MAX_IP));
        constancia.setUserAgent(truncar(userAgent, MAX_USER_AGENT));
        constancia.setFecha(AppClock.now());
        consentimientoRepository.saveAndFlush(constancia);
    }

    private AvisoPrivacidad avisoVigente(Integer companyId) {
        return avisoRepository.findByCompanyIdAndActivoTrue(companyId)
                .orElseThrow(() -> new IllegalStateException(MENSAJE_SIN_AVISO));
    }

    private Usuario usuarioDeLaEmpresa(Integer usuarioId, Integer companyId) {
        if (usuarioId == null || companyId == null
                || !companyMembershipService.hasAnyMembership(usuarioId, companyId)) {
            throw new ResourceNotFoundException("Persona no encontrada");
        }
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada"));
    }

    private String truncar(String valor, int max) {
        return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
    }
}
