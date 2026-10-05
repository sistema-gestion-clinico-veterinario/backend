package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyMembershipServiceImplTest {

    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock UsuarioMembresiaRepository usuarioMembresiaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock CompanyRepository companyRepository;

    @InjectMocks CompanyMembershipServiceImpl service;

    @Test
    void cuentaSinNingunaRelacionNoSeConsideraDesactivada() {
        assertThat(service.hasOnlyInactiveMemberships(1)).isFalse();
    }

    @Test
    void cuentaConRelacionesTodasInactivasSeConsideraDesactivada() {
        when(empleadoRepository.existsByUserId(1)).thenReturn(true);
        when(empleadoRepository.existsByUserIdAndEstadoTrue(1)).thenReturn(false);
        when(apoderadoRepository.existsByUserIdAndEstadoTrue(1)).thenReturn(false);
        when(usuarioMembresiaRepository.existsByUsuarioIdAndEstadoTrue(1)).thenReturn(false);

        assertThat(service.hasOnlyInactiveMemberships(1)).isTrue();
    }

    @Test
    void cuentaConAlMenosUnaRelacionActivaNoSeConsideraDesactivada() {
        when(empleadoRepository.existsByUserId(1)).thenReturn(true);
        when(empleadoRepository.existsByUserIdAndEstadoTrue(1)).thenReturn(false);
        when(apoderadoRepository.existsByUserIdAndEstadoTrue(1)).thenReturn(true);

        assertThat(service.hasOnlyInactiveMemberships(1)).isFalse();
    }

    @Test
    void idNuloNoSeConsideraDesactivado() {
        assertThat(service.hasOnlyInactiveMemberships(null)).isFalse();
    }

    private Usuario usuario(int id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    private Apoderado relacionActiva(Usuario usuario, int companyId) {
        Company company = new Company();
        company.setId(companyId);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(company);
        apoderado.setEstado(true);
        return apoderado;
    }

    @Test
    void elCorreoSeConsideraOcupadoSiOtroUsuarioLoTieneEnUnaClinicaDeLaPersona() {
        Usuario persona = usuario(1);
        Usuario colega = usuario(2);
        when(apoderadoRepository.findAllActiveByUserId(1)).thenReturn(List.of(relacionActiva(persona, 3), relacionActiva(persona, 4)));
        when(usuarioRepository.findAllByEmailIgnoreCase("nuevo@example.com")).thenReturn(List.of(colega));
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(2, 3)).thenReturn(false);
        when(apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(2, 3)).thenReturn(false);
        when(usuarioMembresiaRepository.existsByUsuarioIdAndCompanyIdAndEstadoTrue(2, 3)).thenReturn(false);
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(2, 4)).thenReturn(true);

        assertThat(service.isEmailTakenInUserCompanies(persona, "nuevo@example.com")).isTrue();
    }

    @Test
    void elCorreoNoSeConsideraOcupadoSiSoloLoUsaAlguienDeOtraClinicaOLaMismaPersona() {
        Usuario persona = usuario(1);
        Usuario deOtraClinica = usuario(2);
        when(apoderadoRepository.findAllActiveByUserId(1)).thenReturn(List.of(relacionActiva(persona, 3)));
        when(usuarioRepository.findAllByEmailIgnoreCase("nuevo@example.com")).thenReturn(List.of(persona, deOtraClinica));
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(2, 3)).thenReturn(false);
        when(apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(2, 3)).thenReturn(false);
        when(usuarioMembresiaRepository.existsByUsuarioIdAndCompanyIdAndEstadoTrue(2, 3)).thenReturn(false);

        assertThat(service.isEmailTakenInUserCompanies(persona, "nuevo@example.com")).isFalse();
    }

    @Test
    void unaCuentaSinClinicasNoSeCompararContraNadie() {
        Usuario adminDePlataforma = usuario(1);
        when(usuarioRepository.findAllByUsernameIgnoreCase("nuevo")).thenReturn(List.of(usuario(2)));

        assertThat(service.isUsernameTakenInUserCompanies(adminDePlataforma, "nuevo")).isFalse();
    }

    @Test
    void unaRelacionDeEmpleadoSuspendidaEnLaEmpresaSeReconoce() {
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoFalseAndTipoInactividad(
                1, 7, veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION)).thenReturn(true);

        assertThat(service.isSuspendedIn(1, 7)).isTrue();
    }

    @Test
    void unaRelacionDeClienteSuspendidaEnLaEmpresaSeReconoce() {
        when(apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoFalseAndTipoInactividad(
                1, 7, veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION)).thenReturn(true);

        assertThat(service.isSuspendedIn(1, 7)).isTrue();
    }

    @Test
    void unaRelacionDadaDeBajaSeReconoceComoBajaYNoComoSuspension() {
        when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoFalseAndTipoInactividad(
                1, 7, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA)).thenReturn(true);

        assertThat(service.isDeactivatedIn(1, 7)).isTrue();
        assertThat(service.isSuspendedIn(1, 7)).isFalse();
        assertThat(service.isDeactivatedIn(null, 7)).isFalse();
    }

    @Test
    void sinSuspensionEnEsaEmpresaNoSeConsideraSuspendida() {
        assertThat(service.isSuspendedIn(1, 7)).isFalse();
    }

    @Test
    void sinIdentificadoresNoSeConsideraSuspendida() {
        assertThat(service.isSuspendedIn(null, 7)).isFalse();
        assertThat(service.isSuspendedIn(1, null)).isFalse();
    }
}
