package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MascotaBuscarPorClienteTest {

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;

    private Company clinica;
    private Company otraClinica;
    private Apoderado primerHomonimo;
    private Apoderado segundoHomonimo;
    private Apoderado clienteDeOtraClinica;

    @BeforeEach
    void datos() {
        clinica = clinica("Clinica Uno");
        otraClinica = clinica("Clinica Dos");
        primerHomonimo = apoderado(clinica, "Rafael", "Saavedra");
        segundoHomonimo = apoderado(clinica, "Rafael", "Saavedra");
        clienteDeOtraClinica = apoderado(otraClinica, "Rafael", "Saavedra");

        mascota(primerHomonimo, "Luna", true);
        mascota(primerHomonimo, "Rocky", false);
        mascota(segundoHomonimo, "Nala", true);
        mascota(clienteDeOtraClinica, "Ajena", true);
    }

    private Page<Mascota> buscar(Integer empresa, Long cliente, Boolean activo, String propietario, int tamano) {
        return mascotaRepository.buscar(empresa, null, null, propietario, activo, cliente, PageRequest.of(0, tamano));
    }

    @Test
    void sinFiltroDeClienteDevuelveLasMascotasDeTodosLosClientesDeLaClinica() {
        assertThat(buscar(clinica.getId(), null, null, null, 10).getContent())
                .extracting(Mascota::getNombreCompleto).containsExactlyInAnyOrder("Luna", "Rocky", "Nala");
    }

    @Test
    void conFiltroDeClienteDevuelveSoloLasMascotasDeEseCliente() {
        assertThat(buscar(clinica.getId(), primerHomonimo.getId(), null, null, 10).getContent())
                .extracting(Mascota::getNombreCompleto).containsExactlyInAnyOrder("Luna", "Rocky");
    }

    @Test
    void distingueAClientesConElMismoNombre() {
        assertThat(buscar(clinica.getId(), segundoHomonimo.getId(), null, "Rafael Saavedra", 10).getContent())
                .extracting(Mascota::getNombreCompleto).containsExactly("Nala");
    }

    @Test
    void elFiltroDeClienteSeCombinaConElDeEstado() {
        assertThat(buscar(clinica.getId(), primerHomonimo.getId(), true, null, 10).getContent())
                .extracting(Mascota::getNombreCompleto).containsExactly("Luna");
    }

    @Test
    void elTotalDeLaPaginaRespetaElFiltroDeCliente() {
        Page<Mascota> pagina = buscar(clinica.getId(), primerHomonimo.getId(), null, null, 1);

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    void unClienteDeOtraClinicaNoDevuelveNada() {
        assertThat(buscar(clinica.getId(), clienteDeOtraClinica.getId(), null, null, 10).getContent()).isEmpty();
    }

    private Company clinica(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("c-" + UUID.randomUUID());
        company.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Apoderado apoderado(Company empresa, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setEmail(nombre + "-" + UUID.randomUUID() + "@clinica.test");
        usuario.setUsername(nombre + "-" + UUID.randomUUID());
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(empresa);
        usuario = usuarioRepository.save(usuario);

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(empresa);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(usuario.getDni());
        apoderado.setGenero(Genero.FEMENINO);
        return apoderadoRepository.save(apoderado);
    }

    private void mascota(Apoderado dueno, String nombre, boolean activa) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(dueno);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota.setActivo(activa);
        mascotaRepository.save(mascota);
    }
}
