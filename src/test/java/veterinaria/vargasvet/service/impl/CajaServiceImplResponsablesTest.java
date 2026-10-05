package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.AperturaCajaRequest;
import veterinaria.vargasvet.dto.response.SesionCajaResponse;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.MovimientoCajaRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.SesionCajaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CajaServiceImplResponsablesTest {

    @Mock MovimientoCajaRepository movimientoRepo;
    @Mock CitaRepository citaRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock SesionCajaRepository sesionCajaRepository;
    @Mock AuditLogService auditLogService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock veterinaria.vargasvet.service.PuntoCobroService puntoCobroService;
    @Mock veterinaria.vargasvet.repository.CajaRepository cajaRepository;

    private CajaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CajaServiceImpl(movimientoRepo, citaRepository, purchaseRepository, sesionCajaRepository,
                auditLogService, usuarioRepository, puntoCobroService, cajaRepository);
        UsuarioPrincipal principal = new UsuarioPrincipal(10, "ana@example.test", "", List.of(), 7,
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario persona(int id, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        return usuario;
    }

    private SesionCaja sesion(Integer abreId, String abre, Integer cierraId, String cierra) {
        SesionCaja sesion = new SesionCaja();
        sesion.setId(1L);
        sesion.setCompanyId(7);
        sesion.setEstado(EstadoSesionCaja.CERRADA);
        sesion.setAbiertaPorUsuarioId(abreId);
        sesion.setAbiertaPor(abre);
        sesion.setCerradaPorUsuarioId(cierraId);
        sesion.setCerradaPor(cierra);
        return sesion;
    }

    private SesionCajaResponse unica(SesionCaja sesion) {
        when(sesionCajaRepository.findByCompanyIdOrderByAbiertaAtDesc(eq(7), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sesion)));
        return service.listarSesiones(7, 0, 10).getContent().get(0);
    }

    @Test
    void elHistorialMuestraElNombreDeQuienAbrioYCerroLaCaja() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(persona(10, "Ana", "Pérez")));
        when(usuarioRepository.findById(11)).thenReturn(Optional.of(persona(11, "Luis", "Gómez")));

        SesionCajaResponse respuesta = unica(sesion(10, "ana@example.test", 11, "luis@example.test"));

        assertThat(respuesta.getAbiertaPorNombre()).isEqualTo("Ana Pérez");
        assertThat(respuesta.getCerradaPorNombre()).isEqualTo("Luis Gómez");
        assertThat(respuesta.getAbiertaPor()).isEqualTo("ana@example.test");
    }

    @Test
    void lasCajasAnterioresSinPersonaGuardadaBuscanElNombrePorElCorreo() {
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test"))
                .thenReturn(List.of(persona(10, "Ana", "Pérez")));

        SesionCajaResponse respuesta = unica(sesion(null, "ana@example.test", null, null));

        assertThat(respuesta.getAbiertaPorNombre()).isEqualTo("Ana Pérez");
        assertThat(respuesta.getCerradaPorNombre()).isNull();
    }

    @Test
    void siNoSeEncuentraLaPersonaNoSeInventaNadaYQuedaElCorreo() {
        when(usuarioRepository.findAllByEmailIgnoreCase("fantasma@example.test")).thenReturn(List.of());

        SesionCajaResponse respuesta = unica(sesion(null, "fantasma@example.test", null, null));

        assertThat(respuesta.getAbiertaPorNombre()).isNull();
        assertThat(respuesta.getAbiertaPor()).isEqualTo("fantasma@example.test");
    }

    @Test
    void laRespuestaNuncaExponeElIdentificadorInternoDeLaPersona() {
        assertThat(SesionCajaResponse.class.getDeclaredFields())
                .extracting(campo -> campo.getName())
                .noneMatch(nombre -> nombre.toLowerCase().contains("usuarioid"));
    }

    @Test
    void laMismaPersonaEnVariasFilasSeConsultaUnaSolaVez() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(persona(10, "Ana", "Pérez")));
        SesionCaja una = sesion(10, "ana@example.test", 10, "ana@example.test");
        SesionCaja otra = sesion(10, "ana@example.test", 10, "ana@example.test");
        when(sesionCajaRepository.findByCompanyIdOrderByAbiertaAtDesc(eq(7), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(una, otra)));

        service.listarSesiones(7, 0, 10);

        verify(usuarioRepository, times(1)).findById(10);
    }

    @Test
    void alAbrirLaCajaSeGuardaLaPersonaYSuNombreSeMuestraEnLaRespuesta() {
        veterinaria.vargasvet.domain.entity.Caja caja = new veterinaria.vargasvet.domain.entity.Caja();
        caja.setId(1L);
        caja.setNombre("Caja principal");
        when(puntoCobroService.resolverParaOperar(7)).thenReturn(caja);
        when(sesionCajaRepository.findFirstByCajaIdAndEstado(1L, EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());
        when(sesionCajaRepository.findFirstByAbiertaPorUsuarioIdAndEstado(10, EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(sesionCajaRepository.saveAndFlush(any(SesionCaja.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(persona(10, "Ana", "Pérez")));
        AperturaCajaRequest pedido = new AperturaCajaRequest();
        pedido.setCompanyId(7);
        pedido.setMontoApertura(new BigDecimal("100.00"));

        SesionCajaResponse respuesta = service.abrirCaja(pedido);

        ArgumentCaptor<SesionCaja> guardada = ArgumentCaptor.forClass(SesionCaja.class);
        verify(sesionCajaRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getAbiertaPorUsuarioId()).isEqualTo(10);
        assertThat(guardada.getValue().getCajaId()).isEqualTo(1L);
        assertThat(respuesta.getCajaNombre()).isEqualTo("Caja principal");
        assertThat(guardada.getValue().getAbiertaPor()).isEqualTo("ana@example.test");
        assertThat(respuesta.getAbiertaPorNombre()).isEqualTo("Ana Pérez");
    }

    private void abrirCajaDeMostrador() {
        veterinaria.vargasvet.domain.entity.Caja caja = new veterinaria.vargasvet.domain.entity.Caja();
        caja.setId(1L);
        caja.setNombre("Mostrador 1");
        when(puntoCobroService.resolverParaOperar(7)).thenReturn(caja);
        when(sesionCajaRepository.findFirstByCajaIdAndEstado(1L, EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());
        when(sesionCajaRepository.findFirstByAbiertaPorUsuarioIdAndEstado(10, EstadoSesionCaja.ABIERTA)).thenReturn(Optional.empty());
    }

    private AperturaCajaRequest apertura() {
        AperturaCajaRequest pedido = new AperturaCajaRequest();
        pedido.setCompanyId(7);
        pedido.setMontoApertura(new BigDecimal("100.00"));
        return pedido;
    }

    @Test
    void siDosAperturasLlegarAlaVezLaSegundaRecibeUnMensajeClaroDeLaCajaOcupada() {
        abrirCajaDeMostrador();
        when(sesionCajaRepository.saveAndFlush(any(SesionCaja.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("x",
                        new RuntimeException("duplicate key value violates unique constraint \"uq_sesion_caja_abierta_por_caja\"")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.abrirCaja(apertura()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La caja de «Mostrador 1» ya se encuentra abierta");
    }

    @Test
    void siLaMismaPersonaAbreDosCajasALaVezElMensajeEsClaro() {
        abrirCajaDeMostrador();
        when(sesionCajaRepository.saveAndFlush(any(SesionCaja.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("x",
                        new RuntimeException("duplicate key value violates unique constraint \"uq_sesion_caja_abierta_por_persona\"")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.abrirCaja(apertura()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya tienes una caja abierta. Ciérrala antes de abrir otra.");
    }

    @Test
    void elHistorialDeMovimientosMuestraElPuntoDeCobroYElNombreDeQuienLoRegistro() {
        veterinaria.vargasvet.domain.entity.MovimientoCaja movimiento = new veterinaria.vargasvet.domain.entity.MovimientoCaja();
        movimiento.setId(5L);
        movimiento.setSesionCajaId(3L);
        movimiento.setCompanyId(7);
        movimiento.setRegistradoPor("ana@example.test");
        SesionCaja sesion = new SesionCaja();
        sesion.setId(3L);
        sesion.setCajaId(1L);
        veterinaria.vargasvet.domain.entity.Caja caja = new veterinaria.vargasvet.domain.entity.Caja();
        caja.setId(1L);
        caja.setNombre("Mostrador 2");
        Usuario deOtraSede = persona(99, "Ana", "Ajena");
        deOtraSede.setCompany(otraEmpresa(8));
        Usuario deEstaSede = persona(10, "Ana", "Pérez");
        deEstaSede.setCompany(otraEmpresa(7));
        when(movimientoRepo.findByCompanyIdOrderByFechaDesc(eq(7), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimiento, movimiento)));
        when(sesionCajaRepository.findById(3L)).thenReturn(Optional.of(sesion));
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findAllByEmailIgnoreCase("ana@example.test")).thenReturn(List.of(deOtraSede, deEstaSede));

        var respuesta = service.listar(7, null, null, 0, 10).getContent();

        assertThat(respuesta).hasSize(2);
        assertThat(respuesta.get(0).getPuntoCobro()).isEqualTo("Mostrador 2");
        assertThat(respuesta.get(0).getRegistradoPorNombre()).isEqualTo("Ana Pérez");
        verify(sesionCajaRepository, times(1)).findById(3L);
        verify(usuarioRepository, times(1)).findAllByEmailIgnoreCase("ana@example.test");
    }

    private veterinaria.vargasvet.domain.entity.Company otraEmpresa(int id) {
        veterinaria.vargasvet.domain.entity.Company empresa = new veterinaria.vargasvet.domain.entity.Company();
        empresa.setId(id);
        return empresa;
    }
}
